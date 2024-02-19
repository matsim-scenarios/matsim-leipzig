package org.matsim.run;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimApplication;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.run.prepare.NetworkOptions;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class ParkingLeipzigTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/";
	private static final String exampleShp = "input/v1.2/drtServiceArea/Leipzig_stadt.shp";
	private static final List<String> agents = new ArrayList<>(List.of("residentLeisureInOA", "outsiderLeisureInOA", "parkingAgentCarFreeLeisureCloseToResParkingZone"));

	@Test
	public final void runPoint1pctIntegrationTestWithParking() {
		Path output = Path.of("output-parking-test/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile("testParkingPopulation.xml");

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct","--drt-area", exampleShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.3" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking");

		//new ParkingLocation().execute("--directory", output.toString());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new ParkingActivityStartEventHandler());
		EventsUtils.readEvents(eventsManager , output +"/" + "/leipzig-1pct.output_events.xml.gz");
		Assert.assertTrue(ParkingActivityStartEventHandler.parkingEvents.size() >0);
		for (ActivityStartEvent event: ParkingActivityStartEventHandler.parkingEvents) {
			if (event.getPersonId().equals("221462")) {
				Assert.assertTrue(event.getLinkId().equals("-152600315#3"));
			}
		}
	}

	@Test
	public final void runPoint1pctIntegrationTestWithParkingWithCarFreeArea() throws IOException {
		Path output = Path.of("output-parking-test-withCarFreeArea/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		//TODO maybe write the test population
		// for the test case combination look at the ChessboardParkingTest
		config.plans().setInputFile("testParkingPopulation.xml");

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct","--drt-area", exampleShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.3" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking", "--car-free-area", "input/v1.2/carfree/leipzig_carfree_area_small/Zonen99_update.shp");

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new ParkingActivityStartEventHandler());
		EventsUtils.readEvents(eventsManager , output +"/" + "/leipzig-1pct.output_events.xml.gz");


		for (ActivityStartEvent event: ParkingActivityStartEventHandler.parkingEvents) {
			if (event.getAttributes().get("person").equals("residentLeisureInOA")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("3888.0")){
					Assert.assertTrue(link.equals("206552443"));
				}

				//Assert.assertTrue(event.getLinkId().equals("11827009#2"));
			}
			if (event.getAttributes().get("person").equals("outsiderLeisureInOA")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("4113.0")){
					Assert.assertTrue(link.equals("-56064787#1"));
				}
			}
			if (event.getAttributes().get("person").equals("parkingAgentCarFreeLeisureCloseToResParkingZone")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("3889.0")){
					Assert.assertTrue(link.equals("11827009#2"));
				}

			}
		}

//		writer.close();

	}

	class ParkingActivityStartEventHandler implements ActivityStartEventHandler {
		static List<ActivityStartEvent> parkingEvents = new ArrayList<>();
		@Override
		public void handleEvent(ActivityStartEvent event) {
			if(agents.contains(event.getPersonId().toString())){
				if(event.getActType().equals("car interaction") || event.getActType().equals("parking interaction"))
					parkingEvents.add(event);
			}
		}
	}

}

