package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.util.*;

public class CreateBicycleHighwayNetwork implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(CreateBicycleHighwayNetwork.class);

    @CommandLine.Option(names = "--network", description = "Path to network file", required = true)
    private String networkFile;

    @CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
    private String outputPath;

    @CommandLine.Option(names = "--policy-case", description = "Defines for which policy case the cycle highways are created.", required = true)
    private PolicyCase policyCase;

    @CommandLine.Option(names = "--max-link-length", description = "Max. allowed cycle highway link length", defaultValue = "800.0")
    private Double maxLinkLength;

    @CommandLine.Option(names = "--search-radius", description = "Search radius for nearest node search", defaultValue = "100.0")
    private Double radius;

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

    private static final String NODE_PREFIX = "cycle-highway-node-";
    private static final String LINK_PREFIX = "cycle-highway-";

    private final Random rnd = new Random();

    public static void main(String[] args) {
        new CreateBicycleHighwayNetwork().execute(args);
    }

    @Override
    public Integer call() throws Exception {

        Network network = NetworkUtils.readNetwork(networkFile);

        Network highwayNetwork = NetworkUtils.createNetwork();

        createBicycleHighways(highwayNetwork, shp);
        breakLinksIntoSmallerPieces(highwayNetwork);

//      add cycle highways to original network
        highwayNetwork.getNodes().values().forEach(network::addNode);
        highwayNetwork.getLinks().values().forEach(network::addLink);

        //        connect cycle highways to original network
        highwayNetwork.getNodes().values().forEach(n -> {
//          search for possible connections
            Collection<Node> nodes = NetworkUtils.getNearestNodes(network, n.getCoord(), radius).stream()
                    .filter(no -> !no.getId().toString().startsWith("pt_")).toList();

            nodes.stream()
                    .filter(nearNode -> !highwayNetwork.getNodes().containsKey(nearNode.getId()))
                    .sorted((node1, node2) -> {
                        Double dist1 = NetworkUtils.getEuclideanDistance(node1.getCoord(), n.getCoord());
                        Double dist2 = NetworkUtils.getEuclideanDistance(node2.getCoord(), n.getCoord());
                        return dist1.compareTo(dist2);
                    })
                    .limit(1)
                    .forEach(nearNode -> {
                        Link connection1 = network.getFactory()
                                .createLink(Id.createLinkId(LINK_PREFIX + "connect-" + n.getId() + "-" + nearNode.getId()), n, nearNode);
                        setLinkAttributes(connection1, LINK_PREFIX + "feeder");
                        NetworkUtils.setType(connection1, LINK_PREFIX);
                        Link connection2 = network.getFactory()
                                .createLink(Id.createLinkId(LINK_PREFIX + "connect-" + nearNode.getId() + "-" + n.getId()), nearNode, n);
                        setLinkAttributes(connection2, LINK_PREFIX + "feeder");

                        network.addLink(connection1);
                        network.addLink(connection2);
                    });
        });

//        add infrastructure speed factor according to policy case for each cycle highway link
        addInfrastructureFactor(network);

//      clean new bike network
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.bike));
        NetworkUtils.writeNetwork(network, outputPath);

        return 0;
    }

    private void breakLinksIntoSmallerPieces(Network network) {

        Set<Link> brokenUpLinksToAdd = new HashSet<>();
        Set<Link> longLinksToRemove = new HashSet<>();
        Set<Node> newNodesToAdd = new HashSet<>();

        NetworkFactory fac = network.getFactory();

        for (Link l : network.getLinks().values()) {

            double length = l.getLength();

            if (length > maxLinkLength) {

                longLinksToRemove.add(l);
                Node fromNode = l.getFromNode();
                Node toNode = l.getToNode();
                double numberOfParts = Math.ceil(length / maxLinkLength);
                double partLength = length / numberOfParts;
                double lengthFraction = partLength / length;
                double deltaX = toNode.getCoord().getX() - fromNode.getCoord().getX();
                double deltaY = toNode.getCoord().getY() - fromNode.getCoord().getY();
                Node currentNode = fromNode;

                log.info("original length of link {}: {}", l.getId(), length);
                log.info("splitting link {} into {} parts.", l.getId(), numberOfParts);

                while (numberOfParts > 1) {
                    // calculate new coordinate and add a node to the network
                    Coord newCoord = new Coord(
                            currentNode.getCoord().getX() + deltaX * lengthFraction,
                            currentNode.getCoord().getY() + deltaY * lengthFraction
                    );
                    Node newNode = fac.createNode(Id.createNodeId(NODE_PREFIX + rnd.nextInt(99999)), newCoord
                    );
                    newNodesToAdd.add(newNode);

                    // connect current and new node with a link and add it to the network
                    Link newLink = fac.createLink(Id.createLinkId(LINK_PREFIX + rnd.nextInt(99999)), currentNode, newNode);
                    setLinkAttributes(newLink, LINK_PREFIX);
                    brokenUpLinksToAdd.add(newLink);

                    // wrap up for next iteration
                    currentNode = newNode;
                    numberOfParts--;
                }

                // last link to be inserted must be connected to currentNode and toNode
                Link lastLink = fac.createLink(Id.createLinkId(LINK_PREFIX + rnd.nextInt(99999)), currentNode, toNode);
                setLinkAttributes(lastLink, LINK_PREFIX);
                brokenUpLinksToAdd.add(lastLink);
            }
        }

//      remove long links and add new shorter links + new nodes
        longLinksToRemove.forEach(l -> network.removeLink(l.getId()));
        newNodesToAdd.forEach(network::addNode);
        brokenUpLinksToAdd.forEach(network::addLink);
    }

    private void createBicycleHighways(Network network, ShpOptions shp) {

        NetworkFactory fac = network.getFactory();

        List<SimpleFeature> features = shp.readFeatures();

        int id = 0;

        for (SimpleFeature feature : features) {

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coord firstCoord = new Coord(geometry.getCoordinates()[0].x, geometry.getCoordinates()[0].y);

            Node prevNode = fac.createNode(Id.createNodeId(NODE_PREFIX + id), firstCoord);
            network.addNode(prevNode);
            id++;

            for (int i = 1; i < geometry.getCoordinates().length; i++) {

                Coord coord = new Coord(geometry.getCoordinates()[i].x, geometry.getCoordinates()[i].y);
                Node node = fac.createNode(Id.createNodeId(NODE_PREFIX + id), coord);
                network.addNode(node);

//                create links for both directions
                Link link = fac.createLink(Id.createLinkId(LINK_PREFIX + id + "_0"), prevNode, node);
                setLinkAttributes(link, LINK_PREFIX);

                Link link2 = fac.createLink(Id.createLinkId(LINK_PREFIX + id + "_1"), node, prevNode);
                setLinkAttributes(link2, LINK_PREFIX);

                network.addLink(link);
                network.addLink(link2);

                prevNode = node;
                id++;
            }
        }
    }

    private void addInfrastructureFactor(Network network) {
        for (Link l : network.getLinks().values()) {
            if (!l.getId().toString().contains(LINK_PREFIX)) continue;

            switch (policyCase) {
                case SPEED_15:
                    l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1);
                    break;
                case SPEED_25:
                    l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.67);
                    break;
                case SPEED_1500:
                    l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 100);
                    break;
                case SPEED_50:
                    l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 3.33);
                    break;
            }
        }
    }

    private static void setLinkAttributes(Link link, String type) {
//        freespeed does not matter because bikes can only drive 15kmh anyways
        link.setFreespeed(1000.);
        link.setCapacity(10000.);
        link.setAllowedModes(Set.of(TransportMode.bike));
        link.setNumberOfLanes(1);
        link.setLength(Math.round(NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()) * 100.) / 100.);
        NetworkUtils.setType(link, type);
    }

    private enum PolicyCase {
        SPEED_25,
        SPEED_15,
        SPEED_1500,
        SPEED_50
    }
}
