package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.net.URL;
import java.util.*;

public class TimeRestrictedParkingCostHandlerTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	enum Situation { noTimeRestrictionDefined, timeRestrictionStartAndEndDefined,
		timeRestrictionStartDefined, timeRestrictionEndDefined, multipleHomeActivities }

	void createExamplePopulation(Population population, Scenario scenario, Situation situation ) {

		ParkingCostConfigGroup parkingCostConfigGroup = new ParkingCostConfigGroup();

		PopulationFactory factory = population.getFactory();

		Person person = factory.createPerson(Id.createPersonId(situation.toString()));
		Plan plan = factory.createPlan();

		Link startLink = scenario.getNetwork().getLinks().get(Id.createLinkId("76"));
		startLink.getAttributes().putAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName(), 1.);
		startLink.getAttributes().putAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName(), 1.);
		startLink.getAttributes().putAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName(), 1.);

		Link destinationLink = scenario.getNetwork().getLinks().get(Id.createLinkId("78"));
		destinationLink.getAttributes().putAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName(), 1.);
		destinationLink.getAttributes().putAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName(), 1.);
		destinationLink.getAttributes().putAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName(), 1.);

		Activity originActivity = factory.createActivityFromLinkId(SnzActivities.work.name(), startLink.getId());
		Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), destinationLink.getId());

		Leg carLeg = factory.createLeg(TransportMode.car);

		Person person2 = factory.createPerson(Id.createPersonId(situation + "_2"));
		Plan plan2 = factory.createPlan();


		switch (situation) {
			case noTimeRestrictionDefined -> {
				//1) no time restriction is set -> parkingCost charged at any time
				originActivity.setEndTime(75500.);
				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}

			case timeRestrictionStartAndEndDefined, timeRestrictionEndDefined -> {
				//2) start + end of parking period is set
				//3) start of parking period is not defined, the end is defined
				//	2.1) eventTime inside parking period -> charging of parking cost
				//	3.1) eventTime is earlier or equal to end of period -> charging
				originActivity.setEndTime(75500.);
				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
				//	2.2) eventime outside parking period -> no charging
				//	3.2) eventTime is later than end of period -> no charging
				Activity originActivity2 = factory.createActivityFromLinkId(SnzActivities.work.name(), startLink.getId());
				originActivity2.setEndTime(75700.);
				plan2.addActivity(originActivity2);
				plan2.addLeg(carLeg);
				plan2.addActivity(destinationActivity);

				person2.addPlan(plan2);
				population.addPerson(person2);
			}

			case timeRestrictionStartDefined -> {
				//4) //start of parking period is defined, the end is not defined
				//	4.1) eventTime is earlier than start of period -> no charging
				originActivity.setEndTime(28700.);
				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);

				//	4.2) eventTime is equal or later than start of period -> charging
				Activity originActivity2 = factory.createActivityFromLinkId(SnzActivities.work.name(), startLink.getId());
				originActivity2.setEndTime(28900.);
				plan2.addActivity(originActivity2);
				plan2.addLeg(carLeg);
				plan2.addActivity(destinationActivity);

				person2.addPlan(plan2);
				population.addPerson(person2);
			}
			case multipleHomeActivities -> {
				originActivity.setEndTime(75500.);
				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);

				// set res parking cost to a different value
				Link homeLink = scenario.getNetwork().getLinks().get(Id.createLinkId("70"));
				homeLink.getAttributes().putAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName(), 1.);
				homeLink.getAttributes().putAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName(), 1.);
				homeLink.getAttributes().putAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName(), 70.);

				Link workLink = scenario.getNetwork().getLinks().get(Id.createLinkId("80"));
				workLink.getAttributes().putAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName(), 1.);
				workLink.getAttributes().putAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName(), 1.);
				workLink.getAttributes().putAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName(), 80.);

				Link workLink2 = scenario.getNetwork().getLinks().get(Id.createLinkId("85"));
				workLink2.getAttributes().putAttribute(LeipzigUtils.getFirstHourParkingCostLinkAttributeName(), 1.);
				workLink2.getAttributes().putAttribute(LeipzigUtils.getExtraHourParkingCostLinkAttributeName(), 1.);
				workLink2.getAttributes().putAttribute(LeipzigUtils.getResidentialParkingFeeAttributeName(), 85.);

				Activity homeActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), homeLink.getId());
				Activity homeActivity2 = factory.createActivityFromLinkId(SnzActivities.home.name(), homeLink.getId());
				Activity workActivity = factory.createActivityFromLinkId(SnzActivities.work.name(), workLink.getId());
				Activity workActivity2 = factory.createActivityFromLinkId(SnzActivities.work.name(), workLink2.getId());

				homeActivity.setEndTime(55500.);
				workActivity.setEndTime(65900.);
				homeActivity2.setEndTime(70900.);
				workActivity2.setEndTime(76950.);

				plan2.addActivity(homeActivity);
				plan2.addLeg(carLeg);
				plan2.addActivity(workActivity);
				plan2.addLeg(carLeg);
				plan2.addActivity(homeActivity2);
				plan2.addLeg(carLeg);
				plan2.addActivity(workActivity2);
				person2.addPlan(plan2);
				population.addPerson(person2);
			}
		}
		person.addPlan(plan);
		population.addPerson(person);
	}

	@Test
	public void runTimeRestrictedParkingCostChessboardTest() {

		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml");

		Map<Situation, double[]> situations = new HashMap<>();

		//parking time period defined from 8am-9pm
		situations.put(Situation.noTimeRestrictionDefined, new double[] {0., 0.});
		situations.put(Situation.timeRestrictionStartAndEndDefined, new double[] {28800., 75600.});
		situations.put(Situation.timeRestrictionEndDefined, new double[] {0., 75600.});
		situations.put(Situation.timeRestrictionStartDefined, new double[] {28800., 0.});
		situations.put(Situation.multipleHomeActivities, new double[] {0., 0.});

		for (Situation situation : situations.keySet()) {

			Config config = ConfigUtils.loadConfig(url);
			config.controler().setLastIteration(0);
			config.controler().setOutputDirectory(utils.getOutputDirectory() + "/" + situation.toString());
			config.global().setNumberOfThreads(0);
			config.qsim().setNumberOfThreads(1);
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

			ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);

			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

			Population population = PopulationUtils.createPopulation(config);

			createExamplePopulation(population, scenario, situation);

			scenario.setPopulation(population);

			Controler controler = new Controler(scenario);

			PersonMoneyEventsCounter tracker = new PersonMoneyEventsCounter();

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(new TimeRestrictedParkingCostHandler(situations.get(situation)[0], situations.get(situation)[1]));
					install(new PersonMoneyEventsAnalysisModule());

					addEventHandlerBinding().toInstance(tracker);
				}
			});

			controler.run();

			getAssertions(situation, tracker);
		}



	}

	private void getAssertions(Situation situation, PersonMoneyEventsCounter tracker) {

		switch (situation) {
			case noTimeRestrictionDefined -> {
				//1) no time restriction is set -> parkingCost charged at any time
				Assert.assertEquals("number of tested persons", 1, tracker.notSoRichPersonsAnymore.keySet().size());
				Assert.assertEquals("number of charged activities", 1,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation.toString())).size());
			}

			case timeRestrictionStartAndEndDefined, timeRestrictionEndDefined -> {
				//2) start + end of parking period is set
				//3) start of parking period is not defined, the end is defined
				//	2.1) eventTime inside parking period -> charging of parking cost
				//	3.1) eventTime is earlier or equal to end of period -> charging
				Assert.assertEquals("number of tested persons", 2, tracker.notSoRichPersonsAnymore.keySet().size());
				Assert.assertEquals("number of charged activities for person inside parking time period", 1,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation.toString())).size());
				//	2.2) eventime outside parking period -> no charging
				//	3.2) eventTime is later than end of period -> no charging
				Assert.assertEquals("number of charged activities for person outside parking time period", 0,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation + "_2")).size());
			}

			case timeRestrictionStartDefined -> {
				//4) //start of parking period is defined, the end is not defined
				//	4.1) eventTime is earlier than start of period -> no charging
				Assert.assertEquals("number of tested persons", 2, tracker.notSoRichPersonsAnymore.keySet().size());
				Assert.assertEquals("number of charged activities for person outside parking time period", 0,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation.toString())).size());

				//	4.2) eventTime is equal or later than start of period -> charging
				Assert.assertEquals("number of charged activities for person inside parking time period", 1,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation + "_2")).size());

			}

			case multipleHomeActivities -> {
				Assert.assertEquals("number of tested persons", 2, tracker.notSoRichPersonsAnymore.keySet().size());
				Assert.assertEquals("number of charged activities", 2,
						tracker.notSoRichPersonsAnymore.get(Id.createPersonId(situation + "_2")).size());
			}
		}

	}

	class PersonMoneyEventsCounter implements PersonMoneyEventHandler, ActivityStartEventHandler {

		Map<Id<Person>, List<PersonMoneyEvent>> notSoRichPersonsAnymore = new HashMap<>();

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			if (!notSoRichPersonsAnymore.containsKey(event.getPersonId())) {
				notSoRichPersonsAnymore.put(event.getPersonId(), new ArrayList<>(Arrays.asList(event)));
			} else {
				notSoRichPersonsAnymore.get(event.getPersonId()).add(event);
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			notSoRichPersonsAnymore.putIfAbsent(event.getPersonId(), new ArrayList<>());
		}
	}
}
