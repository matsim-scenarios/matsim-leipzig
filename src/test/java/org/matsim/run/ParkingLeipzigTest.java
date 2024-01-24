package org.matsim.run;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.run.prepare.NetworkOptions;
import org.matsim.simwrapper.SimWrapperConfigGroup;
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
		config.plans().setInputFile("/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.3/input/testParkingPopulation.xml");

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
	public final void runPoint1pctIntegrationTestWithParkingWithCarFreeArea() {
		Path output = Path.of("output-parking-test-withCarFreeArea/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile("/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.3/input/testParkingPopulation.xml");

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct","--drt-area", exampleShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.3" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking", "--car-free-area", "/Users/gregorr/Documents/work/respos/shared-svn/projects/NaMAV/data/shapefiles/leipzig_carfree_area_small/Zonen99_update.shp");

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new ParkingActivityStartEventHandler());
		EventsUtils.readEvents(eventsManager , output +"/" + "/leipzig-1pct.output_events.xml.gz");
		Assert.assertTrue(ParkingActivityStartEventHandler.parkingEvents.size() == 0);
	}

	class ParkingActivityStartEventHandler implements ActivityStartEventHandler {
		static List<ActivityStartEvent> parkingEvents = new ArrayList<>();
		@Override
		public void handleEvent(ActivityStartEvent activityStartEvent) {
			if (activityStartEvent.getActType().equals("parking interaction")) {
				parkingEvents.add(activityStartEvent);
			}
		}
	}

}

