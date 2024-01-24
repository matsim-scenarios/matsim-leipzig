package org.matsim.run.prepare;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 * This class writes out a network and a tsv file with the parking capacites of Leipzig according to a simplified calculation based on the RASt.
 */

public final class ParkingCapacities {

	private static Network network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/leipzig-v1.2-network-with-pt.xml.gz");
	private static List<ParkingCapacities.ParkingCapacityRecord> listOfParkingCapacities = new ArrayList<>();
	private ParkingCapacities() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	public static void main(String[] args) throws IOException {
		for (Link l: network.getLinks().values()) {
			//skip motorways and non car links
			if (l.getAllowedModes().contains(TransportMode.car) && l.getFreespeed() < 55/3.6) {
				double usableLength = (l.getLength() - 10) * 0.9;
				double maxCapacity = 0;
				double minCapacity = 0;
				if (usableLength > 0) {
					maxCapacity = usableLength / 6;
					minCapacity = usableLength /50;
				}

				l.getAttributes().putAttribute("maxParkingCapacity", Math.floor(maxCapacity));
				l.getAttributes().putAttribute("minParkingCapacity", Math.floor(minCapacity));
				listOfParkingCapacities.add(new ParkingCapacityRecord(l.getId().toString(), (int) Math.floor(maxCapacity), (int) Math.floor(minCapacity)));
			}
		}
		writeResults(Path.of("../"), listOfParkingCapacities);
		NetworkUtils.writeNetwork(network, "networkWithParkingCap.xml.gz");
	}


	private static void writeResults(Path outputFolder, List<ParkingCapacities.ParkingCapacityRecord> listOfParkingCapacities) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.resolve("parkingCapacities.tsv").toString());
		writer.write("linkId\tmaxCapacity\tminCapacity");
		writer.newLine();
		for (ParkingCapacities.ParkingCapacityRecord pd : listOfParkingCapacities) {
			writer.write(pd.linkId + "\t" + pd.maxCapacity + "\t" + pd.minCapacity);
			writer.newLine();
		}
		writer.close();
	}

	 private record ParkingCapacityRecord(String linkId, int maxCapacity, int minCapacity) { }


}
