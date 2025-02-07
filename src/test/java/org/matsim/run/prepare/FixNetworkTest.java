package org.matsim.run.prepare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.*;


public class FixNetworkTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";

	@Test
	public void runFixNetworkTest() {
		String output = "/leipzig-v1.1-network.xml.gz";

		new FixNetwork().execute("--output", utils.getOutputDirectory() + output, URL + "leipzig-v1.1-network.xml.gz");

		Network outputNetwork = NetworkUtils.readNetwork(utils.getOutputDirectory() + output);

		Link bridgeLink1 = outputNetwork.getLinks().get(Id.createLinkId("24020319"));

		assertNotNull(bridgeLink1);
		assertEquals("260443657", bridgeLink1.getFromNode().getId().toString(),  "From node");
		assertEquals( "206313940", bridgeLink1.getToNode().getId().toString(),"To node");
		assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.car));
		assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.bike));
		assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.ride));
		assertTrue(bridgeLink1.getAllowedModes().contains("freight"));
		assertEquals(bridgeLink1.getLength(), 62.47, 0.);
		assertEquals(bridgeLink1.getCapacity(), 1500, 0.);
		assertEquals(bridgeLink1.getFreespeed(), 12.50, 0.);
		assertEquals(bridgeLink1.getNumberOfLanes(), 1, 0.);
		assertEquals((double) bridgeLink1.getAttributes().getAttribute("allowed_speed"), 13.89, 0.);
		assertEquals(bridgeLink1.getAttributes().getAttribute("type"), "highway.primary");
		assertEquals(bridgeLink1.getAttributes().getAttribute("name"), "Richard-Lehmann-Straße");

		Link bridgeLink2 = outputNetwork.getLinks().get(Id.createLinkId("-24020319"));

		assertNotNull(bridgeLink2);
		assertEquals("206313940",  bridgeLink2.getFromNode().getId().toString(), "From node");
		assertEquals("260443657",  bridgeLink2.getToNode().getId().toString(), "To node");
		assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.car));
		assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.bike));
		assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.ride));
		assertTrue(bridgeLink2.getAllowedModes().contains("freight"));
		assertEquals(bridgeLink2.getLength(), 62.47, 0.);
		assertEquals(bridgeLink2.getCapacity(), 1500, 0.);
		assertEquals(bridgeLink2.getFreespeed(), 12.50, 0.);
		assertEquals(bridgeLink2.getNumberOfLanes(), 1, 0.);
		assertEquals((double) bridgeLink2.getAttributes().getAttribute("allowed_speed"), 13.89, 0.);
		assertEquals(bridgeLink2.getAttributes().getAttribute("type"), "highway.primary");
		assertEquals(bridgeLink2.getAttributes().getAttribute("name"), "Richard-Lehmann-Straße");
	}
}
