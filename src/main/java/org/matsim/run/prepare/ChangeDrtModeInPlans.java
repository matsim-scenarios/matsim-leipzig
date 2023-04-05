package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.LeipzigMainModeIdentifier;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Replace DRT modes with specified new mode.
 */
public class ChangeDrtModeInPlans implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ChangeDrtModeInPlans.class);

	@CommandLine.Option(names = "--plans", description = "Input original plan file. Should be selected + cleaned plans.", required = true)
	private Path plans;

	@CommandLine.Option(names = "--old-modes", description = "List of modes to change. Use comma as delimiter", required = true, defaultValue = TransportMode.drt)
	private String modes;

	@CommandLine.Option(names = "--new-mode", description = "Mode to replace old modes", required = true, defaultValue = TransportMode.drt)
	private String modeToReplace;

	@CommandLine.Option(names = "--output", description = "Output file name", required = true)
	private Path output;

	public static void main(String[] args) {
		new ChangeDrtModeInPlans().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Population population = PopulationUtils.readPopulation(plans.toString());
		TripsToLegsAlgorithm trips2Legs = new TripsToLegsAlgorithm(new LeipzigMainModeIdentifier());

		Set<String> modesToDelete = new HashSet<>(Arrays.asList(modes.split(",")));

		for (Person person : population.getPersons().values()) {

			for (Plan plan : person.getPlans()) {
				trips2Legs.run(plan);

				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Leg) {
						if (modesToDelete.contains(((Leg) el).getMode())) {
							((Leg) el).setMode(modeToReplace);
						} else {
							if (modesToDelete.contains(el.getAttributes().getAttribute("routingMode"))) {
								TripStructureUtils.setRoutingMode((Leg) el, modeToReplace);
							}
						}
					}
				}
			}
		}
		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
