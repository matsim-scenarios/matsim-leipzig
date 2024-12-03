package org.matsim.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.List;

public class LeipzigSimwrapperRunner implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(LeipzigSimwrapperRunner.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which dashboards are to be generated.")
	private List<Path> inputPaths;

//	@CommandLine.Mixin
//	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--cycle-highway-analysis", defaultValue = "DISABLED", description = "create cycle highway dashboard")
	private CycleHighwayAnalysis cycleHighwayAnalysis;

	enum CycleHighwayAnalysis {ENABLED, DISABLED}

	public static void main(String[] args) {
		new LeipzigSimwrapperRunner().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (cycleHighwayAnalysis != CycleHighwayAnalysis.ENABLED){
			throw new IllegalArgumentException("you have not configured any dashboard to be created! Please use command line parameters!");
		}

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			renameExistingDashboardYAMLs(runDirectory);

			String configPath = ApplicationUtils.matchInput("config.xml", runDirectory).toString();
			Config config = ConfigUtils.loadConfig(configPath);

			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			//add dashboards according to command line parameters
			if (cycleHighwayAnalysis == CycleHighwayAnalysis.ENABLED) {
				sw.addDashboard(Dashboard.customize(new CycleHighwayDashboard()).context("cycle-highway"));
			}

			try {
				sw.generate(runDirectory);
				sw.run(runDirectory);
			} catch (IOException e) {
				throw new InterruptedIOException();
			}
		}

		return 0;
	}

	/**
	 * workaround method to rename existing dashboards to avoid overriding.
	 */
	private static void renameExistingDashboardYAMLs(Path runDirectory) {
		// List of files in the folder
		File folder = new File(runDirectory.toString());
		File[] files = folder.listFiles();

		// Loop through all files in the folder
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					// Check if the file name starts with "dashboard-" and ends with ".yaml"
					if (file.getName().startsWith("dashboard-") && file.getName().endsWith(".yaml")) {
						// Get the current file name
						String oldName = file.getName();

						// Extract the number from the file name
						String numberPart = oldName.substring(oldName.indexOf('-') + 1, oldName.lastIndexOf('.'));

						// Increment the number by ten
						int number = Integer.parseInt(numberPart) + 10;

						// Create the new file name
						String newName = "dashboard-" + number + ".yaml";

						// Create the new File object with the new file name
						File newFile = new File(file.getParent(), newName);

						// Rename the file
						if (file.renameTo(newFile)) {
							log.info("File successfully renamed: " + newName);
						} else {
							log.info("Error renaming file: " + file.getName());
						}
					}
				}
			}
		}
	}
}
