package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.*;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.analysis.population.SubTourAnalysis;
import org.matsim.application.analysis.traffic.LinkStats;
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
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.choosers.ForceInnovationStrategyChooser;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.run.prepare.*;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.smallScaleCommercialTrafficGeneration.CreateSmallScaleCommercialTrafficDemand;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Run the Leipzig scenario.  All the upstream stuff (network generation, initial demand generation) is in the Makefile.
 * For the simulation of policy cases input parameters from {@link org.matsim.run.prepare.NetworkOptions} are needed
 */
@CommandLine.Command(header = ":: Open Leipzig Scenario ::", version = RunLeipzigScenario.VERSION)
@MATSimApplication.Prepare({
	CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
	MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, CleanNetwork.class,
	CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, AdjustActivityToLinkDistances.class,
	SplitActivityTypesDuration.class, ExtractHomeCoordinates.class, FixSubtourModes.class, FixNetwork.class, PrepareTransitSchedule.class,
	CreateSmallScaleCommercialTrafficDemand.class
})
@MATSimApplication.Analysis({
	CheckPopulation.class, LinkStats.class, SubTourAnalysis.class, DrtServiceQualityAnalysis.class,
	DrtVehiclesRoadUsageAnalysis.class, ParkedVehiclesAnalysis.class, NoiseAnalysis.class
})
public class RunLeipzigScenario extends MATSimApplication {

	/**
	 * Coordinate system used in the scenario.
	 */
	public static final String CRS = "EPSG:25832";

	static final String VERSION = "1.1";

	private static final Logger log = LogManager.getLogger(RunLeipzigScenario.class);

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(1, 10, 25);
	@CommandLine.ArgGroup(heading = "%nNetwork options%n", exclusive = false, multiplicity = "0..1")
	private final NetworkOptions networkOpt = new NetworkOptions();

	@CommandLine.Option(names = "--relativeSpeedChange", defaultValue = "1", description = "provide a value that is bigger then 0.0 and smaller then 1.0, else the speed will be reduced to 20 km/h")
	Double relativeSpeedChange;
	@CommandLine.Option(names = "--bikes", defaultValue = "onNetworkWithStandardMatsim", description = "Define how bicycles are handled")
	private BicycleHandling bike;

	//TODO: define adequate values for the following doubles
	@CommandLine.Option(names = "--parking-cost-time-period-start", defaultValue = "0", description = "Start of time period for which parking cost will be charged.")
	private Double parkingCostTimePeriodStart;
	@CommandLine.Option(names = "--parking-cost-time-period-end", defaultValue = "0", description = "End of time period for which parking cost will be charged.")
	private Double parkingCostTimePeriodEnd;
	@CommandLine.Mixin
	private ShpOptions shp;

	public RunLeipzigScenario(@Nullable Config config) {
		super(config);
	}

	public RunLeipzigScenario() {
		super(String.format("input/v%s/leipzig-v%s-25pct.config.xml", VERSION, VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunLeipzigScenario.class, args);
		// This implicitly calls "call()", which then calls prepareConfig, prepareScenario, prepareControler from the class here, and then calls controler.run().
	}

	/**
	 * Replaces reroute strategy with leipzig specific one.
	 */
	private static void adjustStrategiesForParking(Config config) {
		Collection<StrategyConfigGroup.StrategySettings> modifiableCollectionOfOldStrategySettings = new ArrayList<>(config.strategy().getStrategySettings());
		config.strategy().clearStrategySettings();

		for (StrategyConfigGroup.StrategySettings strategySetting : modifiableCollectionOfOldStrategySettings) {
			if (strategySetting.getStrategyName().equals("ReRoute")) {
				StrategyConfigGroup.StrategySettings newReRouteStrategy = new StrategyConfigGroup.StrategySettings();
				newReRouteStrategy.setStrategyName(LeipzigRoutingStrategyProvider.STRATEGY_NAME);
				newReRouteStrategy.setSubpopulation(strategySetting.getSubpopulation());
				newReRouteStrategy.setWeight(strategySetting.getWeight());
				newReRouteStrategy.setDisableAfter(strategySetting.getDisableAfter());
				config.strategy().addStrategySettings(newReRouteStrategy);
			} else {
				config.strategy().addStrategySettings(strategySetting);
			}
		}
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);
		// senozon activity types that are always the same.  Differentiated by typical duration.

		if (sample.isSet()) {
			// in [%].  adjust if sample size is less than 100%

			config.controler().setOutputDirectory(sample.adjustName(config.controler().getOutputDirectory()));
			config.controler().setRunId(sample.adjustName(config.controler().getRunId()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

			config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
			config.qsim().setStorageCapFactor(sample.getSize() / 100.0);
		}


		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		// ok.  :-)

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		// (yyyy what exactly is this doing?)
		// walk probably is teleported.
		// but we do not know where the facilities are.  (Facilities are not written to file.)

		if (networkOpt.hasDrtArea()) {
			MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfigGroup, config.planCalcScore(), config.plansCalcRoute());
		}

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		// checks if a teleportation is physically possible (i.e. not too fast).

		config.qsim().setUsePersonIdForMissingVehicleId(false);

		// this is how it is supposed to be
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile);

		switch ((bike)) {
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
			default -> throw new IllegalStateException("Unexpected value: " + (bike));
		}

		if (networkOpt.hasParkingCostArea()) {
			ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);
			config.planCalcScore().addActivityParams(new PlanCalcScoreConfigGroup.ActivityParams(TripStructureUtils.createStageActivityType("parking")).setScoringThisActivityAtAll(false));

			adjustStrategiesForParking(config);

		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		// TODO: can be removed once v1.2 is done, because this is done in the preparation phase
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				Set<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		if (networkOpt.hasDrtArea()) {
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
			// (matsim core does not know about DRT routes. This makes it possible to read them before the controler is there.)
		}

		networkOpt.prepare(scenario.getNetwork());
		// (passt das Netz an aus den mitgegebenen shape files, z.B. parking area, car-free area, ...)
	}

	@Override
	protected void prepareControler(Controler controler) {
		Config config = controler.getConfig();

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

				if (networkOpt.hasCarFreeArea()) {
					bind(MultimodalLinkChooser.class).to(CarfreeMultimodalLinkChooser.class);
				}

				if (networkOpt.hasParkingCostArea()) {

					this.addEventHandlerBinding().toInstance(new TimeRestrictedParkingCostHandler(parkingCostTimePeriodStart, parkingCostTimePeriodEnd));
					this.addPersonPrepareForSimAlgorithm().to(LeipzigRouterPlanAlgorithm.class);
					this.addPlanStrategyBinding(LeipzigRoutingStrategyProvider.STRATEGY_NAME).toProvider(LeipzigRoutingStrategyProvider.class);

					install(new PersonMoneyEventsAnalysisModule());

				}

				// TODO FIXME yyyyyy replace by config option
				{
					addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);

					Multibinder<StrategyWeightFadeout.Schedule> schedules = StrategyWeightFadeout.getBinder(binder());

					// Mode-choice fades out earlier than the other strategies
					// Given a fixed mode, the "less disruptive" choice dimensions will be weighted higher during the end
					schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice, "person", 0.65, 0.80));

					// Fades out until 0.9 (innovation switch off)
					//TODO switch no new ReRoute!!!!
					schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(LeipzigRoutingStrategyProvider.STRATEGY_NAME, "person", 0.75));
					schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, "person", 0.75));

				}
				bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
				}).toInstance(new ForceInnovationStrategyChooser<>(10, ForceInnovationStrategyChooser.Permute.yes));
			}
		});

		if (networkOpt.hasDrtArea()) {
			// FIXME yyyyyy move above into prepareConfig
			// FIXME will be integrated into DrtCaseSetup class

			MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

			//set fare params; flexa has the same prices as leipzig PT: Values taken out of LeipzigPtFareModule -sm0522
			Double ptBaseFare = 2.4710702921120262;
			Double ptDistanceFare = 0.00017987993018495408;

			DrtFareParams drtFareParams = new DrtFareParams();
			drtFareParams.baseFare = ptBaseFare;
			drtFareParams.distanceFare_m = ptDistanceFare;
			drtFareParams.timeFare_h = 0.;
			drtFareParams.dailySubscriptionFee = 0.;

			Set<String> drtModes = new HashSet<>();

			multiModeDrtConfigGroup.getModalElements().forEach(drtConfigGroup -> {
				drtConfigGroup.addParameterSet(drtFareParams);
				DrtConfigs.adjustDrtConfig(drtConfigGroup, config.planCalcScore(), config.plansCalcRoute());
				drtModes.add(drtConfigGroup.getMode());
			});

			controler.addOverridingModule(new DvrpModule());
			controler.addOverridingModule(new MultiModeDrtModule());
			controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfigGroup));

			prepareDrtFareCompensation(config, controler, drtModes, ptBaseFare);
		}

		if (bike == BicycleHandling.onNetworkWithBicycleContrib) {
			controler.addOverridingModule(new BicycleModule());
		}
	}

	/**
	 * FIXME: will be moved into separate class.
	 */
	private void prepareDrtFareCompensation(Config config, Controler controler, Set<String> nonPtModes, Double ptBaseFare) {
		IntermodalTripFareCompensatorsConfigGroup intermodalTripFareCompensatorsConfigGroup =
			ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);

		IntermodalTripFareCompensatorConfigGroup drtFareCompensator = new IntermodalTripFareCompensatorConfigGroup();
		drtFareCompensator.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedAnywhereInTheDay);

		//Flexa is integrated into pt system, so users only pay once
		drtFareCompensator.setCompensationMoneyPerTrip(ptBaseFare);
		drtFareCompensator.setNonPtModes(ImmutableSet.copyOf(nonPtModes));

		intermodalTripFareCompensatorsConfigGroup.addParameterSet(drtFareCompensator);
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());

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

		controler.addOverridingModule(new PtIntermodalRoutingModesModule());

		//SRRConfigGroup needs to have the same personFilterAttr and Value as PtIntermodalRoutingModesConfigGroup
		SwissRailRaptorConfigGroup ptConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		for (SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSet : ptConfig.getIntermodalAccessEgressParameterSets()) {
			if (paramSet.getMode().contains("drt")) {
				paramSet.setPersonFilterAttribute("canUseDrt");
				paramSet.setPersonFilterValue("true");
			}
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
			}
		});

		//finally the new pt mode has to be added to subtourModeChoice
		SubtourModeChoiceConfigGroup modeChoiceConfigGroup = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);
		List<String> modes = new ArrayList<>();
		Collections.addAll(modes, modeChoiceConfigGroup.getModes());
		modes.add(artificialPtMode);
		modeChoiceConfigGroup.setModes(modes.toArray(new String[0]));
	}

	/**
	 * Defines how bicycles are scored.
	 */
	enum BicycleHandling {onNetworkWithStandardMatsim, onNetworkWithBicycleContrib}

}
