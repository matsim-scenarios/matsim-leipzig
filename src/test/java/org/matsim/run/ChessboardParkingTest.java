package org.matsim.run;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test class to test a parking logic, which included several areas where only residents are allowed to park
 */
public class ChessboardParkingTest {
	private static final Logger log = LogManager.getLogger(ChessboardParkingTest.class);
	//	private static final String HOME_ZONE_ID = "homeLinkId";
	final String RE_ROUTE_LEIPZIG = "ReRouteLeipzig";
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	static void createExampleParkingPopulation(Population population, Network network, Situation situation) {

		PopulationFactory factory = population.getFactory();
		Leg carLeg = factory.createLeg(TransportMode.car);
		Link originLink = network.getLinks().get(Id.createLinkId("80"));
		Link destinationLink = network.getLinks().get(Id.createLinkId("81"));

		Person person = factory.createPerson(Id.createPersonId(situation.toString()));
		Plan plan = factory.createPlan();

		switch (situation) {
			//1.1
			case defaultLogicLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(originLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//1.2
			case defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkInResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(destinationLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//1.3
			case defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.work.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//2.1
			case parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(originLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//2.2
			case parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(originLink);
				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(destinationLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//3.1
			case defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.work.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(destinationLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//3.2
			case defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.home.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(originLink);
				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(destinationLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}
			//4.1
			case parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				Activity originActivity = factory.createActivityFromLinkId(SnzActivities.leisure.name(), originLink.getId());
				Activity destinationActivity = factory.createActivityFromLinkId(SnzActivities.work.name(), destinationLink.getId());
				originActivity.setEndTime(3600.);

				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(originLink);
				LeipzigUtils.setLinkParkingTypeToInsideResidentialArea(destinationLink);

				plan.addActivity(originActivity);
				plan.addLeg(carLeg);
				plan.addActivity(destinationActivity);
			}

			default -> throw new IllegalStateException("Unexpected value: " + situation);
		}

		person.addPlan(plan);
		population.addPerson(person);
	}

	@Test
	public final void runChessboardParkingTest1() {

		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml");

		for (Situation situation : Situation.values()) {
			Config config = ConfigUtils.loadConfig(url);
			config.controller().setLastIteration(1);
			config.controller().setOutputDirectory(utils.getOutputDirectory() + "/" + situation.toString());
			config.global().setNumberOfThreads(0);
			config.qsim().setNumberOfThreads(1);
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

			config.replanning().clearStrategySettings();
			{
				ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
				stratSets.setWeight(1.);
				stratSets.setStrategyName(RE_ROUTE_LEIPZIG);
				config.replanning().addStrategySettings(stratSets);
			}

			config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile);

			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(TripStructureUtils.createStageActivityType("parking")).setScoringThisActivityAtAll(false));

			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

			Population population = PopulationUtils.createPopulation(config);
			createExampleParkingPopulation(population, scenario.getNetwork(), situation);
			scenario.setPopulation(population);
			log.warn("population size=" + scenario.getPopulation().getPersons().size() + " for case" + situation);

//		System.exit(-1);
			//why ist the above here? Doesn't it end the test / sim so it is of no use here?! -sme0523


			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
//				this.bind( MultimodalLinkChooser.class ).toInstance( new LeipzigMultimodalLinkChooser() );
					this.addPlanStrategyBinding(RE_ROUTE_LEIPZIG).toProvider(LeipzigRoutingStrategyProvider.class);
					// yyyy this only uses it during replanning!!!  kai, apr'23
				}
			});

			TestParkingListener testParkingListener = new TestParkingListener();
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(testParkingListener);
				}
			});
			controler.run();

			getAssertions(situation, testParkingListener);
		}


	}


//	static final class LeipzigMultimodalLinkChooser implements MultimodalLinkChooser {
//
//		private final MultimodalLinkChooser delegate;
//		@Inject LeipzigMultimodalLinkChooser() {
//			this.delegate = RouterUtils.getMultimodalLinkChooserDefault();
//		}
//
//		@Override public Link decideOnLink( Facility facility, Network network, RoutingRequest routingRequest ){
//			Link result = null ;
//
////			Person person = routingRequest.getPerson();
////
////			String homeZoneId = (String) person.getAttributes().getAttribute( HOME_ZONE_ID );
////
////			if ( homeLinkId.equals( facility.getLinkId().toString() ) ) {
////
////			 fall back on default:
////			if ( result == null ){
////				result = this.delegate.decideOnLink( facility, network, routingRequest );
////			}
//			return result;
//		}
//	}

	private void getAssertions(Situation situation, TestParkingListener listener) {

		switch (situation) {

			//1.1
			case defaultLogicLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				assertFalse(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea))));
			}
			//1.2
			case defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkInResidentialArea -> {
				assertFalse(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkInResidentialArea))));
			}
			//1.3
			case defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				assertFalse(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkOutsideResidentialArea))));
			}
			//2.1
			case parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea -> {
				assertTrue(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf
					(Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea))));
				assertEquals(1, listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea))).size(), "wrong number of parking activites!");
				assertEquals(Id.createLinkId("169"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea))).get(0).getLinkId(), "wrong origin parking link");
			}
			//2.2
			case parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea -> {
				assertTrue(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf
					(Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea))));
				assertEquals(1, listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea))).size(), "wrong number of parking activites!");
				assertEquals(Id.createLinkId("169"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea))).get(0).getLinkId(), "wrong origin parking link");
			}
			//3.1
			case defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				assertTrue(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf
					(Situation.defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))));
				assertEquals(1, listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).size(), "wrong number of parking activites!");
				assertEquals(Id.createLinkId("133"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).get(0).getLinkId(), "wrong destination parking link");
			}
			//3.2
			case defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				assertTrue(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf
					(Situation.defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))));
				assertEquals(1, listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).size(), "wrong number of parking activites!");
				assertEquals(Id.createLinkId("133"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).get(0).getLinkId(), "wrong destination parking link");
			}
			//4.1
			case parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea -> {
				assertTrue(listener.parkingActivities.containsKey(Id.createPersonId(String.valueOf
					(Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))));
				assertEquals(2, listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).size(), "wrong number of parking activites!");
				assertEquals(Id.createLinkId("169"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).get(0).getLinkId(), "wrong origin parking link");
				assertEquals(Id.createLinkId("133"), listener.parkingActivities.get(Id.createPersonId(String.valueOf(
					Situation.parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea))).get(1).getLinkId(), "wrong destination parking link");
			}
		}
	}

	enum Situation {
		//1) defaultLogic to defaultLogic
		//1.1) defaultLogic linkInResidentialArea (=home act) to defaultLogic linkOutsideResidentialArea
		defaultLogicLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea,
		//1.2) defaultLogic linkOutsideResidentialArea to defaultLogic linkInResidentialArea (=home act)
		defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkInResidentialArea,
		//1.3) defaultLogic linkOutsideResidentialArea to defaultLogic linkOutsideResidentialArea
		defaultLogicLinkOutsideResidentialAreaToDefaultLogicLinkOutsideResidentialArea,

		//2) parkingSearchLogicLeipzig to defaultLogic
		//2.1) parkingSearchLogicLeipzig linkInResidentialArea (!=home act) to defaultLogic linkOutsideResidentialArea
		parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkOutsideResidentialArea,
		//2.2) parkingSearchLogicLeipzig linkInResidentialArea (!=home act) to defaultLogic linkInResidentialArea (=home act)
		parkingSearchLogicLeipzigLinkInResidentialAreaToDefaultLogicLinkInResidentialArea,

		//3) defaultLogic to parkingSearchLogicLeipzig
		//3.1) defaultLogic linkOutsideResidentialArea to parkingSearchLogicLeipzig linkInResidentialArea (!=home act)
		defaultLogicLinkOutsideResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea,
		//3.2) defaultLogic linkInResidentialArea (=home act) to parkingSearchLogicLeipzig linkInResidentialArea (!=home act)
		defaultLogicLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea,

		//4) parkingSearchLogicLeipzig to parkingSearchLogicLeipzig
		//4.1) parkingSearchLogicLeipzig linkInResidentialArea (!=home act) to parkingSearchLogicLeipzig linkInResidentialArea (!=home act)
		parkingSearchLogicLeipzigLinkInResidentialAreaToParkingSearchLogicLeipzigLinkInResidentialArea
	}

	class TestParkingListener implements ActivityStartEventHandler {

		HashMap<Id<Person>, List<ActivityStartEvent>> parkingActivities = new HashMap<>();

		@Override
		public void handleEvent(ActivityStartEvent activityStartEvent) {
			if (activityStartEvent.getActType().equals("parking interaction")) {
				if (!parkingActivities.containsKey(activityStartEvent.getPersonId())) {
					parkingActivities.put(activityStartEvent.getPersonId(), new ArrayList<>(Arrays.asList(activityStartEvent)));
				} else parkingActivities.get(activityStartEvent.getPersonId()).add(activityStartEvent);
			}
		}

		@Override
		public void reset(int iteration) {
			ActivityStartEventHandler.super.reset(iteration);
		}
	}
}
