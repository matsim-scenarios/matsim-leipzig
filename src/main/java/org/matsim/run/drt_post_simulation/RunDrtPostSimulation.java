package org.matsim.run.drt_post_simulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.run.prepare.PrepareNetwork;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.nio.file.Path;

@CommandLine.Command(header = ":: Run DRT post-simulation ::", version = RunDrtPostSimulation.VERSION)
public class RunDrtPostSimulation extends MATSimApplication {
	@CommandLine.Option(names = "--drt-area", description = "Path to SHP file specifying where DRT mode is allowed")
	private Path drtArea;

	static final String VERSION = "1.0";

	public static void main(String[] args) {
		MATSimApplication.run(RunDrtPostSimulation.class, args);
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());
		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		scenario.getPopulation()
			.getFactory()
			.getRouteFactories()
			.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		// Prepare network by adding DRT mode to service area
		PrepareNetwork.prepareDRT(scenario.getNetwork(), new ShpOptions(drtArea, null, null));

	}

	@Override
	protected void prepareControler(Controler controler) {
		Config config = controler.getConfig();
		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));
	}
}
