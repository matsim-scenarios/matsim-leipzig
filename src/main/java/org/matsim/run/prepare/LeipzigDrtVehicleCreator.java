package org.matsim.run.prepare;

import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.*;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "create-drt-vehicles",
		description = "Writes drt vehicles file"
)

public class LeipzigDrtVehicleCreator implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(LeipzigDrtVehicleCreator.class);

	private final Random random = MatsimRandom.getRandom();
	private final Vehicles vehicles = VehicleUtils.createVehiclesContainer();

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--network", description = "network file", required = true)
	private String network;

	@CommandLine.Option(names = "--vehicle-types-file", description = "path to existing vehicle types file. Vehicles will be added to this file. Use / instead of backslash.", required = true)
	private String vehTypesFile;

	@CommandLine.Option(names = "--drt-mode", description = "network mode for which the vehicle fleet is created", defaultValue = "drt")
	private String drtMode;

	@CommandLine.Option(names = "--no-vehicles", description = "no of vehicles per service area to create", required = true)
	private int noVehiclesPerArea;

	@CommandLine.Option(names = "--service-start-time", description = "start of vehicle service time in seconds", defaultValue = "18000")
	private double serviceStartTime;

	@CommandLine.Option(names = "--service-end-time", description = "end of vehicle service time in seconds", defaultValue = "86400")
	private double serviceEndTime;

	public static void main(String[] args) throws IOException {
		new LeipzigDrtVehicleCreator().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(network);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		Network drtNetwork = NetworkUtils.createNetwork();
		Set<String> modes = new HashSet<>();
		modes.add(drtMode);

		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		filter.filter(drtNetwork, modes);

		List<SimpleFeature> serviceAreas = shp.readFeatures();

		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
		reader.readFile(vehTypesFile);

		VehicleType drtType = null;

		//this is ugly hard coded and should maybe be converted into a run input parameter
		for (VehicleType type : vehicles.getVehicleTypes().values()) {
			if (type.getId().toString().contains("conventional")) {
				drtType = type;
			}
		}

		for (SimpleFeature serviceArea : serviceAreas) {
			createVehiclesByRandomPointInShape(serviceArea, drtNetwork, noVehiclesPerArea, serviceStartTime,
					serviceEndTime, serviceAreas.indexOf(serviceArea), drtType);
		}

		String string = vehTypesFile.split("xml")[0].substring(0, vehTypesFile.split("xml")[0].length() - 1) + "-scaledFleet-caseNamav-"
				+ noVehiclesPerArea + "veh.xml";

		//write files
		new MatsimVehicleWriter(vehicles).writeFile(vehTypesFile.split("xml")[0].substring(0, vehTypesFile.split("xml")[0].length() - 1) + "-scaledFleet-caseNamav-"
				+ noVehiclesPerArea + "veh.xml");
		writeVehStartPositionsCSV(drtNetwork, vehTypesFile.split("xml")[0].substring(0, vehTypesFile.split("xml")[0].length() - 1) + "-scaledFleet-caseNamav-"
				+ noVehiclesPerArea + "veh_startPositions.csv");

		return 0;
	}

	private void createVehiclesByRandomPointInShape(SimpleFeature feature, Network network, int noVehiclesPerArea,
													double serviceStartTime, double serviceEndTime, int serviceAreaCount, VehicleType drtType) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();

		for (int i = 0; i < noVehiclesPerArea; i++) {
			Link link = null;

			while (link == null) {
				Point randomPoint = getRandomPointInFeature(random, geometry);
				link = NetworkUtils.getNearestLinkExactly(network, MGC.point2Coord(randomPoint));

				if (MGC.coord2Point(link.getFromNode().getCoord()).within(geometry) &&
						MGC.coord2Point(link.getToNode().getCoord()).within(geometry)) {

				} else {
					link = null;
				}
			}

			Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(drtMode + serviceAreaCount + 0 + i), drtType);
			vehicle.getAttributes().putAttribute("dvrpMode", drtMode);
			vehicle.getAttributes().putAttribute("startLink", link.getId().toString());
			vehicle.getAttributes().putAttribute("serviceBeginTime", serviceStartTime);
			vehicle.getAttributes().putAttribute("serviceEndTime", serviceEndTime);
			vehicles.addVehicle(vehicle);
		}
	}

	//copied from BerlinShpUtils -sm0922
	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x;
		double y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		}
		while (!g.contains(p));
		return p;
	}

	//copied and adapted from matsim-berlin DrtVehicleCreator -sm0922
	private void writeVehStartPositionsCSV(Network drtNetwork, String outputFile) {
		Map<Id<Link>, Long> linkId2NrVeh = vehicles.getVehicles().values().stream().
				map(veh -> Id.createLinkId(veh.getAttributes().getAttribute("startLink").toString())).
				collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFile)), ';', '"', '"', "\n");
			writer.writeNext(new String[]{"link", "x", "y", "drtVehicles"}, false);
			linkId2NrVeh.forEach((linkId, numberVeh) -> {
				Coord coord = drtNetwork.getLinks().get(linkId).getCoord();
				double x = coord.getX();
				double y = coord.getY();
				writer.writeNext(new String[]{linkId.toString(), "" + x, "" + y, "" + numberVeh}, false);
			});

			writer.close();
		} catch (IOException e) {
			log.error(e);
		}
	}
}
