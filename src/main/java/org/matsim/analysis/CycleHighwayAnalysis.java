package org.matsim.analysis;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.joining.DataFrameJoiner;
import tech.tablesaw.selection.Selection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;
import static tech.tablesaw.aggregate.AggregateFunctions.count;

@CommandLine.Command(name = "cycle-highway", description = "Calculates various cycle highway related metrics.")
@CommandSpec(
	requireRunDirectory=true,
	produces = {"mode_share.csv", "mode_share_base.csv", "mode_shift.csv", "bike_income_groups.csv", "bike_income_groups_base.csv", "allModes_income_groups_base_leipzig.csv",
		"bike_income_groups_base_leipzig.csv", "car_income_groups_base_leipzig.csv", "walk_income_groups_base_leipzig.csv", "ride_income_groups_base_leipzig.csv",
		"pt_income_groups_base_leipzig.csv", "cycle_highway_agents_trip_start_end.csv", "mean_travel_stats.csv", "cycle_highways.shp"}
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
	@CommandLine.Option(names = "--highways-shp-path", description = "Path to run directory of base case.", required = true)
	private String highwaysShpPath;
	@CommandLine.Option(names = "--crs", description = "CRS for shp files.", defaultValue = "EPSG:25832")
	private String crs;
	@CommandLine.Option(names = "--dist-groups", split = ",", description = "List of distances for binning", defaultValue = "0.,1000.,2000.,5000.,10000.,20000.")
	private List<Double> distGroups;
	@CommandLine.Option(names = "--income-groups", split = ",", description = "List of income for binning. Derived from SrV 2018.", defaultValue = "0.,500.,900.,1500.,2000.,2600.,3000.,3600.,4600.,5600.")
	private List<Double> incomeGroups;

	List<String> modeOrder = null;
//	cannot use the original String from class CreateBicycleHighwayNetwork because the class is on another branch. In the matsim version of this branch Simwrapper was not yet implemented
	private static final String LINK_PREFIX = "cycle-highway-";
	private static final String INCOME_SUFFIX = "_income_groups_base_leipzig.csv";
	private static final String PERSON = "person";
	private static final String INCOME = "income";
	private static final String TRAV_TIME = "trav_time";
	private static final String TRAV_DIST = "traveled_distance";
	private static final String MAIN_MODE = "main_mode";
	private static final String LONG_MODE = "longest_distance_mode";
	private static final String TRIP_ID = "trip_id";
//	private static final String COUNT_PERSON = "Count [person]";
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
			.columnTypesPartial(Map.of(PERSON, ColumnType.TEXT, INCOME, ColumnType.DOUBLE, "subpopulation", ColumnType.TEXT))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(personsPath)).build());

		Map<String, ColumnType> columnTypes = new HashMap<>(Map.of(PERSON, ColumnType.TEXT,
			TRAV_TIME, ColumnType.STRING, "dep_time", ColumnType.STRING,
			LONG_MODE, ColumnType.STRING, MAIN_MODE, ColumnType.STRING, TRAV_DIST, ColumnType.DOUBLE,
			"first_act_x", ColumnType.DOUBLE, "first_act_y", ColumnType.DOUBLE, TRIP_ID, ColumnType.TEXT));

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(tripsPath))
			.columnTypesPartial(columnTypes)
			.sample(false)
			.separator(CsvOptions.detectDelimiter(tripsPath)).build());

		Table basePersons = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(basePersonsPath))
			.columnTypesPartial(Map.of(PERSON, ColumnType.TEXT, INCOME, ColumnType.DOUBLE))
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

//		add group columns for dist and income
		addGroupColumn(basePersons, INCOME, incomeGroups, incomeLabels);
		addGroupColumn(persons, INCOME, incomeGroups, incomeLabels);

//		filter Leipzig agents
		Table basePersonsLeipzig = filterLeipzigAgents(basePersons);
		Table personsLeipzig = filterLeipzigAgents(persons);

//		the 2 populations should consist of the same persons
		if (basePersonsLeipzig.rowCount() != personsLeipzig.rowCount()) {
			log.fatal("Number of agents living in Leipzig for base ({}) and policy case ({}) are not the same!", basePersonsLeipzig.rowCount(), personsLeipzig.rowCount());
			throw new IllegalStateException();
		}

		// Use longest_distance_mode where main_mode is not present
		trips.stringColumn(MAIN_MODE)
			.set(trips.stringColumn(MAIN_MODE).isMissing(),
				trips.stringColumn(LONG_MODE));
		baseTrips.stringColumn(MAIN_MODE)
			.set(baseTrips.stringColumn(MAIN_MODE).isMissing(),
				baseTrips.stringColumn(LONG_MODE));


//		calc modal split for base and policy
		writeModeShare(trips, personsLeipzig, distLabels, "mode_share.csv");
		writeModeShare(baseTrips, basePersonsLeipzig, distLabels, "mode_share_base.csv");

//		calc modal shift base to policy
		writeModeShift(trips, baseTrips);

//		join persons and trips
		Table joined = new DataFrameJoiner(trips, PERSON).inner(persons);
		Table baseJoined = new DataFrameJoiner(baseTrips, PERSON).inner(basePersons);

//		write income group distr for mode bike in policy and base
		writeIncomeGroups(joined, incomeLabels, TransportMode.bike, "_income_groups.csv");
		writeIncomeGroups(baseJoined, incomeLabels, TransportMode.bike, "_income_groups_base.csv");

//		write income group distr for every mode in base (Leipzig)
		Table baseJoinedLeipzig = new DataFrameJoiner(baseTrips, PERSON).inner(basePersonsLeipzig);

		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, "allModes", INCOME_SUFFIX);
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.bike, INCOME_SUFFIX);
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.car, INCOME_SUFFIX);
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.walk, INCOME_SUFFIX);
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.pt, INCOME_SUFFIX);
		writeIncomeGroups(baseJoinedLeipzig, incomeLabels, TransportMode.ride, INCOME_SUFFIX);

//		filter for bike trips
		Table bikeJoined = filterModeAgents(joined, TransportMode.bike);

//		filter for trips "cycleHighwayAgents" map
		IntList idx = new IntArrayList();

		for (int i = 0; i < bikeJoined.rowCount(); i++) {
			Row row = bikeJoined.row(i);

			int tripStart = durationToSeconds(row.getString("dep_time"));
//			waiting time already included in travel time
			int travelTime = durationToSeconds(row.getString(TRAV_TIME));

			List<Integer> enterTimes = highwayPersons.get(row.getString(PERSON));

//			TODO: enterTimes is null, fix this
			for (int enterTime : enterTimes) {
				if (Range.of(tripStart, tripStart + travelTime).contains(enterTime)) {
					idx.add(i);
				}
			}
		}
//		write trip start and end of every trip using cycle highway to csv
		bikeJoined = bikeJoined.where(Selection.with(idx.toIntArray())).selectColumns(PERSON, "start_x", "start_y", "end_x", "end_y");
		bikeJoined.write().csv(output.getPath("cycle_highway_agents_trip_start_end.csv").toFile());

//		here: filter base trip ids for trip ids of bikeJoined
//		TODO: check if this filter works properly
		TextColumn tripIdCol = baseJoined.textColumn(TRIP_ID);
		baseJoined = baseJoined.where(tripIdCol.isIn(bikeJoined.textColumn(TRIP_ID)));

		calcAndWriteMeanStats(bikeJoined, baseJoined);

		writeHighwaysShpFile();


//		TODO: figure like plot 4.5 MA thesis?
		return 0;
	}

	private void calcAndWriteMeanStats(Table bikeJoined, Table baseJoined) throws IOException {
		DoubleColumn distanceCol = bikeJoined.doubleColumn(TRAV_DIST);
		DoubleColumn timeCol = bikeJoined.doubleColumn(TRAV_TIME);

//		calc mean / median distances / times
		double meanDist = distanceCol.mean();
		double medianDist = distanceCol.median();
		double meanTravTime = timeCol.mean();
		double medianTravTime = timeCol.median();

		DoubleColumn baseDistanceCol = baseJoined.doubleColumn(TRAV_DIST);
		DoubleColumn baseTimeCol = baseJoined.doubleColumn(TRAV_TIME);

		double baseMeanDist = baseDistanceCol.mean();
		double baseMedianDist = baseDistanceCol.median();
		double baseMeanTravTime = baseTimeCol.mean();
		double baseMedianTravTime = baseTimeCol.median();

		//		write mean stats to csv
		DecimalFormat f = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(output.getPath("mean_travel_stats.csv").toString()),
			CSVFormat.DEFAULT.builder()
			.setQuote(null)
			.setDelimiter(',')
			.setRecordSeparator("\r\n")
			.build())) {
			printer.printRecord("\"mean travel distance policy case\"", f.format(meanDist));
			printer.printRecord("\"mean travel distance base case\"", f.format(baseMeanDist));
			printer.printRecord("\"median travel distance policy case\"", f.format(medianDist));
			printer.printRecord("\"median travel distance base case\"", f.format(baseMedianDist));
			printer.printRecord("\"mean travel time policy case\"", f.format(meanTravTime));
			printer.printRecord("\"mean travel time base case\"", f.format(baseMeanTravTime));
			printer.printRecord("\"median travel time policy case\"", f.format(medianTravTime));
			printer.printRecord("\"median travel time base case\"", f.format(baseMedianTravTime));
		}
	}

	private void writeHighwaysShpFile() throws IOException {

		ShapeFileWriter.writeGeometries(new ShpOptions(highwaysShpPath, crs, null).readFeatures(), output.getPath("cycle_highways.shp").toString());

//		We cannot use the same output option for 2 different files, so the string has to be manipulated
		String prj = output.getPath("cycle_highways.shp").toString().replace(".shp", ".prj");

//		.prj file needs to be simplified to make it readable for simwrapper
		try (BufferedWriter writer = IOUtils.getBufferedWriter(prj)) {
			writer.write(crs);
		}
	}

	private void writeIncomeGroups(Table joined, List<String> incomeLabels, String mode, String outputFile) {

//		only filter if specific mode is given
		if (!mode.equals("allModes")){
			joined = filterModeAgents(joined, mode);
		}

		Table aggr = joined.summarize(TRIP_ID, count).by("income_group");

		DoubleColumn countColumn = aggr.doubleColumn("Count [trip_id]");
		DoubleColumn share = countColumn.divide(countColumn.sum()).setName("share");
		aggr.addColumns(share);

		// Sort by income_group
		Comparator<Row> cmp = Comparator.comparingInt(row -> incomeLabels.indexOf(row.getString("income_group")));
		aggr = aggr.sortOn(cmp);

		aggr.write().csv(output.getPath(mode + outputFile).toFile());
	}

	private void writeModeShift(Table trips, Table baseTrips) {
		baseTrips.column(MAIN_MODE).setName("original_mode");

		Table joined = new DataFrameJoiner(trips, TRIP_ID).inner(true, baseTrips);
		Table aggr = joined.summarize(TRIP_ID, count).by("original_mode", MAIN_MODE);

		aggr.write().csv(output.getPath("mode_shift.csv").toFile());

//		rename column again because we need the column as main_mode later
		baseTrips.column("original_mode").setName(MAIN_MODE);
	}

	private void writeModeShare(Table trips, Table persons, List<String> labels, String outputFile) {

//		join needed to filter for Leipzig agents only
		Table joined = new DataFrameJoiner(trips, PERSON).inner(persons);

		addGroupColumn(joined, TRAV_DIST, distGroups, labels);

		Table aggr = joined.summarize(TRIP_ID, count).by(TRAV_DIST + "_group", MAIN_MODE);

		DoubleColumn share = aggr.numberColumn(2).divide(aggr.numberColumn(2).sum()).setName("share");
		aggr.replaceColumn(2, share);

		// Sort by dist_group and mode
		Comparator<Row> cmp = Comparator.comparingInt(row -> labels.indexOf(row.getString(TRAV_DIST + "_group")));
		aggr = aggr.sortOn(cmp.thenComparing(row -> row.getString(MAIN_MODE)));

		aggr.write().csv(output.getPath(outputFile).toFile());

		// Derive mode order if not given
		if (modeOrder == null) {
			modeOrder = new ArrayList<>();
			for (Row row : aggr) {
				String mainMode = row.getString(MAIN_MODE);
				if (!modeOrder.contains(mainMode)) {
					modeOrder.add(mainMode);
				}
			}
		}
	}

	private List<String> getLabels(List<Double> groups) {
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < groups.size() - 1; i++) {
			labels.add(String.format("%d - %d", groups.get(i).intValue(), groups.get(i + 1).intValue()));
		}
		labels.add(groups.get(groups.size() - 1) + "+");
		groups.add(Double.MAX_VALUE);
		return labels;
	}

	private void addGroupColumn(Table table, String valueLabel, List<Double> groups, List<String> labels) {
		StringColumn group = table.doubleColumn(valueLabel)
			.map(dist -> cut(dist, groups, labels), ColumnType.STRING::create).setName(valueLabel + "_group");
		table.addColumns(group);
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

			if (subPop.equals(PERSON)) {
				idx.add(i);
			}
		}
		return persons.where(Selection.with(idx.toIntArray()));
	}

	private Table filterModeAgents(Table trips, String mode) {
		IntList idx = new IntArrayList();
		for (int i = 0; i < trips.rowCount(); i++) {
			Row row = trips.row(i);
			String mainMode = row.getString(MAIN_MODE);

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
