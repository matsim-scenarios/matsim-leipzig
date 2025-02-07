package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Deprecated.
 *
 * @deprecated Use @{@link org.matsim.application.prepare.population.CleanPopulation}
 */
@Deprecated
public class DeleteRoutesFromPlans {
//    private static final String inputConfig = "C:/Users/Simon/Desktop/leipzig-v1.0-test.with-drt.config.xml";

	private static final Logger log = LogManager.getLogger(DeleteRoutesFromPlans.class);

	public DeleteRoutesFromPlans() {
	}

	public static void main(String[] args) {

		String inputPop = "C:/Users/Simon/Desktop/deleteRoutesLeipzig/008.output_plans.xml.gz";
		String outputPop = "C:/Users/Simon/Desktop/deleteRoutesLeipzig/008.output_plans_cleaned.xml.gz";

		DeleteRoutesFromPlans routeDeleter = new DeleteRoutesFromPlans();
		routeDeleter.deleteRoutesFromPlans(inputPop, outputPop);
	}

	private Config deleteRoutesFromPlans(String inputPop, String outputPop) {

		Population pop = PopulationUtils.readPopulation(inputPop);

		Config config = ConfigUtils.createConfig();
		Population newPop = PopulationUtils.createPopulation(config);

		for (Person person : pop.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				PopulationUtils.resetRoutes(plan);
			}
			if (!newPop.getPersons().containsKey(person.getId())) {
				newPop.addPerson(person);
			}
		}
		PopulationUtils.writePopulation(newPop, outputPop);
		log.info("Pop with deleted routes written to: {}", outputPop);
		log.info("size of new population: {}", newPop.getPersons().size());
		log.info("size of old population: {}", pop.getPersons().size());
		config.plans().setInputFile(outputPop);
		return config;

	}
}
