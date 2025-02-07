package org.matsim.run;


import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.application.MATSimApplication;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParkingLeipzigTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/";
	private static final String exampleShp = String.format("input/v%s/testInput/Zonen99_update.shp", LeipzigScenario.VERSION);
	private static final List<String> agents = new ArrayList<>(List.of("residentLeisureInOA", "outsiderLeisureInOA", "parkingAgentCarFreeLeisureCloseToResParkingZone"));
	private static final String flexaShp = String.format("input/v%s/drtServiceArea/leipzig_flexa_service_area_2021.shp", LeipzigScenario.VERSION);


	@Test
	public final void runPoint1pctIntegrationTestWithParking() {
		Path output = Path.of("output-parking-test/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.3/input/test/testParkingPopulation.xml");

		MATSimApplication.execute(LeipzigScenario.class, config, "run", "--1pct","--drt-area", flexaShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.3" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking");

		//new ParkingLocation().execute("--directory", output.toString());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new ParkingActivityStartEventHandler());
		EventsUtils.readEvents(eventsManager , output +"/" + "/leipzig-1pct.output_events.xml.gz");
		assertThat(ParkingActivityStartEventHandler.parkingEvents)
			.hasSizeGreaterThan(0);

		for (ActivityStartEvent event: ParkingActivityStartEventHandler.parkingEvents) {
			if (event.getPersonId().toString().equals("221462")) {
                assertEquals("-152600315#3", event.getLinkId().toString());
			}
		}
	}

	@Test
	public final void runPoint1pctIntegrationTestWithParkingWithCarFreeArea() throws IOException {
		Path output = Path.of("output-parking-test-withCarFreeArea/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.3/input/test/testParkingPopulation.xml");

		MATSimApplication.execute(LeipzigScenario.class, config, "run", "--1pct","--drt-area", flexaShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.3" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking", "--car-free-area", exampleShp);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new ParkingActivityStartEventHandler());
		EventsUtils.readEvents(eventsManager , output +"/" + "/leipzig-1pct.output_events.xml.gz");


		for (ActivityStartEvent event: ParkingActivityStartEventHandler.parkingEvents) {
			if (event.getAttributes().get("person").equals("residentLeisureInOA")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("3888.0")){
                    assertEquals("206552443", link);
				}

				//Assert.assertTrue(event.getLinkId().equals("11827009#2"));
			}
			if (event.getAttributes().get("person").equals("outsiderLeisureInOA")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("4113.0")){
                    assertEquals("-56064787#1", link);
				}
			}
			if (event.getAttributes().get("person").equals("parkingAgentCarFreeLeisureCloseToResParkingZone")) {
				var link = event.getAttributes().get("link");
				var time = event.getAttributes().get("time");
				if(time.equals("3889.0")){
                    assertEquals("11827009#2", link);
				}

			}
		}

//		writer.close();

	}

	static final class ParkingActivityStartEventHandler implements ActivityStartEventHandler {
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

