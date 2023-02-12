package org.matsim.run.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Attach parking information to links.
 */
public final class ParkingNetworkWriter {

	private static final Logger log = LogManager.getLogger(ParkingNetworkWriter.class);

	Network network;
	private final ShpOptions shp;
	Path inputParkingCapacities;
	private static int adaptedLinksCount = 0;
	private static int networkLinksCount = 0;
	private static double firstHourParkingCost;
	private static double extraHourParkingCost;

	ParkingNetworkWriter(Network network, ShpOptions shp, Path inputParkingCapacities, Double firstHourParkingCost, Double extraHourParkingCost) {
		this.network = network;
		this.shp = shp;
		this.inputParkingCapacities = inputParkingCapacities;
		ParkingNetworkWriter.firstHourParkingCost = firstHourParkingCost;
		ParkingNetworkWriter.extraHourParkingCost = extraHourParkingCost;
	}

	public void addParkingInformationToLinks() {
		Map<String, String> linkParkingCapacities = getLinkParkingCapacities();

		Geometry parkingArea = null;

		if (shp.isDefined()) {
			parkingArea = shp.getGeometry();
		}

		GeometryFactory gf = new GeometryFactory();

		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("pt_")) {
				continue;
			}
			networkLinksCount++;

			LineString line = gf.createLineString(new Coordinate[]{
					MGC.coord2Coordinate(link.getFromNode().getCoord()),
					MGC.coord2Coordinate(link.getToNode().getCoord())
			});

			boolean isInsideParkingArea;

			if (parkingArea != null) {
				isInsideParkingArea = line.intersects(parkingArea);
			} else {
				isInsideParkingArea = true;
			}


			if (isInsideParkingArea) {
				if (linkParkingCapacities.get(link.getId().toString()) != null) {
					int parkingCapacity = Integer.parseInt(linkParkingCapacities.get(link.getId().toString()));

					Attributes linkAttributes = link.getAttributes();
					linkAttributes.putAttribute("parkingCapacity", parkingCapacity);

                    //TODO maybe it would be better to have a csv file with parking cost per link here instead of a fixed value -sm0123
                    // Parking cost are now defined by the shp file already, if link is inside our defined parking area, but has no parking cost we set them to zero to increase them later gr 1802
                    ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(new Config(), ParkingCostConfigGroup.class);
                    if (link.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()).equals(null)) {
                        linkAttributes.putAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName(), 0.0);
                        linkAttributes.putAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName(), 0.0);
                    }

                    adaptedLinksCount++;
                }
            }
        }
        log.info(adaptedLinksCount + " / " + networkLinksCount + " were complemented with parking information attribute.");
    }

	private Map<String, String> getLinkParkingCapacities() {
		Map<String, String> linkParkingCapacities = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(inputParkingCapacities.toString()))) {
			String lineEntry;
			while ((lineEntry = reader.readLine()) != null) {

				linkParkingCapacities.putIfAbsent(lineEntry.split("\t")[0], lineEntry.split("\t")[1]);
			}

		} catch (IOException e) {
			log.error(e);
		}
		return linkParkingCapacities;
	}
}
