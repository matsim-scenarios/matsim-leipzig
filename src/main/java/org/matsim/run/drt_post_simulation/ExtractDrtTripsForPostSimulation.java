package org.matsim.run.drt_post_simulation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.matsim.application.ApplicationUtils.globFile;

public class ExtractDrtTripsForPostSimulation implements MATSimAppCommand {
	@CommandLine.Option(names = "--directory", description = "path to the directory of the simulation output", required = true)
	private Path directory;

	@CommandLine.Option(names = "--drt-operators", description = "path to the directory of the simulation output", arity = "1..*", defaultValue = "drt")
	private String[] drtOperators;

	public static void main(String[] args) {
		new ExtractDrtTripsForPostSimulation().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Population outputDrtPlans = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory populationFactory = outputDrtPlans.getFactory();
		int counter = 0;

		for (String drtOperator : drtOperators) {
			Path drtLegsFile = globFile(directory, "*output_drt_legs_" + drtOperator + ".csv*");
			try (CSVParser parser = new CSVParser(Files.newBufferedReader(drtLegsFile),
				CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
				for (CSVRecord row : parser.getRecords()) {
					double departureTime = Double.parseDouble(row.get("departureTime"));
					Id<Link> fromLinkId = Id.createLinkId(row.get("fromLinkId"));
					Id<Link> toLinkId = Id.createLinkId(row.get("toLinkId"));

					Activity fromAct = populationFactory.createActivityFromLinkId("dummy", fromLinkId);
					fromAct.setEndTime(departureTime);
					Activity toAct = populationFactory.createActivityFromLinkId("dummy", toLinkId);
					Leg leg = populationFactory.createLeg(drtOperator);
					leg.setMode(TransportMode.drt);

					Plan plan = populationFactory.createPlan();
					Person person = populationFactory.createPerson(Id.createPersonId("dummy_person_" + counter));
					plan.addActivity(fromAct);
					plan.addLeg(leg);
					plan.addActivity(toAct);
					person.addPlan(plan);
					outputDrtPlans.addPerson(person);
					counter++;
				}
			}
		}

		new PopulationWriter(outputDrtPlans).write(directory.toString() + "/extracted-drt-plans.xml.gz");
		return 0;
	}
}
