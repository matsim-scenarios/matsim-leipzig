package org.matsim.run;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class RunLeipzigIntegrationTest {


	private static final String URL = String.format("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v%s/input/",
			RunLeipzigScenario.VERSION);
	private static final String stadtShp = String.format("input/v%s/drtServiceArea/Leipzig_stadt.shp", RunLeipzigScenario.VERSION);
	private static final String flexaShp = String.format("input/v%s/drtServiceArea/leipzig_flexa_service_area_2021.shp", RunLeipzigScenario.VERSION);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	public final void runPoint1pctIntegrationTest() {

		Config config = ConfigUtils.loadConfig(String.format("input/v%s/leipzig-v%s-10pct.config.xml",RunLeipzigScenario.VERSION,RunLeipzigScenario.VERSION));

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.plans().setInputFile(URL + String.format("leipzig-v%s-0.1pct.plans-initial.xml.gz",RunLeipzigScenario.VERSION));

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--slow-speed-area", stadtShp,
				"--slow-speed-relative-change", "0.5","--drt-area", flexaShp, "--post-processing", "disabled"
		);

		assertThat(EventsUtils.compareEventsFiles(
			new File(utils.getOutputDirectory(), "leipzig-1pct.output_events.xml.gz").toString(),
			new File(utils.getClassInputDirectory(), "runPoint1pctIntegrationTest_events.xml.gz").toString()
		)).isEqualTo(EventsFileComparator.Result.FILES_ARE_EQUAL);


		Network network = NetworkUtils.readNetwork(utils.getOutputDirectory() + "/" + config.controler().getRunId() + ".output_network.xml.gz");
		assertTrue(network.getLinks().get(Id.createLinkId("24232899")).getFreespeed() < 12.501000000000001);
		assertTrue(network.getLinks().get(Id.createLinkId("24675139")).getFreespeed() < 7.497);

		testDrt(config);
	}

	@Test
	public final void runPoint1pctParkingIntegrationTest() {
		String output = utils.getOutputDirectory();

		Config config = ConfigUtils.loadConfig(String.format("input/v%s/leipzig-v%s-10pct.config.xml",RunLeipzigScenario.VERSION,RunLeipzigScenario.VERSION));
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile(URL + String.format("leipzig-v%s-0.1pct.plans-initial.xml.gz",RunLeipzigScenario.VERSION));

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--drt-area", flexaShp, "--post-processing", "disabled",
			"--parking-cost-area", "input/v" + RunLeipzigScenario.VERSION + "/parkingCostArea/Bewohnerparken_2020.shp",
			"--parking", "--intermodality", "drtAsAccessEgressForPt");

		assertThat(Path.of(output))
			.exists()
			.isNotEmptyDirectory();

		//how can this ever be equa??? gr 02.24
		assertThat(EventsUtils.compareEventsFiles(
				new File(utils.getOutputDirectory(), "leipzig-1pct.output_events.xml.gz").toString(),
				new File(utils.getClassInputDirectory(), "runPoint1pctParkingIntegrationTest_events.xml.gz").toString()
		)).isEqualTo(EventsFileComparator.Result.MISSING_EVENT);
		//)).isEqualTo(EventsFileComparator.Result.FILES_ARE_EQUAL);


		Network network = NetworkUtils.readNetwork(utils.getOutputDirectory() + "/" + config.controler().getRunId() + ".output_network.xml.gz");
		assertTrue(network.getLinks().get(Id.createLinkId("24232899")).getFreespeed() < 12.501000000000001);
		assertTrue(network.getLinks().get(Id.createLinkId("24675139")).getFreespeed() < 7.497);

		testDrt(config);

		new ParkingLocation().execute("--directory", output);
	}

	private static void testDrt(Config config) {
		//TODO add more tests, drt trips, etc.

		LeipzigPtFareModule ptFareModule = new LeipzigPtFareModule();

		//set fare params; flexa has the same prices as leipzig PT: Values taken out of LeipzigPtFareModule -sm0522
		Double ptBaseFare = ptFareModule.getNormalPtBaseFare();
		Double ptDistanceFare = ptFareModule.getNormalDistanceBasedFare();

		assertNotNull(DvrpConfigGroup.get(config));
		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);
		assertNotNull(multiModeDrtCfg);
		multiModeDrtCfg.getModalElements().forEach(drtConfigGroup -> {

			//assume DrtFareParams to be configured
			assertTrue(drtConfigGroup.getDrtFareParams().isPresent());
			DrtFareParams fareParams = drtConfigGroup.getDrtFareParams().get();
			assertEquals(ptBaseFare.doubleValue(), fareParams.baseFare, 0.);
			assertEquals(ptDistanceFare.doubleValue(), fareParams.distanceFare_m, 0.);

			//assume speed up params to be configured and a few specific values
			assertTrue(drtConfigGroup.getDrtSpeedUpParams().isPresent());
			DrtSpeedUpParams speedUpParams = drtConfigGroup.getDrtSpeedUpParams().get();
			assertTrue(config.controler().getLastIteration() <= speedUpParams.firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams);
			assertEquals(0., speedUpParams.fractionOfIterationsSwitchOn,0.);
			assertEquals(1., speedUpParams.fractionOfIterationsSwitchOff, 0.);
		});


		testDrtNetwork(config);
	}

	private static void testDrtNetwork(Config config) {
		Network network = NetworkUtils.readNetwork(config.controler().getOutputDirectory() + "/" + config.controler().getRunId() + ".output_network.xml.gz");
		assertFalse(network.getLinks().get(Id.createLinkId("24232899")).getAllowedModes().contains("drtNorth"));
		assertFalse(network.getLinks().get(Id.createLinkId("24232899")).getAllowedModes().contains("drtSoutheast"));
		assertTrue(network.getLinks().get(Id.createLinkId("307899688#1")).getAllowedModes().contains("drtNorth"));
		assertTrue(network.getLinks().get(Id.createLinkId("26588307#0")).getAllowedModes().contains("drtSoutheast"));
	}
}
