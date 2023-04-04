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

public class SpeedReduction {

    private static final Logger log = LogManager.getLogger(SpeedReduction.class);

    public static void implementPushMeasuresByModifyingNetworkInArea(Network network, List<PreparedGeometry> geometries, Double relativeSpeedChange) {
        Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
//                .filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
                //we have to filter for bike here! otherwise the speed reduction will not work for car free areas. -sm0423
                .filter(link -> link.getAllowedModes().contains(TransportMode.bike))
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) //spatial filter
                .filter(link -> ! ((String)link.getAttributes().getAttribute("type")).contains("motorway") )//we won't change motorways and motorway_links
                .filter(link -> !((String)link.getAttributes().getAttribute("type")).contains("trunk") )
                .collect(Collectors.toSet());

        if (relativeSpeedChange >= 0.0 && relativeSpeedChange < 1.0) {
            log.info("reduce speed relatively by a factor of: " + relativeSpeedChange);
            carLinksInArea.forEach(link -> {
                //apply 'tempo 20' to all roads but motorways
                link.setFreespeed(link.getFreespeed() * relativeSpeedChange);
            });

        } else {
            log.info("reduce speed to 20 km/h");
            carLinksInArea.forEach(link -> {
                //apply 'tempo 20' to all roads but motorways
                link.setFreespeed(5.5); //20 km/h
            });
        }
    }

}