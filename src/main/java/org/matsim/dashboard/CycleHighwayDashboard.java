package org.matsim.dashboard;

import org.matsim.analysis.CycleHighwayAnalysis;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows information about an optional policy case, which implements cycle highways in Leipzig.
 * It also compares the agents and their trips using the cycle highways with their respective trips in the base case.
 */
public class CycleHighwayDashboard implements Dashboard {
	private final String basePath;
	private final String shp;
	private final String highwaysShp;
	private static final String SHARE = "share";
	private static final String ABSOLUTE = "Count [trip_id]";
	private static final String INCOME_GROUP = "income_group";
	private static final String CRS = "EPSG:25832";
	private static final String SOURCE = "source";
	private static final String MAIN_MODE = "main_mode";
	private static final String TRAFFIC = "traffic";

	CycleHighwayDashboard(String basePath, String shp, String highwaysShp) {
		if (!basePath.endsWith("/")) {
			basePath += "/";
		}
		this.basePath = basePath;
		this.shp = shp;
		this.highwaysShp = highwaysShp;
	}

	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Cycle Highways Dashboard";
		header.description = "Shows statistics about agents, who used the newly implemented cycle highway " +
			"and compares to the corresponding trips in the base case.";

		String[] args = new ArrayList<>(List.of("--base-path", basePath, "--shp", shp, "--highways-shp-path", highwaysShp)).toArray(new String[0]);

		layout.row("first")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "mean_travel_stats.csv", args);
				viz.height = 0.1;
			});

		layout.row("modalSplit")
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Modal split";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				Plotly.DataSet ds = viz.addDataset(data.compute(CycleHighwayAnalysis.class, "mode_share.csv", args))
					.constant(SOURCE, "Policy")
					.aggregate(List.of(MAIN_MODE), SHARE, Plotly.AggrFunc.SUM);

				Plotly.DataSet dsBase = viz.addDataset(data.compute(CycleHighwayAnalysis.class, "mode_share_base.csv", args))
					.constant(SOURCE, "Base")
					.aggregate(List.of(MAIN_MODE), SHARE, Plotly.AggrFunc.SUM);

				viz.mergeDatasets = true;

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					ds.mapping()
						.name(MAIN_MODE)
						.y(SOURCE)
						.x(SHARE)
				);
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					dsBase.mapping()
						.name(MAIN_MODE)
						.y(SOURCE)
						.x(SHARE)
				);
			})
			.el(Sankey.class, (viz, data) -> {
				viz.title = "Mode shift (to bike)";
				viz.width = 1.5d;
				viz.description = "by main mode. Compares base case output with output after the last iteration";
				viz.csv = data.compute(CycleHighwayAnalysis.class, "mode_shift.csv", args);
			});

		layout.row("locations")
			.el(Hexagons.class, (viz, data) -> {

				viz.title = "Cycle highway trips - Origins";
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.height = 7.5;
				viz.width = 2.0;
				viz.file = data.compute(CycleHighwayAnalysis.class, "cycle_highway_agents_trip_start_end.csv");
				viz.projection = CRS;
				viz.addAggregation("trip origins", "person", "start_x", "start_y");
			})
			.el(Hexagons.class, (viz, data) -> {
				viz.title = "Cycle highway trips - Destinations";
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.height = 7.5;
				viz.width = 2.0;
				viz.file = data.compute(CycleHighwayAnalysis.class, "cycle_highway_agents_trip_start_end.csv");
				viz.projection = CRS;
				viz.addAggregation("trip destinations", "person", "end_x", "end_y");
			})
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Cycle highways";
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.height = 7.5;
				viz.width = 2.0;
				viz.setShape(data.compute(CycleHighwayAnalysis.class, "cycle_highways.shp"), "id");
			});

		layout.row("volumes")
			.el(MapPlot.class, (viz, data) -> {

				viz.title = "Simulated traffic volume by bike";
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.height = 7.5;
				viz.width = 2.0;
				viz.setShape(data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties", "--mode-filter", "car,freight,drt,bike"), "id");
				viz.addDataset(TRAFFIC, data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv", "--transport-modes" , "car,bike,freight"));

				viz.display.lineColor.dataset = TRAFFIC;
				viz.display.lineColor.columnName = "vol_bike";
				viz.display.lineColor.join = "link_id";
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, true);

				viz.display.lineWidth.dataset = TRAFFIC;
				viz.display.lineWidth.columnName = "vol_bike";
				viz.display.lineWidth.scaleFactor = 20000d;
				viz.display.lineWidth.join = "link_id";

			});

		createIncomeLayouts(layout, args);

	}

	private static void createIncomeLayouts(Layout layout, String[] args) {
		layout.row("income")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Bike users per income group - Policy";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "bike_income_groups.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Bike users per income group - Policy";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "bike_income_groups.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = ABSOLUTE;
				viz.columns = List.of(ABSOLUTE);
			});

		layout.row("incomeBase")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Bike users per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "bike_income_groups_base.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Bike users per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "bike_income_groups_base.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = ABSOLUTE;
				viz.columns = List.of(ABSOLUTE);
			});

		layout.row("incomeBaseLeipzig")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "allModes_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents (bike) per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "bike_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents (car) per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "car_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			});

		layout.row("incomeBaseLeipzig2")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents (pt) per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "pt_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents (walk) per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "walk_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			})
			.el(Bar.class, (viz, data) -> {
				viz.title = "Leipzig residents (ride) per income group - Base";
				viz.stacked = false;
				viz.dataset = data.compute(CycleHighwayAnalysis.class, "ride_income_groups_base_leipzig.csv", args);
				viz.x = INCOME_GROUP;
				viz.xAxisName = INCOME_GROUP;
				viz.yAxisName = SHARE;
				viz.columns = List.of(SHARE);
			});
	}
}
