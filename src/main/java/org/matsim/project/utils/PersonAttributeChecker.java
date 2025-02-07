package org.matsim.project.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.util.ArrayList;

//this class writes two files containing the IDs of agents going to the stadium and having the HAS_REDUCED_MOBILITY true attribute
//this is to save on runtime while running the RunLeipzigScenario class with the CarUsePenaltyScoring active
public class PersonAttributeChecker implements Serializable {

	static ArrayList <String> hasReducedMobility = new ArrayList<>();
	static ArrayList <String> goesToStadium = new ArrayList<>();

	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile("input/v1.3/population.xml");

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

				if ((Boolean) person.getAttributes().getAttribute("HAS_REDUCED_MOBILITY")) {

					hasReducedMobility.add(person.getId().toString());
				}

				for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

					if(planElement instanceof Activity){

						if (((Activity) planElement).getType().equals("stadium")) {

							goesToStadium.add(person.getId().toString());

						}
					}
				}
			}
		}

		try {
			writeFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private static void writeFile() throws IOException {

		FileOutputStream fileOutputStreamHRM = new FileOutputStream("HRM.tmp");
		FileOutputStream fileOutputStreamGTS = new FileOutputStream("GTS.tmp");
		ObjectOutputStream objectOutputStreamHRM = new ObjectOutputStream(fileOutputStreamHRM);
		ObjectOutputStream objectOutputStreamGTS = new ObjectOutputStream(fileOutputStreamGTS);
		objectOutputStreamHRM.writeObject(hasReducedMobility);
		objectOutputStreamGTS.writeObject(goesToStadium);
		objectOutputStreamHRM.close();
		objectOutputStreamGTS.close();
		hasReducedMobility.clear();
	}


	private static void readFile() throws IOException, ClassNotFoundException {

		FileInputStream fileInputStreamHRM = new FileInputStream("HRM.tmp");
		FileInputStream fileInputStreamGTS = new FileInputStream("GTS.tmp");
		ObjectInputStream objectInputStreamHRM = new ObjectInputStream(fileInputStreamHRM);
		ObjectInputStream objectInputStreamGTS = new ObjectInputStream(fileInputStreamGTS);
		hasReducedMobility = (ArrayList<String>) objectInputStreamHRM.readObject();
		goesToStadium = (ArrayList<String>) objectInputStreamGTS.readObject();
		objectInputStreamHRM.close();
		objectInputStreamGTS.close();
	}


	public static boolean hasPersonAttribute(String personID, String attribute){

		try {
			readFile();
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		if(attribute.equalsIgnoreCase("hasReducedMobility")) {

			return hasReducedMobility.contains(personID);

		} else if (attribute.equalsIgnoreCase("goesToStadium")) {

			return goesToStadium.contains(personID);

		} else {
			return false;
		}
	}
}
