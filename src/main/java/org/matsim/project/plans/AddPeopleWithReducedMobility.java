package org.matsim.project.plans;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Random;

public class AddPeopleWithReducedMobility {

	public static void main(String[] args) {

		Random rnd = new Random();
		int edited = 0;


		//default config because we want to edit population.xml
		Population population = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3.1-1pct.config.xml")).getPopulation();


		for (Person person : population.getPersons().values()) {

			boolean isNum;

			//Is person an actual person or dummy traffic?
			//Only actual people have exclusive number IDs, other has strings as prefixes
			try{
				Double.parseDouble(person.getId().toString());
				isNum = true;
			} catch (NumberFormatException e){
				isNum = false;
			}

			if(isNum) {

				Attributes personAttributes = person.getAttributes();
				if(rnd.nextDouble(100) < 1.6) {

					personAttributes.putAttribute("HAS_REDUCED_MOBILITY", true);
					edited++;

				}else personAttributes.putAttribute("HAS_REDUCED_MOBILITY", false);
			}
		}

		System.out.println(edited + " people should now have a PRM true modifier.");

		PopulationWriter populationWriter = new PopulationWriter(population);

		populationWriter.write("input/v1.3/population.xml");
	}
}
