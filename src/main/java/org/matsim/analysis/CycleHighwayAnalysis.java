package org.matsim.analysis;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.Range;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;
import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "cycle-highway", description = "Calculates various cycle highway related metrics.")
@CommandSpec(
	requires = {"trips.csv", "persons.csv"},
//	TODO: define output csvs here
	produces = {"mode_share.csv", "mode_share_per_dist.csv", "mode_users.csv", "trip_stats.csv", "population_trip_stats.csv", "trip_purposes_by_hour.csv"}
)
public class CycleHighwayAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(CycleHighwayAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(CycleHighwayAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(CycleHighwayAnalysis.class);
	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path basePath;
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--dist-groups", split = ",", description = "List of distances for binning", defaultValue = "0.,1000.,2000.,5000.,10000.,20000.")
	private List<Double> distGroups;
	@CommandLine.Option(names = "--income-groups", split = ",", description = "List of income for binning. Derived from SrV 2018.", defaultValue = "0.,500.,900.,1500.,2000.,2600.,3000.,3600.,4600.,5600.")
	private List<Double> incomeGroups;

	List<String> modeOrder = null;
//	cannot use the original String from class CreateBicycleHighwayNetwork because the class is on another branch. In the matsim version of this branch Simwrapper was not yet implemented
	private static final String LINK_PREFIX = "cycle-highway-";
	private final Map<Id<Vehicle>, String> bikers = new HashMap<>();
	private final Map<String, List<Integer>> highwayPersons = new HashMap<>();

	public static void main(String[] args) {
		new CycleHighwayAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		//		all necessary file input paths are defined here
		String eventsPath = globFile(input.getRunDirectory(), "*output_events.xml.gz").toString();
		String personsPath = globFile(input.getRunDirectory(), "*output_persons.csv.gz").toString();
		String tripsPath = globFile(input.getRunDirectory(), "*output_trips.csv.gz").toString();
		String basePersonsPath = globFile(basePath, "*output_persons.csv.gz").toString();
		String baseTripsPath = globFile(basePath, "*output_trips.csv.gz").toString();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new CycleHighwayEventHandler());
		manager.initProcessing();

		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventsPath);
		manager.finishProcessing();

//		read necessary tables
		Table persons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(personsPath))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT, "income", ColumnType.DOUBLE, "subpopulation", ColumnType.TEXT))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(personsPath)).build());

		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of("person", ColumnType.TEXT,
			"trav_time", ColumnType.STRING, "dep_time", ColumnType.STRING,
			"longest_distance_mode", ColumnType.STRING, "main_mode", ColumnType.STRING,
			"start_activity_type", ColumnType.TEXT, "end_activity_type", ColumnType.TEXT, "traveled_distance", ColumnType.DOUBLE,
			"first_act_x", ColumnType.DOUBLE, "first_act_y", ColumnType.DOUBLE));

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(tripsPath))
			.columnTypesPartial(columnTypes)
			.sample(false)
			.separator(CsvOptions.detectDelimiter(tripsPath)).build());

		Table basePersons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(basePersonsPath))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT, "income", ColumnType.DOUBLE))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(basePersonsPath)).build());

		Table baseTrips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(baseTripsPath))
			.columnTypesPartial(columnTypes)
			.sample(false)
			.separator(CsvOptions.detectDelimiter(baseTripsPath)).build());

//		only analyze person agents
		basePersons = filterPersonAgents(basePersons);
		persons = filterPersonAgents(persons);


//		create labels for dist and income groups
		List<String> distLabels = getLabels(distGroups);
		List<String> incomeLabels = getLabels(incomeGroups);

//		TODO: test if this works with addGroupColumn as void
//		add group columns fro dist and income
		basePersons = addGroupColumn(basePersons, "income", incomeGroups, incomeLabels);
		persons = addGroupColumn(persons, "income", incomeGroups, incomeLabels);

//		filter Leipzig agents
		Table basePersonsLeipzig = filterLeipzigAgents(basePersons);
		Table personsLeipzig = filterLeipzigAgents(persons);

//		the 2 populations should consist of the same persons
		if (basePersonsLeipzig.rowCount() != personsLeipzig.rowCount()) {
			log.fatal("Number of agents living in Leipzig for base ({}) and policy case ({}) are not the same!", basePersonsLeipzig.rowCount(), personsLeipzig.rowCount());
			throw new IllegalStateException();
		}

		// Use longest_distance_mode where main_mode is not present
		trips.stringColumn("main_mode")
			.set(trips.stringColumn("main_mode").isMissing(),
				trips.stringColumn("longest_distance_mode"));
		baseTrips.stringColumn("main_mode")
			.set(baseTrips.stringColumn("main_mode").isMissing(),
				baseTrips.stringColumn("longest_distance_mode"));


//		calc modal split for base and policy
		writeModeShare(trips, personsLeipzig, distLabels, "mode_share.csv");
		writeModeShare(baseTrips, basePersonsLeipzig, distLabels, "mode_share_base.csv");

//		calc modal shift base to policy
		writeModeShift(trips, baseTrips);

//		join persons and trips
		Table joined = new DataFrameJoiner(trips, "person").inner(persons);
		Table baseJoined = new DataFrameJoiner(baseTrips, "person").inner(basePersons);

//		write income group distr for mode bike in policy and base
		writeIncomeGroups(joined, incomeLabels, TransportMode.bike, "_income_groups.csv");
		writeIncomeGroups(baseJoined, incomeLabels, TransportMode.bike, "_income_groups_base.csv");

//		write income group distr for every mode in base (Leipzig)
		Table baseJoinedLeipzig = new DataFrameJoiner(baseTrips, "person").inner(basePersonsLeipzig);

		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, "allModes", "_income_groups_base_leipzig.csv");
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.bike, "_income_groups_base_leipzig.csv");
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.car, "_income_groups_base_leipzig.csv");
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.walk, "_income_groups_base_leipzig.csv");
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.pt, "_income_groups_base_leipzig.csv");
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.ride, "_income_groups_base_leipzig.csv");

//		TODO for cycle highway agents end and start points
//		filter for bike trips
		Table bikeJoined = filterModeAgents(joined, TransportMode.bike);



//		filter for trips "cycleHighwayAgents" map
		IntList idx = new IntArrayList();

		for (int i = 0; i < bikeJoined.rowCount(); i++) {
			Row row = bikeJoined.row(i);

			int tripStart = durationToSeconds(row.getString("dep_time"));
//			waiting time already included in travel time
			int travelTime = durationToSeconds(row.getString("trav_time"));

			List<Integer> enterTimes = highwayPersons.get(row.getString("person"));

			for (int enterTime : enterTimes) {
				if (Range.of(tripStart, tripStart + travelTime).contains(enterTime)) {
					idx.add(i);
				}
			}
		}
//		write trip start and end of every trip using cycle highway to csv
		bikeJoined = bikeJoined.where(Selection.with(idx.toIntArray())).selectColumns("person", "start_x", "start_y", "end_x", "end_y");
		bikeJoined.write().csv(output.getPath("cycle-highway-agents-trip-start-end.csv").toFile());

//		TODO: mean travel dist / time before / after policy
//		here: filter base trip ids for trip ids of bikeJoined and calc mean / median before / after
//		TODO: median ""
//		TODO: bike traffic volumes -> comparison to Machbarkeitsstudie Halle-Leipzig. probably use code from OverviewDashboard or CarTrafficAnalysis?!





		return 0;
	}

	private void writeIncomeGroups(Table joined, List<String> incomeLabels, String mode, String outputFile) {

//		only filter if specific mode is given
		if (!mode.equals("allModes")){
			joined = filterModeAgents(joined, mode);
		}

		Table aggr = joined.summarize("trip_id", count).by("income_group");

//		TODO: column probably is not named "count"
		DoubleColumn countColumn = aggr.doubleColumn("count");
		DoubleColumn share = countColumn.divide(countColumn.sum()).setName("share");
		aggr.addColumns(share);

		// Sort by income_group
		Comparator<Row> cmp = Comparator.comparingInt(row -> incomeLabels.indexOf(row.getString("income_group")));
		aggr = aggr.sortOn(cmp);

		aggr.write().csv(output.getPath(mode + outputFile).toFile());
	}

	private void writeModeShift(Table trips, Table baseTrips) {
		baseTrips.column("main_mode").setName("original_mode");

		Table joined = new DataFrameJoiner(trips, "trip_id").inner(true, baseTrips);
		Table aggr = joined.summarize("trip_id", count).by("original_mode", "main_mode");

		aggr.write().csv(output.getPath("mode_shift.csv").toFile());
	}

	private void writeModeShare(Table trips, Table persons, List<String> labels, String outputFile) {

//		join needed to filter for Leipzig agents only
		Table joined = new DataFrameJoiner(trips, "person").inner(persons);

		joined = addGroupColumn(joined, "traveled_distance", distGroups, labels);

		Table aggr = joined.summarize("trip_id", count).by("dist_group", "main_mode");

		DoubleColumn share = aggr.numberColumn(2).divide(aggr.numberColumn(2).sum()).setName("share");
		aggr.replaceColumn(2, share);

		// Sort by dist_group and mode
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString("dist_group")));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString("main_mode")));

		aggr.write().csv(output.getPath(outputFile).toFile());

		// Derive mode order if not given
		if (modeOrder == null) {
			modeOrder = new ArrayList<>();
			for (Row row : aggr) {
				String mainMode = row.getString("main_mode");
				if (!modeOrder.contains(mainMode)) {
					modeOrder.add(mainMode);
				}
			}
		}
	}

	private List<String> getLabels(List<Double> groups) {
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < groups.size() - 1; i++) {
			labels.add(String.format("%d - %d", groups.get(i), groups.get(i + 1)));
		}
		labels.add(groups.get(groups.size() - 1) + "+");
		groups.add(Double.MAX_VALUE);
		return labels;
	}

	private Table addGroupColumn(Table table, String valueLabel, List<Double> groups, List<String> labels) {
		StringColumn group = table.doubleColumn(valueLabel)
			.map(dist -> cut(dist, groups, labels), ColumnType.STRING::create).setName(valueLabel + "_group");
		table.addColumns(group);

		return table;
	}

	private static String cut(double value, List<Double> groups, List<String> labels) {
		int idx = Collections.binarySearch(groups, value);

		if (idx >= 0)
			return labels.get(idx);

		int ins = -(idx + 1);
		return labels.get(ins - 1);
	}

	private Table filterLeipzigAgents(Table persons) {
		Geometry geometry = shp.getGeometry();
		GeometryFactory f = new GeometryFactory();

		IntList idx = new IntArrayList();
		for (int i = 0; i < persons.rowCount(); i++) {
			Row row = persons.row(i);
			Point p = f.createPoint(new Coordinate(row.getDouble("first_act_x"), row.getDouble("first_act_y")));
			if (geometry.contains(p)) {
				idx.add(i);
			}
		}
		return persons.where(Selection.with(idx.toIntArray()));
	}

	private Table filterPersonAgents(Table persons) {
		IntList idx = new IntArrayList();
		for (int i = 0; i < persons.rowCount(); i++) {
			Row row = persons.row(i);
			String subPop = row.getText("subpopulation");

			if (subPop.equals("person")) {
				idx.add(i);
			}
		}
		return persons.where(Selection.with(idx.toIntArray()));
	}

	private Table filterModeAgents(Table trips, String mode) {
		IntList idx = new IntArrayList();
		for (int i = 0; i < trips.rowCount(); i++) {
			Row row = trips.row(i);
			String mainMode = row.getString("main_mode");

			if (mainMode.equals(mode)) {
				idx.add(i);
			}
		}
		return trips.where(Selection.with(idx.toIntArray()));
	}

	private int durationToSeconds(String d) {
		String[] split = d.split(":");
		return (Integer.parseInt(split[0]) * 60 * 60) + (Integer.parseInt(split[1]) * 60) + Integer.parseInt(split[2]);
	}

	private final class CycleHighwayEventHandler implements PersonEntersVehicleEventHandler, LinkEnterEventHandler {

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
//			register personId with vehId to get personId in LinkEnterEvent
			if (event.getVehicleId().toString().contains(TransportMode.bike)) {
				bikers.putIfAbsent(event.getVehicleId(), event.getPersonId().toString());
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().toString().contains(LINK_PREFIX)) {
				if (!highwayPersons.containsKey(bikers.get(event.getVehicleId()))) {
					highwayPersons.put(bikers.get(event.getVehicleId()), new ArrayList<>());
				}
				highwayPersons.get(bikers.get(event.getVehicleId())).add((int) event.getTime());
			}
		}
	}
}
