package org.matsim.run;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class RunLeipzigIntegrationTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";
	@CommandLine.Mixin
	private ShpOptions shp;
	private static final String exampleShp = "input/v1.2/drtServiceArea/preliminary-serviceArea-leipzig-utm32n.shp";


	@Test
	public void runPoint1pct() {

		Path output = Path.of("output/it-1pct");

		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-25pct.config.xml");

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);

		// Change input paths
		config.plans().setInputFile(URL + "leipzig-v1.1-0.1pct.plans.xml.gz");

		Controler controler = RunLeipzigScenario.prepare(RunLeipzigScenario.class, config,
				"--drt-area", exampleShp, "--drt-modes", "drtNorth,drtSoutheast"
		);

		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		controler.run();

		assertThat(output)
				.exists()
				.isNotEmptyDirectory();
	}

	@Test
	public void runSlowSpeedExampleTest() {

		Path output = Path.of("output/it-1pct-slowSpeed");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-25pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		// Change input paths
		config.plans().setInputFile(URL + "leipzig-v1.1-0.1pct.plans.xml.gz");
		Controler controler = RunLeipzigScenario.prepare(RunLeipzigScenario.class, config,
				"--slow-speed-area", exampleShp, "--slow-speed-relative-change", "0.5", "--drt-area", exampleShp,
				"--drt-modes", "drtNorth,drtSoutheast", "--output=" + output + "withSlowSpeed"
		);

		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		controler.run();
		assertThat(output)
				.exists()
				.isNotEmptyDirectory();
		Network network = NetworkUtils.readNetwork(output + "/" + config.controler().getRunId() + ".output_network.xml.gz");
		assertTrue(network.getLinks().get(Id.createLinkId("24232899")).getFreespeed() < 12.501000000000001);
		assertTrue(network.getLinks().get(Id.createLinkId("24675139")).getFreespeed() < 7.497);
	}

	@Test
	public final void runDrtExamplePopulationTest() {
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

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--drt-area", exampleShp,
				"--drt-modes", "drtNorth,drtSoutheast", "--post-processing", "disabled");
	}

	@Test
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

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--drt-area", exampleShp,
				"--drt-modes", "drtNorth,drtSoutheast", "--waiting-time-threshold-optDrt", "600", "--post-processing", "disabled");

		Assert.assertNotNull(config.getModules().get("multiModeOptDrt"));
		Assert.assertNotNull(config.getModules().get("multiModeOptDrt").getParameterSets());
	}
}
