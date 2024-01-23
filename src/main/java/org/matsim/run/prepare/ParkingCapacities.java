package org.matsim.run.prepare;

import org.matsim.analysis.ParkingLocation;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParkingCapacities {


	public static void main(String[] args) throws IOException {

		Network network = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.2/input/leipzig-v1.2-network-with-pt.xml.gz");
		List<ParkingCapacities.ParkingCapacityRecord> listOfParkingCapacities = new ArrayList<>();

		for (Link l: network.getLinks().values()) {

			//skip motorways and nn car links
			if (l.getAllowedModes().contains(TransportMode.car) && l.getFreespeed()> 55/3.6) {
				double usableLength = (l.getLength() - 10) * 0.9;

				double capacity =0;
				if (usableLength > 0) {
					capacity = usableLength / 6;
				}

				l.getAttributes().putAttribute("parkingCapacity", (int) Math.floor(capacity));
				listOfParkingCapacities.add(new ParkingCapacityRecord(l.getId().toString(), (int) Math.floor(capacity)));
			}
		}
		writeResults(Path.of("../"), listOfParkingCapacities);
		NetworkUtils.writeNetwork(network, "networkWithParkingCap.xml.gz");
	}


	private static void writeResults(Path outputFolder, List<ParkingCapacities.ParkingCapacityRecord> listOfParkingCapacities) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.resolve("parkingCapacities.tsv").toString());
		writer.write("linkId\tcapacity");
		writer.newLine();

		for (ParkingCapacities.ParkingCapacityRecord pd : listOfParkingCapacities) {
			writer.write(pd.linkId + "\t" + pd.capacity);
			writer.newLine();
		}
		writer.close();

	}

	 private record ParkingCapacityRecord(String linkId, int capacity) { }
}
