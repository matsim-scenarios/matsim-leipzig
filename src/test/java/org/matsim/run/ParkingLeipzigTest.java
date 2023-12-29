package org.matsim.run;

import org.junit.Test;
import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class ParkingLeipzigTest {

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/";
	private static final String exampleShp = "input/v1.2/drtServiceArea/Leipzig_stadt.shp";

	@Test
	public final void runPoint1pctIntegrationTest() {
		Path output = Path.of("output-parking-test/it-1pct");
		Config config = ConfigUtils.loadConfig("input/v1.2/leipzig-v1.2-25pct.config.xml");
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output.toString());
		ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class).defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
		config.plans().setInputFile(URL + "leipzig-v1.2-0.1pct.plans-initial.xml.gz");

		MATSimApplication.execute(RunLeipzigScenario.class, config, "run", "--1pct","--drt-area", exampleShp, "--post-processing", "disabled",
				"--parking-cost-area", "input/v" + "1.2" + "/parkingCostArea/Bewohnerparken_2020.shp",
				"--intermodality", "drtAsAccessEgressForPt", "--parking" );

		assertThat(output)
				.exists()
				.isNotEmptyDirectory();
		new ParkingLocation().execute("--directory", output.toString());
	}
}
