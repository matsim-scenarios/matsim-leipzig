package org.matsim.analysis.inputPlansAnalysis;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class InputPlansAnalysis {
	public static void main(String[] args) throws IOException {
		Population plans = PopulationUtils.readPopulation("/Users/luchengqi/Documents/MATSimScenarios/Lepzig/TUD-analysis/leipzig-v1.3.1-10pct.plans.xml.gz");
//		Set<String> setOfActivities = new HashSet<>();

		CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Lepzig/TUD-analysis/leipzig-v1.3.1-10pct-trip-analysis.tsv"), CSVFormat.TDF);
		csvPrinter.printRecord("person_id", "age", "car_availability", "income", "househould_income", "total_trips",
			"errands", "shopping", "leisure", "educ", "work", "visit");

		for (Person person : plans.getPersons().values()) {
			if (!person.getAttributes().getAttribute("subpopulation").toString().equals("person")) {
				// not a normal person (e.g., freight)
				continue;
			}

			if (PersonUtils.getAge(person) == null) {
				// probably not a normal agent (e.g., parking agent)
				continue;
			}

			int age = PersonUtils.getAge(person);
			double income = PersonUtils.getIncome(person);
			int householdSize = Integer.parseInt(person.getAttributes().getAttribute("MiD:hhgr_gr").toString());
			double householdIncome = income * householdSize;
			String carAvailability = PersonUtils.getCarAvail(person);
			Plan plan = person.getSelectedPlan();
			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);

			int totalTrips = 0;
			int errandsTrips = 0;
			int shoppingTrips = 0;
			int leisureTrips = 0;
			int educationTrips = 0;
			int workTrips = 0;
			int visitTrips = 0;

			for (TripStructureUtils.Trip trip : trips) {
				totalTrips++;
				String destActType = trip.getDestinationActivity().getType();
				if (destActType.contains("errands")) {
					errandsTrips++;
				}
				if (destActType.contains("shop")) {
					shoppingTrips++;
				}
				if (destActType.contains("leisure")) {
					leisureTrips++;
				}
				if (destActType.contains("educ")) {
					educationTrips++;
				}
				if (destActType.contains("work")) {
					workTrips++;
				}
				if (destActType.contains("visit")) {
					visitTrips++;
				}
			}

			csvPrinter.printRecord(person.getId().toString(), Integer.toString(age), carAvailability, Double.toString(income),
				Double.toString(householdIncome), Integer.toString(totalTrips), Integer.toString(errandsTrips), Integer.toString(shoppingTrips),
				Integer.toString(leisureTrips), Integer.toString(educationTrips), Integer.toString(workTrips), Integer.toString(visitTrips)
			);
		}
		csvPrinter.close();
	}

}
