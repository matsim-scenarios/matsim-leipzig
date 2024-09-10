/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.analysis.emissions;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.VspHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.matsim.application.ApplicationUtils.globFile;

/**
 * This analysis class requires two parameters as arguments: <br>
 * (1) the run directory, and <br>
 * (2) the password (passed as environment variable in your IDE
 * and/or on the server) to access the encrypted files on the public-svn.
 *
 * @author Ruan J. Gräbe (rgraebe)
*/

public class RunOfflineAirPollutionAnalysisByVehicleCategory {

	private static final Logger log = LogManager.getLogger(RunOfflineAirPollutionAnalysisByVehicleCategory.class);
	private final String runDirectory;
	private final String runId;
	private final String hbefaWarmFile;
	private final String hbefaColdFile;
	private final String analysisOutputDirectory;
	
	public RunOfflineAirPollutionAnalysisByVehicleCategory(String runDirectory, String runId, String hbefaFileWarm, String hbefaFileCold, String analysisOutputDirectory) {
		this.runDirectory = runDirectory;
		this.runId = runId;
		this.hbefaWarmFile = hbefaFileWarm;
		this.hbefaColdFile = hbefaFileCold;
		
		if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
		this.analysisOutputDirectory = analysisOutputDirectory;
	}
	
	public static void main(String[] args) {

		if (args.length == 1) {
			String runDirectory = args[0];
			if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

			final String runId = "leipzig-25pct"; // based on the simulation output available in this project

			String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
			String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";

			RunOfflineAirPollutionAnalysisByVehicleCategory analysis = new RunOfflineAirPollutionAnalysisByVehicleCategory(
					runDirectory,
					runId,
					hbefaFileWarm,
					hbefaFileCold,
					runDirectory + "emission-analysis-offline");
			try {
				analysis.run();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		} else {
			throw new RuntimeException("Please set the run directory path and/or password. \nCheck the class description for more details. Aborting...");
		}
	}

	public void run() throws IOException {

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(String.valueOf(globFile(Path.of(runDirectory), runId, "vehicles")));
		config.network().setInputFile(String.valueOf(globFile(Path.of(runDirectory), runId, "network")));
		config.transit().setTransitScheduleFile(String.valueOf(globFile(Path.of(runDirectory), runId, "transitSchedule")));
		config.transit().setVehiclesFile(String.valueOf(globFile(Path.of(runDirectory), runId, "transitVehicles")));

		config.global().setCoordinateSystem("EPSG:25832");
		log.info("Using coordinate system '{}'", config.global().getCoordinateSystem());
		config.plans().setInputFile(null);
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(1);
		
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setAverageColdEmissionFactorsFile(this.hbefaColdFile);
		eConfig.setAverageWarmEmissionFactorsFile(this.hbefaWarmFile);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);

		// input and outputs of emissions analysis
		final String eventsFile = globFile(Path.of(runDirectory), runId, "output_events");
		File dir = new File(analysisOutputDirectory);
		if ( !dir.exists() ) { dir.mkdir(); }
		final String emissionEventOutputFile = analysisOutputDirectory + runId + ".emission.events.offline.xml.gz";
		log.info("Writing emissions (link totals) to: {}", emissionEventOutputFile);
		// for SimWrapper
		final String linkEmissionPerMOutputFile = analysisOutputDirectory + runId + ".emissionsPerLinkPerM.csv";
		log.info("Writing emissions per link [g/m] to: {}", linkEmissionPerMOutputFile);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// network
		new VspHbefaRoadTypeMapping().addHbefaMappings(scenario.getNetwork());
		log.info("Using integrated road types");

		{ // vehicles
			Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
			Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);
			Id<VehicleType> drtVehicleTypeId = Id.create("conventional_vehicle", VehicleType.class);

			VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
			VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);
			VehicleType drtVehicleType = scenario.getVehicles().getVehicleTypes().get(drtVehicleTypeId);

			EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
			VehicleUtils.setHbefaVehicleCategory(carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
			VehicleUtils.setHbefaTechnology(carEngineInformation, "average");
			VehicleUtils.setHbefaSizeClass(carEngineInformation, "average");
			VehicleUtils.setHbefaEmissionsConcept(carEngineInformation, "average");

			EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
			VehicleUtils.setHbefaVehicleCategory(freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
			VehicleUtils.setHbefaTechnology(freightEngineInformation, "average");
			VehicleUtils.setHbefaSizeClass(freightEngineInformation, "average");
			VehicleUtils.setHbefaEmissionsConcept(freightEngineInformation, "average");

			EngineInformation drtEngineInformation = drtVehicleType.getEngineInformation();
			VehicleUtils.setHbefaVehicleCategory(drtEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
			VehicleUtils.setHbefaTechnology(drtEngineInformation, "average");
			VehicleUtils.setHbefaSizeClass(drtEngineInformation, "average");
			VehicleUtils.setHbefaEmissionsConcept(drtEngineInformation, "average");

			// public transit vehicles should be considered as non-hbefa vehicles
			for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
				EngineInformation engineInformation = type.getEngineInformation();
				// TODO: Check! Is this a zero emission vehicle?!
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
				VehicleUtils.setHbefaTechnology(engineInformation, "average");
				VehicleUtils.setHbefaSizeClass(engineInformation, "average");
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			}

			// ignore bikes
			VehicleType bikeType = scenario.getVehicles().getVehicleTypes().get(Id.create("bike", VehicleType.class));
			EngineInformation bikeEngineInformation = bikeType.getEngineInformation();
			VehicleUtils.setHbefaVehicleCategory(bikeEngineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
		}

        EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		// necessary for link emissions [g/m] output
		EmissionsOnLinkEventHandler emissionsOnLinkEventHandler = new EmissionsOnLinkEventHandler(10.);
		eventsManager.addHandler(emissionsOnLinkEventHandler);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
		log.info("-------------------------------------------------");
		log.info("Done reading the events file");
		log.info("Finish processing...");
		eventsManager.finishProcessing();
		log.info("Closing events file...");
        emissionEventWriter.closeFile();
		log.info("Done");
		log.info("Writing (more) output...");

		{ // writing emissions (per link) per meter
			File file1 = new File(linkEmissionPerMOutputFile);
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));

			bw1.write("linkId");

			for (Pollutant pollutant : Pollutant.values()) {
				bw1.write(";" + pollutant + " [g/m]");
			}
			bw1.newLine();

			Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsOnLinkEventHandler.getLink2pollutants();

			for (Id<Link> linkId : link2pollutants.keySet()) {
				bw1.write(linkId.toString());

				for (Pollutant pollutant : Pollutant.values()) {
					double emission = 0.;
					if (link2pollutants.get(linkId).get(pollutant) != null) {
						emission = link2pollutants.get(linkId).get(pollutant);
					}

					double emissionPerM = Double.NaN;
					Link link = scenario.getNetwork().getLinks().get(linkId);
					if (link != null) {
						emissionPerM = emission / link.getLength();
					}

					bw1.write(";" + emissionPerM);
				}
				bw1.newLine();
			}

			bw1.close();
			log.info("Done");
			log.info("All output written to " + analysisOutputDirectory);
			log.info("-------------------------------------------------");
		}
	}

}

