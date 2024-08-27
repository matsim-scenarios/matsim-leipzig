package org.matsim.run;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.*;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.analysis.population.SubTourAnalysis;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.prepare.*;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Run the Leipzig scenario.  All the upstream stuff (network generation, initial demand generation) is in the Makefile.
 * For the simulation of policy cases input parameters from {@link org.matsim.run.prepare.NetworkOptions} are needed
 */
@CommandLine.Command(header = ":: Open Leipzig Scenario ::", version = LeipzigScenario.VERSION)
@MATSimApplication.Prepare({
	CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
	MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, CleanNetwork.class,
	CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, AdjustActivityToLinkDistances.class,
	SplitActivityTypesDuration.class, ExtractHomeCoordinates.class, FixSubtourModes.class, FixNetwork.class, PrepareTransitSchedule.class,
	GenerateSmallScaleCommercialTrafficDemand.class
})
@MATSimApplication.Analysis({
	CheckPopulation.class, LinkStats.class, SubTourAnalysis.class, DrtServiceQualityAnalysis.class,
	DrtVehiclesRoadUsageAnalysis.class, ParkedVehiclesAnalysis.class, NoiseAnalysis.class, TrafficAnalysis.class
})
public class LeipzigScenario extends MATSimApplication {

	/**
	 * Coordinate system used in the scenario.
	 */
	public static final String CRS = "EPSG:25832";
	/**
	 * Current version number.
	 */
	public static final String VERSION = "1.3";
	private static final Logger log = LogManager.getLogger(LeipzigScenario.class);
	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(1, 10, 25);
	@CommandLine.ArgGroup(heading = "%nNetwork options%n", exclusive = false, multiplicity = "0..1")
	private final NetworkOptions networkOpt = new NetworkOptions();

	@CommandLine.Option(names = "--bikes", defaultValue = "onNetworkWithStandardMatsim", description = "Define how bicycles are handled")
	private BicycleHandling bike;

	@CommandLine.Option(names = "--parking", defaultValue = "false", description = "Define if parking logic should be enabled.")
	private boolean parking = false;

	//TODO: define adequate values for the following doubles
	@CommandLine.Option(names = "--parking-cost-time-period-start", defaultValue = "0", description = "Start of time period for which parking cost will be charged.")
	private Double parkingCostTimePeriodStart;
	@CommandLine.Option(names = "--parking-cost-time-period-end", defaultValue = "0", description = "End of time period for which parking cost will be charged.")
	private Double parkingCostTimePeriodEnd;

	@CommandLine.Option(names = "--intermodality", defaultValue = "drtAndPtSeparateFromEachOther", description = "Define if drt should be used as access and egress mode for pt.")
	private DrtCaseSetup.PtDrtIntermodality ptDrtIntermodality;

	public LeipzigScenario(@Nullable Config config) {
		super(config);
	}

	public LeipzigScenario() {
		super(String.format("input/v%s/leipzig-v1.3.1-10pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(LeipzigScenario.class, args);
		// This implicitly calls "call()", which then calls prepareConfig, prepareScenario, prepareControler from the class here, and then calls controler.run().
	}

	/**
	 * Replaces reroute strategy with leipzig specific one.
	 */
	private static void adjustStrategiesForParking(Config config) {
		Collection<ReplanningConfigGroup.StrategySettings> modifiableCollectionOfOldStrategySettings = new ArrayList<>(config.replanning().getStrategySettings());
		config.replanning().clearStrategySettings();

		for (ReplanningConfigGroup.StrategySettings strategySetting : modifiableCollectionOfOldStrategySettings) {

			if (strategySetting.getStrategyName().equals("ReRoute")) {
				strategySetting.setStrategyName(LeipzigRoutingStrategyProvider.STRATEGY_NAME);
			} else if (strategySetting.getStrategyName().equals("SubtourModeChoice")) {
				strategySetting.setStrategyName(LeipzigSubtourModeChoice.STRATEGY_NAME);
			}

			config.replanning().addStrategySettings(strategySetting);

		}
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		// senozon activity types that are always the same.  Differentiated by typical duration.
		SnzActivities.addScoringParams(config);

		// Prepare commercial config
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("service").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(3600));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(3600));

		SimWrapperConfigGroup simWrapper = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		// Path is relative to config
		simWrapper.defaultParams().shp = "../leipzig-utm32n/leipzig-utm32n.shp";
		simWrapper.defaultParams().mapCenter = "12.38,51.34";
		simWrapper.defaultParams().mapZoomLevel = 10.3;

		for (String subpopulation : List.of("outside_person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(0.95)
					.setSubpopulation(subpopulation)
			);
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.05)
					.setSubpopulation(subpopulation)
			);
		}

		if (sample.isSet()) {
			// in [%].  adjust if sample size is less than 100%

			config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
			config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

			config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
			config.qsim().setStorageCapFactor(sample.getSize() / 100.0);

			simWrapper.sampleSize = sample.getSample();
		}


		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		// ok.  :-)

		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		// (yyyy what exactly is this doing?)
		// walk probably is teleported.
		// but we do not know where the facilities are.  (Facilities are not written to file.)



		if (networkOpt.hasDrtArea()) {
			//drt
			DrtCaseSetup.prepareConfig(config, /* drtCase, */ new ShpOptions(networkOpt.getDrtArea(), null, null), ptDrtIntermodality);
		}

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		// checks if a teleportation is physically possible (i.e. not too fast).

		config.qsim().setUsePersonIdForMissingVehicleId(false);

		// We need to use coordinates only, otherwise subtour constraints will be violated by the parking re-routing, because it may change link/facility ids
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		switch (bike) {
			case onNetworkWithStandardMatsim -> {
				// bike is routed on the network per the xml config.

				log.info("Simulating with bikes on the network");

				// add bike to network modes in qsim:
				Set<String> modes = Sets.newHashSet(TransportMode.bike);
				modes.addAll(config.qsim().getMainModes());
				config.qsim().setMainModes(modes);

				config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

			}
			case onNetworkWithBicycleContrib -> {
				// bike is routed on the network per the xml config.

				log.info("Simulating with bikes on the network and bicycle contrib");

				// add bike to network modes in qsim:
				Set<String> modes = Sets.newHashSet(TransportMode.bike);
				modes.addAll(config.qsim().getMainModes());
				config.qsim().setMainModes(modes);

				config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

				// this activates the bicycleConfigGroup.  But the module still needs to be loaded for the controler.
				BicycleConfigGroup bikeConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
				bikeConfigGroup.setBicycleMode(TransportMode.bike);
			}
			case bikeTeleportedStandardMatsim -> {

				log.info("Simulating with bikes teleported");
				RoutingConfigGroup plansCalcRouteConfigGroup = ConfigUtils.addOrGetModule(config, RoutingConfigGroup.class);

				if (plansCalcRouteConfigGroup.getNetworkModes().contains(TransportMode.bike)) {

					Collection<String> networkModes = Sets.newHashSet();

					for (String mode : plansCalcRouteConfigGroup.getNetworkModes()) {
						if (!mode.equals(TransportMode.bike)) {
							networkModes.add(mode);
						}
					}
					plansCalcRouteConfigGroup.setNetworkModes(networkModes);
				}

				if (!plansCalcRouteConfigGroup.getTeleportedModeParams().containsKey(TransportMode.bike)) {
					RoutingConfigGroup.TeleportedModeParams teleportedModeParams = new RoutingConfigGroup.TeleportedModeParams();
					teleportedModeParams.setMode(TransportMode.bike);
					teleportedModeParams.setBeelineDistanceFactor(1.3);
					teleportedModeParams.setTeleportedModeSpeed(3.1388889);
					plansCalcRouteConfigGroup.addTeleportedModeParams(teleportedModeParams);
				}
			}

			default -> throw new IllegalStateException("Unexpected value: " + bike);
		}

		if (parking) {
			ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(TripStructureUtils.createStageActivityType("parking")).setScoringThisActivityAtAll(false));
			adjustStrategiesForParking(config);
		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		//this has to be executed before DrtCaseSetup.prepareScenario() as the latter method relies on the drt mode being added to the network
		networkOpt.prepare(scenario.getNetwork());
		// (passt das Netz an aus den mitgegebenen shape files, z.B. parking area, car-free area, ...)

		if (networkOpt.hasDrtArea()) {
			DrtCaseSetup.prepareScenario(scenario, new ShpOptions(networkOpt.getDrtArea(), null, null), VERSION, ptDrtIntermodality);
		}
		if (networkOpt.hasCarFreeArea()){
			PreparePopulation.deleteCarAndRideRoutesThatHaveForbiddenLinks(scenario.getPopulation(), networkOpt.getNonPtLinksInCarFreeArea(scenario.getNetwork()));
		}
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());
		controler.addOverridingModule(new PtStop2StopAnalysisModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LeipzigPtFareModule());

				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());

				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();

				bind(AnalysisMainModeIdentifier.class).to(LeipzigMainModeIdentifier.class);

				// Plots how many different modes agents tried out
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				// Leipzig parking specific planning strategies
				if (parking) {
					this.addPersonPrepareForSimAlgorithm().to(LeipzigRouterPlanAlgorithm.class);
					this.addPlanStrategyBinding(LeipzigRoutingStrategyProvider.STRATEGY_NAME).toProvider(LeipzigRoutingStrategyProvider.class);
					this.addPlanStrategyBinding(LeipzigSubtourModeChoice.STRATEGY_NAME).toProvider(LeipzigSubtourModeChoice.class);

					// Normally this is bound with the default subtour mode choice, because we use our own variant this is bound again here
					bind(PermissibleModesCalculator.class).to(PermissibleModesCalculatorImpl.class);

					this.addEventHandlerBinding().toInstance(new TimeRestrictedParkingCostHandler(parkingCostTimePeriodStart, parkingCostTimePeriodEnd));

					install(new PersonMoneyEventsAnalysisModule());
				}
			}
		});

		/*
		 * i changed this from networkOpt.hasDrtArea() which relies on command line input to an automatic check of the config
		 * so that we _can_ run Drt simulations without calling DrtCaseSetup, i.e. without creating and configuring drt input in the code
		 * but rather by configuring it at the cml level.
		 * In this case, the modeller has to assure that the input (including the (allowed modes in the) network!!) is well prepared for drt, themselves!
		 * As the following checks whether the MultiModeDrtConfigGroup is configured, it still works with the DrtCaseSetup
		 * tschlenther feb'24.
		 */
		//if (networkOpt.hasDrtArea()) {
		if (ConfigUtils.hasModule(controler.getConfig(), MultiModeDrtConfigGroup.class)) {
			DrtCaseSetup.prepareControler(controler, ptDrtIntermodality);
		}

		if (bike == BicycleHandling.onNetworkWithBicycleContrib) {
			controler.addOverridingModule(new BicycleModule());
		}

		if (networkOpt.hasEBikeCity()) {
			controler.getScenario().getVehicles().getVehicleTypes().get(TransportMode.bike).setMaximumVelocity(8.32);

		}
	}

	/**
	 * Defines how bicycles are scored.
	 */
	enum BicycleHandling {onNetworkWithStandardMatsim, onNetworkWithBicycleContrib, bikeTeleportedStandardMatsim}
}
