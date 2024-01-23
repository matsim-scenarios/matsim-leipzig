package org.matsim.run.prepare;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

public class ParkingCapacities implements MATSimAppCommand {


	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private static String networkFile;

	@CommandLine.Option(names = "--output", description = "Output path of the prepared network", required = true)
	private String outputPath;

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork(networkFile);

		for (Link l: network.getLinks().values()) {
			double useAbleLength = (l.getLength() - 10) * 0.9;

			double capacity =0;
			if (useAbleLength > 0) {
				capacity = useAbleLength / 6;
			}

			l.getAttributes().putAttribute("parkingCapacity", capacity);


		}





	}

	@Override
	public Integer call() throws Exception {
		return null;
	}


	 Record parkingCapacity(String linkId, int capacity) {
		return null;
	}
}
