package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.*;
import org.matsim.analysis.emissions.RunOfflineAirPollutionAnalysisByVehicleCategory;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.CheckPopulation;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.analysis.population.SubTourAnalysis;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.application.analysis.travelTimeValidation.TravelTimeAnalysis;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.CreateLandUseShp;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.application.prepare.network.CreateNetworkFromSumo;
import org.matsim.application.prepare.population.*;
import org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.Bicycles;
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
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.optDRT.OptDrt;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.run.prepare.*;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

/**
 * Run the Leipzig scenario.  All the upstream stuff (network generation, initial demand generation) is in the Makefile.
 */
@CommandLine.Command(header = ":: Open Leipzig Scenario ::", version = RunLeipzigScenario.VERSION)
@MATSimApplication.Prepare({
		CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
		MergePopulations.class, ExtractRelevantFreightTrips.class, DownSamplePopulation.class, PrepareNetwork.class, CleanNetwork.class,
		CreateLandUseShp.class, ResolveGridCoordinates.class, PreparePopulation.class, CleanPopulation.class, AdjustActivityToLinkDistances.class,
		FixSubtourModes.class, FixNetwork.class
})
@MATSimApplication.Analysis({
		CheckPopulation.class, TravelTimeAnalysis.class, LinkStats.class, SubTourAnalysis.class, DrtServiceQualityAnalysis.class,
		DrtVehiclesRoadUsageAnalysis.class, ParkedVehiclesAnalysis.class, NoiseAnalysis.class
})
public class RunLeipzigScenario extends MATSimApplication {

	private static final Logger log = LogManager.getLogger(RunLeipzigScenario.class);

	static final String VERSION = "1.2";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(1, 10, 25);

	@CommandLine.Option(names = "--with-drt", defaultValue = "false", description = "Enable DRT service")
	private boolean drt;

	@CommandLine.Option(names = "--waiting-time-threshold-optDrt", description = "Set waitingTime Threshold fot DRT optimization and enable it. Here, enabling DRT service is mandatory.")
	private Double waitingTimeThreshold;

	@CommandLine.Option(names = "--bikes", defaultValue = "true", description = "Enable qsim for bikes", negatable = true)
	private boolean bike;

	@CommandLine.Option(names = "--parking-cost", defaultValue = "false", description = "Enable parking costs on network", negatable = true)
	private boolean parkingCost;

	@CommandLine.Option(names = "--income-dependent", defaultValue = "true", description = "Income dependent scoring", negatable = true)
	private boolean incomeDependent;

	@CommandLine.Option(names = "--tempo30Zone", defaultValue = "false", description = "measures to reduce car speed to 30 km/h")
	boolean tempo30Zone;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.ArgGroup(heading = "%nNetwork options%n", exclusive = false, multiplicity = "0..1")
	private final NetworkOptions network = new NetworkOptions();

	public RunLeipzigScenario(@Nullable Config config) {
		super(config);
	}

	public RunLeipzigScenario() {
		super(String.format("scenarios/input/leipzig-v%s-25pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunLeipzigScenario.class, args);
	}

	@Nullable
	@Override
	protected Config prepareConfig(Config config) {

		SnzActivities.addScoringParams(config);

		if (sample.isSet()) {
			config.controler().setOutputDirectory(sample.adjustName(config.controler().getOutputDirectory()));
			config.controler().setRunId(sample.adjustName(config.controler().getRunId()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

			config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
			config.qsim().setStorageCapFactor(sample.getSize() / 100.0);
		}

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
		// (yyyy what exactly is this doing?)

		if (drt) {
			MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfigGroup, config.planCalcScore(), config.plansCalcRoute());

			if (waitingTimeThreshold != null) {
				ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
			}
		}

		if (bike) {

			log.info("Simulating with bikes on the network");

			BicycleConfigGroup bikeConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
			bikeConfigGroup.setBicycleMode(TransportMode.bike);

			config.qsim().setUsingTravelTimeCheckInTeleportation(true);
			config.qsim().setUsePersonIdForMissingVehicleId(false);

			Set<String> modes = Sets.newHashSet(TransportMode.bike);
			modes.addAll(config.qsim().getMainModes());

			config.qsim().setMainModes(modes);
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

		} else
			log.warn("Bikes on network are disabled");

		if (parkingCost) {
			ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);
		}

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {


		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				Set<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		if (drt) {
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		}

		network.prepare(scenario.getNetwork());

		if (tempo30Zone) {
			SlowSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
		}
	}

	@Override
	protected void prepareControler(Controler controler) {
		Config config = controler.getConfig();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LeipzigPtFareModule());
				install(new SwissRailRaptorModule());

				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());

				if (incomeDependent) {
					bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
				}

				bind(AnalysisMainModeIdentifier.class).to(LeipzigMainModeIdentifier.class);
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				if (network.hasCarFreeArea()) {
					bind(MultimodalLinkChooser.class).to(CarfreeMultimodalLinkChooser.class);
				}

				if (parkingCost) {
					install(new ParkingCostModule());
					install(new PersonMoneyEventsAnalysisModule());
				}

				addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);

				Multibinder<StrategyWeightFadeout.Schedule> schedules = StrategyWeightFadeout.getBinder(binder());

				// Mode-choice fades out earlier than the other strategies
				// Given a fixed mode, the "less disruptive" choice dimensions will be weighted higher during the end
				schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice, "person", 0.65, 0.80));

				// Fades out until 0.9 (innovation switch off)
				schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute, "person", 0.75));
				schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator, "person", 0.75));

				bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
				}).toInstance(new ForceInnovationStrategyChooser<>(10, ForceInnovationStrategyChooser.Permute.yes));
			}
		});

		if (drt) {
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

			//here we need to set optDrt parameters for each drt mode; especially the wished fleetSizeAdjustmentWaitingTimeThreshold
			//my idea is to a waitingTime calculated by some manually configured drt fleet and see if optDrt suggests the same fleet size -sm0922
			if (waitingTimeThreshold != null) {
				MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
				multiModeOptDrtConfigGroup.setUpdateInterval(20);

				multiModeDrtConfigGroup.getModalElements().forEach(drtConfigGroup -> {
					OptDrtConfigGroup optDrtConfigGroup = new OptDrtConfigGroup();

					optDrtConfigGroup.setOptDrtMode(drtConfigGroup.getMode());
					optDrtConfigGroup.setFleetSizeAdjustmentApproach(OptDrtConfigGroup.FleetSizeAdjustmentApproach.WaitingTimeThreshold);
					optDrtConfigGroup.setWaitingTimeThresholdForFleetSizeAdjustment(waitingTimeThreshold);
					optDrtConfigGroup.setFleetSizeAdjustmentPercentage(0.5);

					multiModeOptDrtConfigGroup.addParameterSet(optDrtConfigGroup);
				});

				OptDrt.addAsOverridingModule(controler, multiModeOptDrtConfigGroup);
			}
		}

		if (bike) {
			Bicycles.addAsOverridingModule(controler);
		}
	}

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

	@Override
	protected List<MATSimAppCommand> preparePostProcessing(Path outputFolder, String runId) {

		String hbefaFileWarm = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";
		String hbefaFileCold = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/ColdStart_Vehcat_2020_Average_withHGVetc.csv.enc";

		return List.of(new RunOfflineAirPollutionAnalysisByVehicleCategory(outputFolder.toString(), runId, hbefaFileWarm, hbefaFileCold, outputFolder.toString()));
	}
}
