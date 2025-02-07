package org.matsim.project.plans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;


public class EditPlans {

	//kick-off is assumed to be 20:30, minus 30 minutes for security and walk to seats as latest startTime
	static final double startTime = 20 * 3600;

	public static double getStartTime(){

		return startTime;

	}

	public static void main(String[] args) {

		Random rnd = new Random();
		int populationSize = 0;
		//1% of original capacity due to sample size
		int stadiumCapacity = 450;

		Config config = ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3.1-1pct.config-EditPlans.xml");

		Population population = ScenarioUtils.loadScenario(config).getPopulation();

		PopulationFactory factory = population.getFactory();


		//stadium coord
		Coord stadiumCoord = new Coord(733385, 5693055);


		//get actual population size, excluding freight, etc.
		for(Person person : population.getPersons().values()) {

			boolean isNum;

			//is person an actual person or dummy traffic?
			//only actual people have exclusive number IDs, other has strings as prefixes
			try{
				Double.parseDouble(person.getId().toString());
				isNum = true;
			} catch (NumberFormatException e){
				isNum = false;
			}

			//also filters outside_persons
			if(isNum && !person.getAttributes().getAttribute("subpopulation").toString().equals("outside_person"))
				populationSize++;
		}


		//capacity of stadium by total persons
		double probability = (double) stadiumCapacity /populationSize;

		//to check if number of agents going to the stadium is close enough to its capacity
		int changed = 0;

		for(Person person : population.getPersons().values()) {

			boolean isNum;

			//is person an actual person or dummy traffic?
			try{
				Double.parseDouble(person.getId().toString());
				isNum = true;
			} catch (NumberFormatException e){
				isNum = false;
			}


			if(isNum && rnd.nextDouble(1) <= probability && !person.getAttributes().getAttribute("subpopulation").toString().equals("outside_person")) {

				//people ending their last activity before the stadium visit 1.5 to 3 hours before planning to enter
				double endLastActivityTime = startTime - 1.5 * 3600 - rnd.nextDouble(1.5) * 3600;

				changed++;

				Plan plan = person.getSelectedPlan();

				int planSize = plan.getPlanElements().size();
				int i;
				boolean lastActivityReached = false;
				boolean lastLegReached = false;

				//loop breaks off if the checked activity exceeds the start time, ideally leaving a Leg as last PlanElement (if such exists)
				for(i = 0; i < planSize; i++) {

					if(lastLegReached) {

						break;
					}

					PlanElement element = plan.getPlanElements().get(i);

					//activity exceeds starttime
					if(lastActivityReached && element instanceof  Leg){

						lastLegReached = true;

					}

					if(element instanceof Activity activity){

						if(activity.getStartTime().isDefined() && activity.getStartTime().seconds() >= endLastActivityTime){

							break;
							//from here activites in plan exceed starttime, last checked PlanElement was a Leg

						}

						if (activity.getStartTime().isDefined() && activity.getStartTime().seconds() < endLastActivityTime) {

							if(activity.getEndTime().isDefined() && activity.getEndTime().seconds() > endLastActivityTime) {

								activity.setEndTime(endLastActivityTime);
								lastActivityReached = true;

							} else if (activity.getMaximumDuration().isDefined() && activity.getMaximumDuration().seconds() + activity.getStartTime().seconds() > endLastActivityTime){

								activity.setMaximumDuration(endLastActivityTime - activity.getStartTime().seconds());
								lastActivityReached = true;

							}

						} else if(activity.getEndTime().isDefined() && activity.getEndTime().seconds() > endLastActivityTime) {

							activity.setEndTime(endLastActivityTime);
							lastActivityReached = true;

						}else if (activity.getStartTime().isUndefined() && activity.getEndTime().isUndefined()) {

							//this is the case if there is only one activity (staying at home) for the entire day

							activity.setEndTime(endLastActivityTime);
						}
					}
				}

				//deletes every PlanElement after the break point
				if (planSize > i) {
					plan.getPlanElements().subList(i, planSize).clear();
				}


				//getLast somehow doesn't work here
				PlanElement lastElement = plan.getPlanElements().get(plan.getPlanElements().size() - 1);

				//creating a new leg to the stadium, initially set to car
				Leg leg = factory.createLeg("car");

				if(lastElement instanceof Activity) {

					((Activity) lastElement).setEndTime(endLastActivityTime);

					plan.addLeg(leg);

				} else {

					PlanElement secondLastElement = plan.getPlanElements().get(plan.getPlanElements().size()-2);

					if (secondLastElement instanceof Activity) {

						((Activity) secondLastElement).setEndTime(endLastActivityTime);
						if (((Activity) secondLastElement).getStartTime().isDefined()) {
							((Activity) secondLastElement).setMaximumDuration(endLastActivityTime - ((Activity) secondLastElement).getStartTime().seconds());
						}
					}
				}

				//stadium end time
				double endTime = (22.5 + rnd.nextDouble(0.5))* 3600;

				Activity stadium = factory.createActivityFromCoord("stadium", stadiumCoord);
				//giving a default of 60 minutes for travel to the stadium
				stadium.setStartTime(endLastActivityTime+3600);
				stadium.setEndTime(endTime);
				plan.addActivity(stadium);

				plan.addLeg((Leg) plan.getPlanElements().get(plan.getPlanElements().size() - 2));


				Activity firstActivity = (Activity) plan.getPlanElements().get(0);
				Coord homeCoord = firstActivity.getCoord();
				String homeString = firstActivity.getType();
				Activity lastActivity = factory.createActivityFromCoord(homeString, homeCoord);
				lastActivity.setStartTime(endTime);
				plan.addActivity(lastActivity);
			}
		}

		System.out.println(changed + " agents should now visit the stadium.");

		PopulationWriter populationWriter = new PopulationWriter(population);

		populationWriter.write("input/v1.3/population.xml");
	}
}
