package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.nio.file.Path;
import java.util.*;
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

		log.info("Modes " + modes + " have been added to network.");
	}

	/**
	 * Adapt network to one or more car-free zones. Therefore, a shape file of the wished car-free area is needed.
	 */
	static void prepareCarFree(Network network, ShpOptions shp, String modes) {
		Set<String> modesToRemove = new HashSet<>(Arrays.asList(modes.split(",")));

		Geometry carFreeArea = shp.getGeometry();
		GeometryFactory gf = new GeometryFactory();

		for (Link link : network.getLinks().values()) {

			if (!link.getAllowedModes().contains(TransportMode.car)) {
				continue;
			}

			LineString line = gf.createLineString(new Coordinate[]{
					MGC.coord2Coordinate(link.getFromNode().getCoord()),
					MGC.coord2Coordinate(link.getToNode().getCoord())
			});

			boolean isInsideCarFreeZone = line.intersects(carFreeArea);

			if (isInsideCarFreeZone) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

				for (String mode : modesToRemove) {
					allowedModes.remove(mode);
				}
				link.setAllowedModes(allowedModes);
			}
		}

		MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
		modesToRemove.forEach(m -> multimodalNetworkCleaner.run(Set.of(m)));

		log.info("Car free areas have been added to network.");

	}

	/**
	 * Add parking cost to network links. Therefore, a shape file of the  parking area is needed
     */
	static void prepareParkingCost(Network network, ShpOptions parkingCostShape) {
		ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(new Config(), ParkingCostConfigGroup.class);
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(String.valueOf(parkingCostShape.getShapeFile()));

		String hourlyParkingCostAttrName = "cost_h";
		String residentialParkingCostAttrName = "resPFee";

		for (var link : network.getLinks().values()) {

			if (!link.getAllowedModes().contains("pt")) {

				GeometryFactory gf = new GeometryFactory();

				LineString line = gf.createLineString(new Coordinate[]{MGC.coord2Coordinate(link.getFromNode().getCoord()),
						MGC.coord2Coordinate(link.getToNode().getCoord())});
				double oneHourPCost = 0.;
				double extraHourPCost = 0.;
				double resPFee = 0.;

				for (SimpleFeature feature : features) {
					Geometry geometry = (Geometry) feature.getDefaultGeometry();
					boolean linkInShp = line.intersects(geometry);
					if (linkInShp) {
						if (feature.getAttribute(hourlyParkingCostAttrName) != null) {
							oneHourPCost = (Double) feature.getAttribute(hourlyParkingCostAttrName);
							extraHourPCost = (Double) feature.getAttribute(hourlyParkingCostAttrName);

						}

						if (feature.getAttribute(residentialParkingCostAttrName) != null) {
							resPFee = (Double) feature.getAttribute(residentialParkingCostAttrName);
						}

						break;

					}
				}

				link.getAttributes().putAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName(), oneHourPCost);
				link.getAttributes().putAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName(), extraHourPCost);
				link.getAttributes().putAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName(), resPFee);
			}
		}

		log.info("Parking cost information has been added to network.");

	}

	/**
	 * Add parking capacities per link to network links. Therefore, a shape file of the parking area + a csv with capacity data are needed
	 * For the creation of a capacities.csv see {@link org.matsim.analysis.ParkedVehiclesAnalysis}
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
