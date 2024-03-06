package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.run.RunLeipzigScenario;
import picocli.CommandLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

@CommandLine.Command(
	name = "population",
	description = "Prepares person attributes and plans."
)
public class PreparePopulation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PreparePopulation.class);

	private final SplittableRandom rnd = new SplittableRandom(1234);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path to output population", required = true)
	private Path output;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--phase", description = "Run pre or post phase", required = true)
	private Phase phase;

	public static void main(String[] args) {
		new PreparePopulation().execute(args);
	}

	private static double round(double x) {
		return BigDecimal.valueOf(x).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(input)) {
			log.error("Input population does not exist: {}", input);
			return 2;
		}

		if (!shp.isDefined()) {
			log.error("Shp file is required to prepare relevant area.");
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.toString());
		ProjectionUtils.putCRS(population, RunLeipzigScenario.CRS);

		ShpOptions.Index index = shp.createIndex(RunLeipzigScenario.CRS, "_");

		switch (phase) {
			case pre -> prePhase(population);
			case post -> postPhase(population, index);
			default -> throw new IllegalArgumentException("Illegal phase");
		}


		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * This step runs before any other steps.
	 */
	private void prePhase(Population population) {

		Set<Id<Person>> toRemove = new HashSet<>();

		for (Person person : population.getPersons().values()) {

			List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			for (Activity act : activities) {
				// Remove persons having activities after 27hours
				if (act.getStartTime().isDefined() && act.getStartTime().seconds() > 27 * 3600)
					toRemove.add(person.getId());
			}

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

		log.info("Removing {} out of {} persons with activities outside valid range", toRemove.size(), population.getPersons().size());
		toRemove.forEach(population::removePerson);

	}

	/**
	 * This step runs at the very end. We need this to define outside persons to calculate the scores separately as agents far outside the study area tend to have very negative scores.
	 * This can only run after every other step is completed.
	 */
	private void postPhase(Population population, ShpOptions.Index index) {

		for (Person person : population.getPersons().values()) {

			List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
			for (Activity act : activities) {
				// Reduce unnecessary precision
				if (act.getCoord() != null) {
					act.setCoord(new Coord(round(act.getCoord().getX()), round(act.getCoord().getY())));
				}
			}

			// Only handles persons
			if (!"person".equals(PopulationUtils.getSubpopulation(person)))
				continue;

			Coord home = setHomeCoordinate(person);
			if (home == null || !index.contains(home)) {
				PopulationUtils.putSubpopulation(person, "outside_person");
			}
		}
	}

	/**
	 * Set and return home coordinate of this person. Can be null if no home activity is known.
	 */
	private Coord setHomeCoordinate(Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					String actType = ((Activity) planElement).getType();
					if (actType.startsWith("home")) {
						Coord homeCoord = ((Activity) planElement).getCoord();

						person.getAttributes().putAttribute("home_x", homeCoord.getX());
						person.getAttributes().putAttribute("home_y", homeCoord.getY());

						return homeCoord;
					}
				}
			}

		}

		return null;
	}

	enum Phase {pre, post}

	/**
	 * deletes all car and ride routes from the population that contain at least one of the given links.
	 */
	//TODO remove hardcoding of the transport modes??
	public static void deleteCarAndRideRoutesThatHaveForbiddenLinks(Population population, Set<Id<Link>> forbiddenLinks) {
//		log.info("start deleting every car route that travels one or more links within car-free-zone");

		population.getPersons().values().stream()
			.forEach(person -> person.getPlans().stream().flatMap(plan ->
					TripStructureUtils.getLegs(plan).stream())
				.forEach(leg -> {
					if (leg.getMode().equals(TransportMode.car) || leg.getMode().equals(TransportMode.ride)){
						Route route = leg.getRoute();
						if (route != null){
							boolean routeTouchesZone = (route instanceof NetworkRoute && ((NetworkRoute) route).getLinkIds().stream().filter(l -> forbiddenLinks.contains(l)).findAny().isPresent() );
							if (routeTouchesZone || forbiddenLinks.contains(route.getStartLinkId()) || forbiddenLinks.contains(route.getEndLinkId()) ){
								leg.setRoute(null);
							}
						}
					}
				}));

//		log.info(".... finished deleting every car route that travels one or more links within car-free-zone");
	}

}
