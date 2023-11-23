package org.matsim.run.prepare;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.LeipzigUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "network",
		description = "Prepare network with various policy options."
)
public class PrepareNetwork implements MATSimAppCommand {
	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
	private String outputPath;

	@CommandLine.Mixin
	private NetworkOptions options;

	private static final Logger log = LogManager.getLogger(PrepareNetwork.class);

	public static void main(String[] args) {
		new PrepareNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkFile);
		options.prepare(network);
		NetworkUtils.writeNetwork(network, outputPath);

		return 0;
	}

	/**
	 * Adapt network to one or more drt service areas. Therefore, a shape file of the wished service area + a list
	 * of drt modes are needed.
	 */
	static void prepareDRT(Network network, ShpOptions shp, String modes) {

		Set<String> modesToAdd = new HashSet<>(Arrays.asList(modes.split(",")));
		Geometry drtOperationArea = null;
		Geometry avOperationArea = null;

		List<SimpleFeature> features = shp.readFeatures();
		for (SimpleFeature feature : features) {
			if (feature.getAttribute("mode").equals("drt")) {
				if (drtOperationArea == null) {
					drtOperationArea = (Geometry) feature.getDefaultGeometry();
				} else {
					drtOperationArea = drtOperationArea.union((Geometry) feature.getDefaultGeometry());
				}
			} else {
				drtOperationArea = avOperationArea.getFactory().createPoint();
			}

			if (feature.getAttribute("mode").equals("av")) {
				if (avOperationArea == null) {
					avOperationArea = (Geometry) feature.getDefaultGeometry();
				} else {
					avOperationArea = avOperationArea.union((Geometry) feature.getDefaultGeometry());
				}
			} else {
				avOperationArea = drtOperationArea.getFactory().createPoint();
			}
		}

		for (Link link : network.getLinks().values()) {
			if (!link.getAllowedModes().contains("car")) {
				continue;
			}

			boolean isDrtAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(drtOperationArea) &&
					MGC.coord2Point(link.getToNode().getCoord()).within(drtOperationArea);
			boolean isAvAllowed = MGC.coord2Point(link.getFromNode().getCoord()).within(avOperationArea) &&
					MGC.coord2Point(link.getToNode().getCoord()).within(avOperationArea);

			if (isDrtAllowed) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.addAll(modesToAdd);
				link.setAllowedModes(allowedModes);
			}

			if (isAvAllowed) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.addAll(modesToAdd);
				link.setAllowedModes(allowedModes);
			}
		}
		MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
		multimodalNetworkCleaner.run(modesToAdd);

		log.log(Level.INFO, "The following modes have been added to the network: {}", modes);
	}

	/**
	 * Adapt network to one or more car-free zones. Therefore, a shape file of the wished car-free area is needed.
	 */
	static void prepareCarFree(Network network, ShpOptions shp, String modes) {
		Set<String> modesToRemove = new HashSet<>(Arrays.asList(modes.split(",")));

		//get all links in shp that allow at least one of modesToRemove
		Set<Id<Link>> modeLinksInArea = getFilteredLinksInArea(network, shp,
			link -> link.getAllowedModes().stream().anyMatch(mode -> modesToRemove.contains(mode)));
		deleteModesFromLinks(network, modeLinksInArea, modesToRemove);

		log.info("Car free areas have been added to network.");
	}

	/**
	 * mutate the allowedModes field for all links in the network whose id is contained in linkIds such that it does not contain any of the given modes.
	 */
	private static void deleteModesFromLinks(Network network, Set<Id<Link>> linkIds, Set<String> modes){
		for (Id<Link> linkId: linkIds){
			Link link = network.getLinks().get(linkId);
			Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
			for (String mode : modes) {
				allowedModes.remove(mode);
			}
			link.setAllowedModes(allowedModes);
		}

		MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
		modes.forEach(m -> multimodalNetworkCleaner.run(Set.of(m)));
	}

	/**
	 * return all links in the network that are inside the shp and fulfill the predicate.
	 */
	static Set<Id<Link>> getFilteredLinksInArea(Network network, ShpOptions shp, Predicate<Link> filter) {

		Geometry carFreeArea = shp.getGeometry();
		GeometryFactory gf = new GeometryFactory();
		Set<Id<Link>> modeLinksInArea = new HashSet<>();

		for (Link link : network.getLinks().values()) {
			if (!filter.test(link)){
				continue;
			}

			LineString line = gf.createLineString(new Coordinate[]{
					MGC.coord2Coordinate(link.getFromNode().getCoord()),
					MGC.coord2Coordinate(link.getToNode().getCoord())
			});

			boolean isInsideCarFreeZone = line.intersects(carFreeArea);
			if (isInsideCarFreeZone){
				modeLinksInArea.add(link.getId());
			}
		}
		return modeLinksInArea;
	}

	/**
	 * Add parking cost to network links. Therefore, a shape file of the  parking area is needed.
     */
	static void prepareParkingCost(Network network, ShpOptions parkingCostShape) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(String.valueOf(parkingCostShape.getShapeFile()));

		String hourlyParkingCostAttrName = "cost_h";
		String residentialParkingCostAttrName = "resPFee";

		for (var link : network.getLinks().values()) {

			if (!link.getAllowedModes().contains("pt")) {

				double oneHourPCost = 0.;
				double extraHourPCost = 0.;
				double resPFee = 0.;

				for (SimpleFeature feature : features) {
					Geometry geometry = (Geometry) feature.getDefaultGeometry();
					// Here we have to use a different logic for checking whether a link is inside the given geometry.
					// The standard procedure (as used in the other methods of this class) did include some link out of the geometry.
					//for this case we have to be more precise than that. -sme0723
					boolean linkInShp = MGC.coord2Point(link.getCoord()).within(geometry);

					if (linkInShp && feature.getAttribute(hourlyParkingCostAttrName) != null) {

						oneHourPCost = (Double) feature.getAttribute(hourlyParkingCostAttrName);
						extraHourPCost = (Double) feature.getAttribute(hourlyParkingCostAttrName);
					}

					if (linkInShp && feature.getAttribute(residentialParkingCostAttrName) != null) {
						resPFee = (Double) feature.getAttribute(residentialParkingCostAttrName);
						LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(link);
					}

					LeipzigUtils.setFirstHourParkingCost(link, oneHourPCost);
					LeipzigUtils.setExtraHourParkingCost(link, extraHourPCost);
					LeipzigUtils.setResidentialParkingCost(link, resPFee);
				}
			}
		}

		log.info("Parking cost information has been added to network.");

	}

	/**
	 * Add parking capacities per link to network links. Therefore, a shape file of the parking area + a csv with capacity data are needed.
	 * For the creation of a capacities.csv see {@link org.matsim.analysis.ParkedVehiclesAnalysis}.
	 */

	static void prepareParkingCapacities(Network network, ShpOptions parkingArea, Path inputParkingCapacities) {

		ParkingCapacitiesAttacher attacher = new ParkingCapacitiesAttacher(network, parkingArea, inputParkingCapacities);
		attacher.addParkingInformationToLinks();

		log.info("Parking capacity information has been added to network.");
	}

	/**
	 * Reduce speed of link in certain zone.
	 */
	static void prepareSlowSpeed(Network network, List<PreparedGeometry> geometries, Double relativeSpeedChange) {

		Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
				//filter car links
				.filter(link -> link.getAllowedModes().contains(TransportMode.car))
				//spatial filter
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
				//we won't change motorways and motorway_links
				.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
				.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("trunk"))
				.collect(Collectors.toSet());

		if (relativeSpeedChange >= 0.0 && relativeSpeedChange < 1.0) {
			log.info("reduce speed relatively by a factor of: {}", relativeSpeedChange);

			//apply 'tempo 20' to all roads but motorways
			carLinksInArea.forEach(link -> link.setFreespeed(link.getFreespeed() * relativeSpeedChange));

		} else {
			log.info("reduce speed to 20 km/h");
			carLinksInArea.forEach(link ->
				//apply 'tempo 20' to all roads but motorways
				//20 km/h --> 5.5 m/s
				link.setFreespeed(5.5)
			);
		}

	}

}
