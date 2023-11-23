package org.matsim.run.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunLeipzigScenario;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Defines all options and parameters related to network modifications.
 */
public class NetworkOptions {

	@CommandLine.Option(names = "--drt-area", description = "Path to SHP file specifying where DRT mode is allowed")
	private Path drtArea;
	@CommandLine.Option(names = "--drt-modes", description = "List of modes to add. Use comma as delimiter", defaultValue = TransportMode.drt)
	private String drtModes;
	@CommandLine.Option(names = "--car-free-area", description = "Path to SHP file specifying car-free area")
	private Path carFreeArea;
	@CommandLine.Option(names = "--car-free-modes-to-delete", description = "List of modes to remove. Use comma as delimiter", defaultValue = TransportMode.car + "," + TransportMode.ride)
	private String carFreeModesToDelete;
	@CommandLine.Option(names = "--parking-capacities-area", description = "Path to SHP file specifying parking area for adding parking capacities")
	private Path parkingCapacitiesArea;
	@CommandLine.Option(names = "--parking-capacities-input", description = "Path to csv file containing parking capacity data per link")
	private Path inputParkingCapacities;
	@CommandLine.Option(names = "--parking-cost-area", description = "Path to SHP file specifying parking cost area", required = true, defaultValue = "input/v" + RunLeipzigScenario.VERSION + "/parkingCostArea/Bewohnerparken_2020.shp")
	private Path parkingCostArea;
	@CommandLine.Option(names = "--slow-speed-area", description = "Path to SHP file specifying area of adapted speed")
	private Path slowSpeedArea;
	@CommandLine.Option(names = "--slow-speed-relative-change", description = "provide a value that is bigger than 0.0 and smaller than 1.0")
	private Double slowSpeedRelativeChange;

	/**
	 * Return whether a car free area is defined.
	 */
	public boolean hasCarFreeArea() {
		return isDefined(carFreeArea);
	}

	/**
	 * Return whether a drt area is defined.
	 */
	public boolean hasDrtArea() {
		return isDefined(drtArea);
	}

	/**
	 * Return whether a parkingCost area is defined.
	 */
	public boolean hasParkingCostArea() {
		return isDefined(parkingCostArea);
	}

	/**
	 * Prepare network with given options.
	 */
	public void prepare(Network network) {

		if (hasDrtArea()) {
			if (!Files.exists(drtArea)) {
				throw new IllegalArgumentException("Path to drt area not found: " + drtArea);
			} else {
				PrepareNetwork.prepareDRT(network, new ShpOptions(drtArea, null, null), drtModes);
			}
		}

		if (hasParkingCostArea()) {
			if (!Files.exists(parkingCostArea)) {
				throw new IllegalArgumentException("Path to parking cost shape information not found: " + parkingCostArea);
			} else {
				PrepareNetwork.prepareParkingCost(network, new ShpOptions(parkingCostArea, null, null));
			}
		}

		if (isDefined(slowSpeedArea)) {
			if (!Files.exists(slowSpeedArea)) {
				throw new IllegalArgumentException("Path to slow speed area not found: " + slowSpeedArea);
			} else if (slowSpeedRelativeChange == null) {
				throw new IllegalArgumentException("No relative change value for freeSpeed defined: " + slowSpeedArea);
			} else {
				PrepareNetwork.prepareSlowSpeed(network,
					ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(new ShpOptions(slowSpeedArea, null, null).getShapeFile().toString())),
					slowSpeedRelativeChange);
			}
		}

		if (isDefined(parkingCapacitiesArea)) {
			if (!Files.exists(parkingCapacitiesArea)) {
				throw new IllegalArgumentException("Path to parking capacities shape information not found: " + parkingCapacitiesArea);
			} else if (!Files.exists(inputParkingCapacities)) {
				throw new IllegalArgumentException("Path to parking capacities input file not found: " + inputParkingCapacities);
			} else {
				PrepareNetwork.prepareParkingCapacities(network, new ShpOptions(parkingCapacitiesArea, null, null), inputParkingCapacities);
			}
		}

		if (hasCarFreeArea()) {
			if (!Files.exists(carFreeArea)) {
				throw new IllegalArgumentException("Path to car free area not found: " + carFreeArea);
			} else {
				PrepareNetwork.prepareCarFree(network, new ShpOptions(carFreeArea, null, null), carFreeModesToDelete);
			}
		}
	}

	/**
	 * Return path to drt area.
	 */
	public Path getDrtArea() {
		return drtArea;
	}

	private boolean isDefined(Path p) {
		return p != null;
	}

	/**
	 * return ids for all links inside the car free area that do not allow public transport.
	 */
	public Set<Id<Link>> getNonPtLinksInCarFreeArea(Network network) {
		if (!hasCarFreeArea()){
			return null;
		}
		return PrepareNetwork.getFilteredLinksInArea(network, new ShpOptions(carFreeArea, null, null), l -> !l.getAllowedModes().contains(TransportMode.pt));
	}

}
