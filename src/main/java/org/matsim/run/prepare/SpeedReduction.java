package org.matsim.run.prepare;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides a method to reduce the speed of the network for a given geometry.
 * This can be done relativly or tempo 20 on every link
 * @deprecated use {@link PrepareNetwork} instead
 */
@Deprecated(since="04-23")
public final class SpeedReduction {

	private static final Logger log = LogManager.getLogger(SpeedReduction.class);

	private SpeedReduction() {}


	/**
	 * See {@link SpeedReduction}.
	 */
	public static void implementPushMeasuresByModifyingNetworkInArea(Network network, List<PreparedGeometry> geometries, Double relativeSpeedChange) {
		Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
				//filter car links
				.filter(link -> link.getAllowedModes().contains(TransportMode.car))
				//spatial filter
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
				//we won't change motorways and motorway_links
				.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
				.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("trunk"))
				.collect(Collectors.toSet());

		if (relativeSpeedChange >= 0.0 && relativeSpeedChange < 1.0) {
			log.info("reduce speed relatively by a factor of: {}", relativeSpeedChange);

			//apply 'tempo 20' to all roads but motorways
			carLinksInArea.forEach(link -> link.setFreespeed(link.getFreespeed() * relativeSpeedChange));

		} else {
			log.info("reduce speed to 20 km/h");
			carLinksInArea.forEach(link -> {
				//apply 'tempo 20' to all roads but motorways
				//20 km/h --> 5.5 m/s
				link.setFreespeed(5.5);
			});
		}
	}

}
