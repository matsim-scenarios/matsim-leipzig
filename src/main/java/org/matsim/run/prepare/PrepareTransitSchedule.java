package org.matsim.run.prepare;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.*;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(
		name = "prepare-transit-schedule",
		description = "Tag transit stops for Intermodal trips"
)
public class PrepareTransitSchedule implements MATSimAppCommand {

	private final Map<Id<TransitStopFacility>, TransitStopFacility> consideredStops = new HashMap<>();

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--input", description = "input transit schedule", required = true)
	private String input;

	@CommandLine.Option(names = "--filter-railways", description = "Filter for using railbound transit lines only")
	private boolean railwaysOnly;

	@CommandLine.Option(names = "--output", description = "output path of the transit schedule", required = true)
	private String output;

	public static void main(String[] args) {
		new PrepareTransitSchedule().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(input);
		config.global().setCoordinateSystem("EPSG:25832");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();

		prepareDrtIntermodality(transitSchedule, shp, railwaysOnly);

		TransitScheduleWriter writer = new TransitScheduleWriter(transitSchedule);
		writer.writeFile(output);

		return 0;
	}

	/**
	 * method for adapting an existing TransitSchedule such that transit stops are available for intermodality between.
	 * public transit and the modelled drt mode(s).
	 * @param transitSchedule transitSchedule loaded from scenario.
	 * @param shp shp file of drt service area
	 */
	public void prepareDrtIntermodality(TransitSchedule transitSchedule, ShpOptions shp, boolean railwaysOnly) {

		Geometry intermodalArea = null;
		List<SimpleFeature> features = shp.readFeatures();
		for (SimpleFeature feature : features) {
			if (intermodalArea == null) {
				intermodalArea = (Geometry) feature.getDefaultGeometry();
			} else {
				intermodalArea = intermodalArea.union((Geometry) feature.getDefaultGeometry());
			}
		}

//        Geometry intermodalArea = shp.getGeometry();

		if (railwaysOnly) {
			filterRailboundTransitLines(transitSchedule);
		} else {
			consideredStops.putAll(transitSchedule.getFacilities());
		}


		for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
			if (consideredStops.containsKey(stop.getId())) {
				if (MGC.coord2Point(stop.getCoord()).within(intermodalArea)) {
					stop.getAttributes().putAttribute("allowDrtAccessEgress", "true");
				}
			}
		}

		ProjectionUtils.putCRS(transitSchedule, "EPSG:25832");

	}

	private void filterRailboundTransitLines(TransitSchedule transitSchedule) {
		//TransitSchedule IDs:
		//Halle: HAT, HAB
		//Naumburg: BLK, NTB
		//Regionalbusse: RL (Regionalbus), MQ, AW, GEISS, VRB, LEU, THU, OBS, OVH, RVT, DBG
		//Leipzig: LVTRAM, LVBUS, EB (Regionalzug), RB (Regionalzug), RE (Regionalzug), S1-9
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			Pattern pattern = Pattern.compile("LVTRAM|EB|RB|RE|S1|S2|S3|S4|S5|S6|S7|S8|S9");
			Matcher matcher = pattern.matcher(line.getId().toString());
			boolean matchFound = matcher.find();

			if (matchFound) {
				for (TransitRoute route : line.getRoutes().values()) {
					for (TransitRouteStop stop : route.getStops()) {
						consideredStops.putIfAbsent(stop.getStopFacility().getId(), stop.getStopFacility());
					}
				}
			}
		}
	}
}
