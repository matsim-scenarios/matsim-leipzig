package org.matsim.run.prepare;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.run.LeipzigUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Attach parking information to links.
 */
@Deprecated
public final class ParkingCapacitiesAttacher {

	private static final Logger log = LogManager.getLogger(ParkingCapacitiesAttacher.class);

	Network network;
	private final ShpOptions shp;
	Path inputParkingCapacities;
	private int adaptedLinksCount = 0;
	private int networkLinksCount = 0;

	ParkingCapacitiesAttacher(Network network, ShpOptions shp, Path inputParkingCapacities) {
		this.network = network;
		this.shp = shp;
		this.inputParkingCapacities = inputParkingCapacities;
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


			if (isInsideParkingArea && linkParkingCapacities.get(link.getId().toString()) != null) {
				double parkingCapacity = Double.parseDouble(linkParkingCapacities.get(link.getId().toString()));

				LeipzigUtils.setParkingCapacity(link, parkingCapacity);

				adaptedLinksCount++;
			}
		}
		log.log(Level.INFO, "%d of %d links were complemented with parking information attribute.", adaptedLinksCount, networkLinksCount);
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
