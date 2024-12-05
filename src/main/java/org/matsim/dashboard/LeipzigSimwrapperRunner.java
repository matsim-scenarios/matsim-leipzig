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

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--cycle-highway-analysis", defaultValue = "DISABLED", description = "create cycle highway dashboard")
	private CycleHighwayAnalysis cycleHighwayAnalysis;
	@CommandLine.Option(names = "--base-dir", description = "dir of base run for cycle highway policy cases.")
	private String baseDir;
	@CommandLine.Option(names = "--highways-shp-path", description = "Path to run directory of base case.", required = true)
	private String highwaysShpPath;

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
//			Config config = ConfigUtils.loadConfig(configPath);

			Config config = ConfigUtils.createConfig();

			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;
			simwrapperCfg.sampleSize = 0.25;
			simwrapperCfg.defaultParams().mapCenter = "12.38,51.34";
			simwrapperCfg.defaultParams().mapZoomLevel = 6.8;

			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			//add dashboards according to command line parameters
			if (cycleHighwayAnalysis == CycleHighwayAnalysis.ENABLED) {
				sw.addDashboard(Dashboard.customize(new CycleHighwayDashboard(baseDir, shp.getShapeFile(), highwaysShpPath)).context("cycle-highway"));
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
				// Check if the file name starts with "dashboard-" and ends with ".yaml"
				if (file.isFile() && file.getName().startsWith("dashboard-") && file.getName().endsWith(".yaml")) {
					// Get the current file name
					String oldName = file.getName();

					// Extract the number from the file name
					String numberPart = oldName.substring(oldName.indexOf('-') + 1, oldName.lastIndexOf('.'));

					try {
						// Increment the number by ten
						int number = Integer.parseInt(numberPart) + 10;

						// Create the new file name and check for conflicts
						File newFile;
						do {
							String newName = "dashboard-" + number + ".yaml";
							newFile = new File(file.getParent(), newName);
							number += 10; // Increment further if a conflict is found
						} while (newFile.exists());

						// Rename the file
						if (file.renameTo(newFile)) {
							log.info("File successfully renamed: {}", newFile.getName());
						} else {
							log.error("Error renaming file: {}", file.getName());
							throw new IllegalArgumentException();
						}
					} catch (NumberFormatException e) {
						log.error("Invalid number format in file name: {}", oldName);
						throw new NumberFormatException("");
					}
				}
			}
		}
	}
}
