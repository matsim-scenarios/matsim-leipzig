package org.matsim.run.prepare;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.impl.EuclideanDistanceBasedDrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.run.LeipzigPtFareModule;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class configures drt config, stops, transitStopsFile and vehicles regarding enum DrtCase of RunLeipzigScenario.
 */
public final class DrtCaseSetup {

	private static final Logger log = LogManager.getLogger(DrtCaseSetup.class);

	//this is not nice because the static set is only filled in prepareConfig
	static Set<String> drtModes = new HashSet<>();

	/**
	 * Defines if intermodality between drt and pt is modelled or not.
	 */
	public enum PtDrtIntermodality {drtAndPtSeparateFromEachOther, drtAsAccessEgressForPt}

	private DrtCaseSetup(){}

	/**
	 * prepare config for drt simulation.
	 */
	public static void prepareConfig(Config config, ShpOptions drtAreas, PtDrtIntermodality ptDrtIntermodality) {

		log.info("reading {}", drtAreas.getShapeFile().toString());
		for (SimpleFeature feature : drtAreas.readFeatures()) {

			String drtMode = String.valueOf(feature.getAttribute("mode"));
			if (drtMode.equals("null")) {
				throw new IllegalArgumentException(String.format("could not find 'mode' attribute in the given shape file at %s", drtAreas.getShapeFile().toString()));
			} else {
				drtModes.add(drtMode);
			}
		}

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		DvrpConfigGroup dvrpConfigGroup = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		LeipzigPtFareModule ptFareModule = new LeipzigPtFareModule();

		//set fare params; flexa has the same prices as leipzig PT: Values taken out of LeipzigPtFareModule -sm0522
		Double ptBaseFare = ptFareModule.getNormalPtBaseFare();
		Double ptDistanceFare = ptFareModule.getNormalDistanceBasedFare();

		DrtFareParams drtFareParams = new DrtFareParams();
		drtFareParams.baseFare = ptBaseFare;
		drtFareParams.distanceFare_m = ptDistanceFare;
		drtFareParams.timeFare_h = 0.;
		drtFareParams.dailySubscriptionFee = 0.;

		if (ptDrtIntermodality.equals(PtDrtIntermodality.drtAsAccessEgressForPt)) {
			configureDrtFareCompensation(config, drtModes, ptBaseFare);
		}

		if (multiModeDrtConfigGroup.getModalElements().isEmpty()) {
			for (String mode : drtModes){
				createDrtModeConfigGroup(multiModeDrtConfigGroup, mode);
			}
		}

		multiModeDrtConfigGroup.getModalElements().forEach(drtConfigGroup -> {
			drtConfigGroup.addParameterSet(drtFareParams);
			DrtConfigs.adjustDrtConfig(drtConfigGroup, config.scoring(), config.routing());

			configureNecessaryConfigGroups(config, drtConfigGroup.getMode());

			// Thd DRT speed-up param will no longer be used
//			DrtSpeedUpParams drtSpeedUpParams = new DrtSpeedUpParams();
//			drtSpeedUpParams.fractionOfIterationsSwitchOn = 0.0;
//			drtSpeedUpParams.fractionOfIterationsSwitchOff = 1.0;
//			drtSpeedUpParams.firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams = 10000;
//			// update to real world data
//			drtSpeedUpParams.initialWaitingTime = 345.6;
//			// update to real world data
//			drtSpeedUpParams.initialInVehicleBeelineSpeed = 20/3.6;
//			drtSpeedUpParams.intervalDetailedIteration = 10000;
//			drtSpeedUpParams.waitingTimeUpdateDuringSpeedUp = DrtSpeedUpParams.WaitingTimeUpdateDuringSpeedUp.Disabled;
//			drtConfigGroup.addParameterSet(drtSpeedUpParams);

			// use estimate and teleport model
			drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;
		});

		//drt modes have to be set as network modes in dvrp CfgGroup
		dvrpConfigGroup.networkModes = drtModes;
		//after adding mode specific multiModeDrtParams -> adjust
	}

	/**
	 * prepare scenario for drt simulation. more specifically, create input vehicles and stops files.
	 * this method does not (!?) change the network. For this, please refer to NetworkOptions.prepareDRT
	 */
	public static void prepareScenario(Scenario scenario, ShpOptions drtAreas, String version, PtDrtIntermodality ptDrtIntermodality) {

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		// (matsim core does not know about DRT routes. This makes it possible to read them before the controler is there.)

		CreateDrtStopsFromNetwork drtStopsCreator = new CreateDrtStopsFromNetwork();
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), MultiModeDrtConfigGroup.class);

		log.info("reading {}", drtAreas.getShapeFile());
		for (SimpleFeature feature : drtAreas.readFeatures()) {
			String drtMode = String.valueOf(feature.getAttribute("mode"));
			if (drtMode.equals("null")) {
				throw new IllegalArgumentException(String.format("could not find 'mode' attribute in the given shape file at %s", drtAreas.getShapeFile().toString()));
			}
			Integer noVehicles = (Integer) feature.getAttribute("noVehicles");
			if (noVehicles == null){
				throw new IllegalArgumentException(String.format("could not find 'noVehicles' attribute in the given shape file at %s", drtAreas.getShapeFile().toString()));
			}

			log.info("filtering network for mode {}. Before, the number of links equals {}.", drtMode, scenario.getNetwork().getLinks().size());
			Network filteredNetwork = NetworkUtils.createNetwork();
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
			filter.filter(filteredNetwork, Sets.newHashSet(drtMode));
			log.info("filtered network contains {} links", filteredNetwork.getLinks().size());

			log.info("attempting to create {} drt vehicles for mode ", drtMode);
			new LeipzigDrtVehicleCreator().createDrtVehiclesForSingleArea(scenario.getVehicles(), filteredNetwork,
					feature, noVehicles, drtMode);

			//normally the following code would be set in prepareConfig, but..
			//.. the stops creator relies on a network with drt modes. Drt modes are added in RunLeipzigScenario.prepareScenario()..
			//.. so stops are created after that step -sme0823
			DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().
				filter(cfg -> cfg.getMode().equals(drtMode))
				.findFirst().orElseThrow();
				{
					//path, tho which stops.xml is saved
					URL path = IOUtils.extendUrl(scenario.getConfig().getContext(), "leipzig-v" + version + "-" + drtMode + "-stops.xml");
					File file;
					try {
						file = new File(path.toURI());
					} catch (URISyntaxException e) {
						throw new IllegalArgumentException(e);
					}

					//create drt stops and save them next to config -> put it as input stops file.
					//unfortunately there is no scenario.setDrtStops, so we have to do this workaround. -sme0723
					drtStopsCreator.processNetworkForStopCreation(scenario.getNetwork(), true, (Geometry) feature.getDefaultGeometry(),
						drtAreas.getShapeFile().toString() + "_" + drtConfigGroup.getMode() + "_stops.csv", drtConfigGroup.getMode(),
						file.toString(), drtAreas);

					//naming pattern comes from @DrtStopsWriter line 81. Should be ok to hard code it here. -sme0523
					drtConfigGroup.transitStopFile = file.toString();

				}
		}

		//if intermodality between pt and drt -> only railways are tagged as intermodal stations (this is how it is handled in reality) -sme0723
		if (ptDrtIntermodality.equals(PtDrtIntermodality.drtAsAccessEgressForPt)) {
			new PrepareTransitSchedule().prepareDrtIntermodality(scenario.getTransitSchedule(), drtAreas, true);
		}
	}

	/**
	 * prepare controler for drt simulation.
	 */
	public static void prepareControler(Controler controler, PtDrtIntermodality ptDrtIntermodality) {

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(), MultiModeDrtConfigGroup.class);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfigGroup));

		// if drt is stopBased, we want to write the drt stops into the global output -sme0723
		for (DrtConfigGroup drtCfg : multiModeDrtConfigGroup.getModalElements()) {
			if (drtCfg.operationalScheme.equals(DrtConfigGroup.OperationalScheme.stopbased)) {
				controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
					@Override
					public void install() {
						bindModal(DrtCaseSetup.StopsControlerListener.class).toProvider(modalProvider(
								getter -> new DrtCaseSetup.StopsControlerListener(drtCfg.getMode(),
										getter.get(OutputDirectoryHierarchy.class), drtCfg.transitStopFile
								)));
						addControlerListenerBinding().to(modalKey(DrtCaseSetup.StopsControlerListener.class));
					}
				});
			}

			// use estimate and teleport option
			controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.mode) {
				@Override
				public void install() {
//					bindModal(DrtEstimator.class).toInstance(DetourBasedDrtEstimator.normalDistributed(1.2, 32,
//						0.3, 120, 0.4));
					// The parameters used below are based on the analysis from the real world data
					bindModal(DrtEstimator.class).toProvider(modalProvider(getter -> new
						EuclideanDistanceBasedDrtEstimator(getter.getModal(Network.class), 2.0, 0.1577493 * 0.9,
						103.0972273 * 0.9, 120, 0.4, -0.1, 0.28)));
				}
			});
		}

		if (ptDrtIntermodality.equals(PtDrtIntermodality.drtAsAccessEgressForPt)) {
			controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());
			controler.addOverridingModule(new PtIntermodalRoutingModesModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
				}
			});
		}



	}

	/**
	 * if no modal params existing, we have to create them.
	 */
	private static void createDrtModeConfigGroup(MultiModeDrtConfigGroup multiModeDrtConfigGroup, String mode/*, String pathToShp*/) {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.mode = mode;
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.stopbased;
		drtConfigGroup.maxTravelTimeAlpha = 1.5;
		drtConfigGroup.maxTravelTimeBeta = 1200.;
		drtConfigGroup.maxWaitTime = 1200.;
		drtConfigGroup.maxWalkDistance = 1500.;
		drtConfigGroup.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
		drtConfigGroup.stopDuration = 60.;
		drtConfigGroup.useModeFilteredSubnetwork = true;

		//add insertionSearch params
		ExtensiveInsertionSearchParams insertionSearchParams = new ExtensiveInsertionSearchParams();
		drtConfigGroup.addParameterSet(insertionSearchParams);

		//we do not simulate drt explicitly anymore, so rebalancing is commented out. tschlenther feb' 24
		{
//			//add rebalancing params and configure standard values
//			RebalancingParams rebalancingParams = new RebalancingParams();
//
//		MinCostFlowRebalancingStrategyParams rebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
//		rebalancingStrategyParams.targetAlpha = 0.5;
//		rebalancingStrategyParams.targetBeta = 0.5;
//		rebalancingParams.addParameterSet(rebalancingStrategyParams);
//		drtConfigGroup.addParameterSet(rebalancingParams);
//
//		DrtZonalSystemParams zonalSystemParams = new DrtZonalSystemParams();
//		zonalSystemParams.zonesGeneration = DrtZonalSystemParams.ZoneGeneration.ShapeFile;
//		zonalSystemParams.zonesShapeFile = new File(pathToShp).getAbsolutePath(); //this needs to be adjusted: should only be the one corresponding feature and not the whole shape file!!
//		drtConfigGroup.addParameterSet(zonalSystemParams);
//
		}

		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
	}

	/**
	 * configure all other config groups, where drt modes need to be included: SMC, ChangeMode, ModeParams.
	 */
	private static void configureNecessaryConfigGroups(Config config, String mode) {

		ScoringConfigGroup planCalcScoreConfigGroup = ConfigUtils.addOrGetModule(config, ScoringConfigGroup.class);
		ChangeModeConfigGroup changeModeConfigGroup = ConfigUtils.addOrGetModule(config, ChangeModeConfigGroup.class);
		SubtourModeChoiceConfigGroup smcCfg = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);

		//add drt mode to modeParams if it does not exist yet
		if (!planCalcScoreConfigGroup.getModes().containsKey(mode)) {
			ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(mode);
			modeParams.setConstant(planCalcScoreConfigGroup.getModes().get(TransportMode.pt).getConstant());
			modeParams.setMarginalUtilityOfTraveling(0.);

			ScoringConfigGroup.ScoringParameterSet scoringParams = planCalcScoreConfigGroup.getOrCreateScoringParameters(null);
			scoringParams.addModeParams(modeParams);
		}

		//add drt modes to changeModeCfgGroup
		if (!Arrays.stream(changeModeConfigGroup.getModes()).toList().contains(mode)) {
			List<String> modes = new ArrayList<>(Arrays.asList(changeModeConfigGroup.getModes()));
			modes.add(mode);
			changeModeConfigGroup.setModes(modes.toArray(new String[modes.size()]));
		}

		//add drt modes to SMC
		if (!Arrays.stream(smcCfg.getModes()).toList().contains(mode)) {
			List<String> modes = new ArrayList<>(Arrays.asList(smcCfg.getModes()));
			modes.add(mode);
			smcCfg.setModes(modes.toArray(new String[modes.size()]));
		}
	}

	private static void configureDrtFareCompensation(Config config, Set<String> nonPtModes, Double ptBaseFare) {
		IntermodalTripFareCompensatorsConfigGroup intermodalTripFareCompensatorsConfigGroup =
				ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

		IntermodalTripFareCompensatorConfigGroup drtFareCompensator = new IntermodalTripFareCompensatorConfigGroup();
		drtFareCompensator.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);

		//Flexa is integrated into pt system, so users only pay once
		drtFareCompensator.setCompensationMoneyPerTrip(ptBaseFare);
		drtFareCompensator.setNonPtModes(ImmutableSet.copyOf(nonPtModes));

		intermodalTripFareCompensatorsConfigGroup.addParameterSet(drtFareCompensator);

		//for intermodality between pt and drt the following modules have to be installed and configured
		String artificialPtMode = "pt_w_drt_allowed";
		PtIntermodalRoutingModesConfigGroup ptIntermodalRoutingModesConfig = ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
		PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet ptIntermodalRoutingModesParamSet
				= new PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet();

		ptIntermodalRoutingModesParamSet.setDelegateMode(TransportMode.pt);
		ptIntermodalRoutingModesParamSet.setRoutingMode(artificialPtMode);

		PtIntermodalRoutingModesConfigGroup.PersonAttribute2ValuePair personAttrParamSet
				= new PtIntermodalRoutingModesConfigGroup.PersonAttribute2ValuePair();
		personAttrParamSet.setPersonFilterAttribute("canUseDrt");
		personAttrParamSet.setPersonFilterValue("true");
		ptIntermodalRoutingModesParamSet.addPersonAttribute2ValuePair(personAttrParamSet);

		ptIntermodalRoutingModesConfig.addParameterSet(ptIntermodalRoutingModesParamSet);

		//SRRConfigGroup needs to have the same personFilterAttr and Value as PtIntermodalRoutingModesConfigGroup
		SwissRailRaptorConfigGroup ptConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		ptConfig.setUseIntermodalAccessEgress(true);

		for (String drtMode : nonPtModes) {
			SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet intermodalParamSet = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
			intermodalParamSet.setMode(drtMode);
			ptConfig.addParameterSet(intermodalParamSet);
			intermodalParamSet.setPersonFilterAttribute("canUseDrt");
			intermodalParamSet.setPersonFilterValue("true");
			intermodalParamSet.setInitialSearchRadius(10000.);
			intermodalParamSet.setMaxRadius(10000.);
			intermodalParamSet.setSearchExtensionRadius(1000.);
			intermodalParamSet.setStopFilterAttribute("allowDrtAccessEgress");
		}

		//finally the new pt mode has to be added to subtourModeChoice
		SubtourModeChoiceConfigGroup modeChoiceConfigGroup = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);
		List<String> modes = new ArrayList<>();
		Collections.addAll(modes, modeChoiceConfigGroup.getModes());
		modes.add(artificialPtMode);
		modeChoiceConfigGroup.setModes(modes.toArray(new String[0]));
	}

	private record StopsControlerListener(String mode,
										  OutputDirectoryHierarchy controlerIO,
										  String stopsFile) implements StartupListener {

		private static final String OUTPUT_FILE_NAME = "stops.xml";

		@Override
		public void notifyStartup(StartupEvent event) {
			try {
				Files.copy(Path.of(stopsFile), Path.of(controlerIO.getOutputFilename(mode + "_" + OUTPUT_FILE_NAME)));
			} catch (IOException e) {
				log.fatal(e);
			}
		}
	}
}
