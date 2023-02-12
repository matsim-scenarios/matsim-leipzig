package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Defines all options and parameters related to network modifications.
 */
public class NetworkOptions {

	private static final Logger log = LogManager.getLogger(NetworkOptions.class);

	@CommandLine.Option(names = "--drt-area", description = "Path to SHP file specifying where DRT mode is allowed")
	private Path drtArea;
	@CommandLine.Option(names = "--drt-modes", description = "List of modes to add. Use comma as delimiter", defaultValue = TransportMode.drt)
	private String drtModes;
	@CommandLine.Option(names = "--car-free-area", description = "Path to SHP file specifying car-free area")
	private Path carFreeArea;
	@CommandLine.Option(names = "--car-free-modes", description = "List of modes to remove. Use comma as delimiter", defaultValue = TransportMode.car)
	private String carFreeModes;
	@CommandLine.Option(names = "--parking-area", description = "Path to SHP file specifying parking area")
	private Path parkingArea;
	@CommandLine.Option(names = "--parking-capacities", description = "Path to csv file containing parking capacity data per link")
	private Path inputParkingCapacities;
	@CommandLine.Option(names = "--parking-cost-first-hour", description = "Parking cost for first hour. Needed for ParkingCostModule", defaultValue = "0.0")
	private String firstHourParkingCost;
	@CommandLine.Option(names = "--parking-cost-extra-hour", description = "Parking cost for every extra hour. Needed for ParkingCostModule", defaultValue = "0.0")
	private String extraHourParkingCost;
	@CommandLine.Option(names = "--city-area", description = "Path to SHP file specifying city area")
	private Path cityArea;

	/**
	 * Return whether a car free area is defined.
	 */
	public boolean hasCarFreeArea() {
		return isDefined(carFreeArea);
	}


	/**
	 * Prepare network with given options.
	 */
	public void prepare(Network network) {

		if (isDefined(drtArea)) {
			if (!Files.exists(drtArea))
				throw new IllegalArgumentException("Path to drt area not found: " + drtArea);

			PrepareNetwork.prepareDRT(network, new ShpOptions(drtArea, null, null), drtModes);
		}

		if (isDefined(carFreeArea)) {
			if (!Files.exists(carFreeArea))
				throw new IllegalArgumentException("Path to car free area not found: " + carFreeArea);

			PrepareNetwork.prepareCarFree(network, new ShpOptions(carFreeArea, null, null), carFreeModes);
		}

		if (isDefined(inputParkingCapacities)) {
			if (parkingArea == null)
				log.warn("No shp file of parking area was defined. Attributes are added for all network links.");
			if (!Files.exists(inputParkingCapacities))
				throw new IllegalArgumentException("Path to parking capacities information not found: " + inputParkingCapacities);

			PrepareNetwork.prepareParking(network,new ShpOptions(parkingCostArea, null, null), new ShpOptions(parkingArea, null, null), inputParkingCapacities);
		}

		if (isDefined(cityArea)) {
			if (!Files.exists(cityArea))
				throw new IllegalArgumentException("Path to city area not found: " + cityArea);

			PrepareNetwork.prepareCityArea(network, new ShpOptions(cityArea, null, null));
		}


	}

	private boolean isDefined(Path p) {
		return p != null;
	}

}
