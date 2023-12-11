package org.matsim.run;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class RunLeipzigIntegrationTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/";
	private static final String exampleShp = "input/v1.3/drtServiceArea/Leipzig_stadt.shp";

	@Test
	public final void runPoint1pctIntegrationTest() {
		Path output = Path.of("output/it-1pct");

		Config config = ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3-10pct.config.xml");

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		config.plans().setInputFile(URL + "leipzig-v1.2-0.1pct.plans-initial.xml.gz");

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--slow-speed-area", exampleShp,
				"--slow-speed-relative-change", "0.5","--drt-area", exampleShp, "--post-processing", "disabled"
		);

		assertThat(output)
				.exists()
				.isNotEmptyDirectory();

		Network network = NetworkUtils.readNetwork(output + "/" + config.controler().getRunId() + ".output_network.xml.gz");
		assertTrue(network.getLinks().get(Id.createLinkId("24232899")).getFreespeed() < 12.501000000000001);
		assertTrue(network.getLinks().get(Id.createLinkId("24675139")).getFreespeed() < 7.497);
	}

	@Test
	public final void runPoint1pctParkingIntegrationTest() {
		Path output = Path.of("output-parking-test/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.3/leipzig-v1.3-10pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile(URL + "leipzig-v1.2-0.1pct.plans-initial.xml.gz");

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--drt-area", exampleShp, "--post-processing", "disabled",
			"--parking-cost-area", "input/v" + RunLeipzigScenario.VERSION + "/parkingCostArea/Bewohnerparken_2020.shp",
			"--parking", "--intermodality", "drtAsAccessEgressForPt");

		assertThat(output)
			.exists()
			.isNotEmptyDirectory();
		new ParkingLocation().execute("--directory", output.toString());
	}

	@Test
	@Ignore("Opt Drt is not used right now.")
	public final void runOptDrtExamplePopulationTest() {
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-test.with-drt.config.xml");

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.network().setInputFile(URL + "drt-base-case/leipzig-v1.1-network-with-pt-drt.xml.gz");
		config.plans().setInputFile(URL + "leipzig-v1.1-0.1pct.plans.xml.gz");
		config.transit().setTransitScheduleFile(URL + "leipzig-v1.1-transitSchedule.xml.gz");
		config.transit().setVehiclesFile(URL + "leipzig-v1.1-transitVehicles.xml.gz");
		config.vehicles().setVehiclesFile(URL + "drt-base-case/leipzig-v1.1-vehicle-types-with-drt-scaledFleet.xml");

		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--drt-area", exampleShp,
				"--drt-modes", "drtNorth,drtSoutheast", "--waiting-time-threshold-optDrt", "600", "--post-processing", "disabled");

		Assert.assertNotNull(config.getModules().get("multiModeOptDrt"));
		Assert.assertNotNull(config.getModules().get("multiModeOptDrt").getParameterSets());
	}
}
