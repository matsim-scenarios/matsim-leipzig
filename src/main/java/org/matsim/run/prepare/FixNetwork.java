package org.matsim.run.prepare;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@CommandLine.Command(
		name = "fix-network",
		description = "Correct network defects"
)
public class FixNetwork implements MATSimAppCommand {

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Input network")
	private List<String> input;

	@CommandLine.Option(names = "--output", description = "Output path")
	private Path output;

	public static void main(String[] args) {
		new FixNetwork().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(input.get(0));

		createSchlachthofBruecke(network, "24020319", "260443657", "206313940");
		createSchlachthofBruecke(network, "-24020319", "206313940", "260443657");

		for (Link link : network.getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				Set<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		NetworkUtils.writeNetwork(network, output.toString());

		return 0;
	}

	/**
	 * Create a link that was missing in OSM data at some point. It was later re-added (around july 2022).
	 */
	private void createSchlachthofBruecke(Network network, String linkId, String from, String to) {

		NetworkFactory f = network.getFactory();

		Link link = f.createLink(Id.createLinkId(linkId),
				network.getNodes().get(Id.createNodeId(from)),
				network.getNodes().get(Id.createNodeId(to)));

		link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike, TransportMode.ride));
		link.setLength(62.47);
		link.setCapacity(1500);
		link.setFreespeed(12.50);
		link.setNumberOfLanes(1);


		link.getAttributes().putAttribute("allowed_speed", 13.89);
		link.getAttributes().putAttribute("type", "highway.primary");
		link.getAttributes().putAttribute("name", "Richard-Lehmann-Straße");

		network.addLink(link);
	}

}
