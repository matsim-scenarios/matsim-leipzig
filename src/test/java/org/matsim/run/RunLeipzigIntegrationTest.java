package org.matsim.run;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class RunLeipzigIntegrationTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";
	@CommandLine.Mixin
	private ShpOptions shp;


	@Test
	public void runPoint1pct() {

		Path output = Path.of("output/it-1pct");

		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-25pct.config.xml");

		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);

		// Change input paths
		config.plans().setInputFile(URL + "leipzig-v1.1-0.1pct.plans.xml.gz");

		Controler controler = RunLeipzigScenario.prepare(RunLeipzigScenario.class, config,
				"--with-drt"
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
	public void testSlowSpeed() {

		Path output = Path.of("outputSlowSpeed/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-25pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		// Change input paths
		config.plans().setInputFile(URL + "leipzig-v1.1-0.1pct.plans.xml.gz");
		Controler controler = RunLeipzigScenario.prepare(RunLeipzigScenario.class, config,
				"--tempo30Zone","--shp", "input/v1.2/drtServiceArea/preliminary-serviceArea-leipzig-utm32n.shp", "--with-drt", "--output=" + output + "withSlowSped"
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

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--with-drt", "--post-processing", "disabled");
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

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct", "--with-drt", "--waiting-time-threshold-optDrt", "600", "--post-processing", "disabled");

		Assert.assertNotNull(config.getModules().get("multiModeOptDrt"));
		Assert.assertNotNull(config.getModules().get("multiModeOptDrt").getParameterSets());
	}
}
