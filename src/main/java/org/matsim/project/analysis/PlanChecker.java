package org.matsim.project.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanChecker {

	public static void main(String[] args) {


		String scenarioFile = "output_scenario/output-leipzig-1pct/ITERS/it.350/leipzig-1pct.350.experienced_plans.xml.gz";
		String baseCaseFile = "output_basecase/output-leipzig-1pct/ITERS/it.350/leipzig-1pct.350.experienced_plans.xml.gz";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(scenarioFile);

		printScores(population);
		printModes(population);
		printTravelTimes(population);
	}

	//prints score of agents going to the stadium
	public static void printScores(Population population){

		double totalScore =0;
		double agents = 0;
		for (Person person : population.getPersons().values()) {

			for(PlanElement element : person.getSelectedPlan().getPlanElements()) {

				if(element instanceof Activity) {

					if(((Activity) element).getType().equals("stadium")) {

						totalScore += person.getSelectedPlan().getScore();
						agents+= 1;
						break;
					}
				}
			}
		}
		System.out.println(agents + " agents with a total score of " + totalScore);
		System.out.println("AverageScore: " + totalScore/agents);
	}


	//prints mode shares for agents going to the stadium
	public static void printModes(Population population){

		int car = 0;
		int ride = 0;
		int pt = 0;
		int walk = 0;
		int bike = 0;
		int agents = 0;

		for (Person person : population.getPersons().values()) {

			for (int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {

				PlanElement element = person.getSelectedPlan().getPlanElements().get(i);
				if(element instanceof Activity){

					if (((Activity) element).getType().equals("stadium")){

						agents++;
						PlanElement legUsed = person.getSelectedPlan().getPlanElements().get(i-1); //change to i+1 to get mode shares for agents leaving the stadium (doesn't work on iteration 0 then)
						if(legUsed instanceof Leg leg){

							if(leg.getRoutingMode().equals("car"))
								car++;
							if(leg.getRoutingMode().equals("walk"))
								walk++;
							if(leg.getRoutingMode().equals("bike"))
								bike++;
							if(leg.getRoutingMode().equals("pt"))
								pt++;
							if(leg.getRoutingMode().equals("ride"))
								ride++;
							break;

						}
					}
				}
			}
		}

		System.out.println(agents + " agents going to/leaving the stadium");
		System.out.println(car + " car users make up for" + (double) car/agents + " percent");
		System.out.println(bike + " bike users make up for" + (double) bike/agents + " percent");
		System.out.println(walk + " walk users make up for" + (double) walk/agents + " percent");
		System.out.println(pt + " pt users make up for" + (double) pt/agents + " percent");
		System.out.println(ride + " ride users make up for" + (double) ride/agents + " percent");
	}

	//prints average travelTime for agents from their last usual activity to the stadium
	public static void printTravelTimes(Population population) {

		double totalTravelTime = 0;
		double agents = 0;

		for (Person person : population.getPersons().values()) {

			double endTime = 0;
			double startTime = 0;
			double travelTime = 0;
			int i;
			boolean foundStadium = false;

			for (i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {

				PlanElement stadiumElement = person.getSelectedPlan().getPlanElements().get(i);

				if (stadiumElement instanceof Activity) {

					if (((Activity) stadiumElement).getType().equals("stadium")) {

						startTime = ((Activity) stadiumElement).getStartTime().seconds();
						foundStadium = true;
						agents++;
						break;
					}
				}
			}


			if(foundStadium) {
				while (i >= 0) {
					i--;
					PlanElement element = person.getSelectedPlan().getPlanElements().get(i);
					if (element instanceof Activity) {

						if(!((Activity) element).getType().equals("ride interaction") && !((Activity) element).getType().equals("car interaction") && !((Activity) element).getType().equals("bike interaction") && !((Activity) element).getType().equals("pt interaction")) {

							endTime = ((Activity) element).getEndTime().seconds();
							break;

						}
					}
				}
				travelTime = startTime - endTime;
			}
			totalTravelTime += travelTime;
		}
		System.out.println("Agents: " + agents);
		System.out.println("Traveltime: " +  totalTravelTime);
		System.out.println("Per agent: " + totalTravelTime/agents);
	}
}
