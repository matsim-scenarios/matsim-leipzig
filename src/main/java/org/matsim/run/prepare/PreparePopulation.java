package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SplittableRandom;

@CommandLine.Command(
		name = "population",
		description = "Set the car availability attribute in the population"
)
public class PreparePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PreparePopulation.class);

	private final SplittableRandom rnd = new SplittableRandom(1234);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());

		for (Person person : population.getPersons().values()) {

			// Set car availability to "never" for agents below 18 years old
			// Standardize the attribute "age"
			String avail = "always";
			Object age = person.getAttributes().getAttribute("microm:modeled:age");
			if (age != null) {
				PersonUtils.setAge(person, (int) age);
				person.getAttributes().removeAttribute("microm:modeled:age");
				if ((int) age < 18) {
					avail = "never";
				}
			}

			// Replace with standardized car availability attribute
			PersonUtils.setCarAvail(person, avail);
			person.getAttributes().removeAttribute("sim_carAvailability");

			// Standardize the attribute "sex"
			Object sex = person.getAttributes().getAttribute("microm:modeled:sex");
			if (sex != null) {
				PersonUtils.setSex(person, (String) sex);
				person.getAttributes().removeAttribute("microm:modeled:sex");
			}

			// Assign income to person (skip the freight agents)
			if (person.getId().toString().startsWith("freight")) {
				continue;
			}

			String incomeGroupString = (String) person.getAttributes().getAttribute("MiD:hheink_gr2");
			String householdSizeString = (String) person.getAttributes().getAttribute("MiD:hhgr_gr");
			int incomeGroup = 0;
			double householdSize = 1;
			if (incomeGroupString != null && householdSizeString != null) {
				incomeGroup = Integer.parseInt(incomeGroupString);
				householdSize = Double.parseDouble(householdSizeString);
			}

			double income = switch (incomeGroup) {
				case 1 -> 500 / householdSize;
				case 2 -> (rnd.nextInt(400) + 500) / householdSize;
				case 3 -> (rnd.nextInt(600) + 900) / householdSize;
				case 4 -> (rnd.nextInt(500) + 1500) / householdSize;
				case 5 -> (rnd.nextInt(1000) + 2000) / householdSize;
				case 6 -> (rnd.nextInt(1000) + 3000) / householdSize;
				case 7 -> (rnd.nextInt(1000) + 4000) / householdSize;
				case 8 -> (rnd.nextInt(1000) + 5000) / householdSize;
				case 9 -> (rnd.nextInt(1000) + 6000) / householdSize;
				case 10 -> (Math.abs(rnd.nextGaussian()) * 1000 + 7000) / householdSize;
				default -> 2364;
				// Average monthly household income per Capita (2021). See comments below for details
				// Average Gross household income: 4734 Euro
				// Average household size: 83.1M persons /41.5M households = 2.0 persons / household
				// Average household income per capita: 4734/2.0 = 2364 Euro
				// Source (Access date: 21 Sep. 2021):
				// https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
				// https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
				// https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html;jsessionid=E0D7A060D654B31C3045AAB1E884CA75.live711
			};
			PersonUtils.setIncome(person, income);
		}


		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}
}
