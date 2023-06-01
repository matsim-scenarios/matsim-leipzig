package org.matsim.run.prepare;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;


public class FixNetworkTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";

	@Test
	public void runFixNetworkTest() {
		String output = "/leipzig-v1.1-network.xml.gz";

		new FixNetwork().execute("--output", utils.getOutputDirectory() + output, URL + "leipzig-v1.1-network.xml.gz");

		Network outputNetwork = NetworkUtils.readNetwork(utils.getOutputDirectory() + output);

		Link bridgeLink1 = outputNetwork.getLinks().get(Id.createLinkId("24020319"));

		Assert.assertNotNull(bridgeLink1);
		Assert.assertEquals("From node", bridgeLink1.getFromNode().getId().toString(), "260443657");
		Assert.assertEquals("To node", bridgeLink1.getToNode().getId().toString(), "206313940");
		Assert.assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.car));
		Assert.assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.bike));
		Assert.assertTrue(bridgeLink1.getAllowedModes().contains(TransportMode.ride));
		Assert.assertTrue(bridgeLink1.getAllowedModes().contains("freight"));
		Assert.assertEquals(bridgeLink1.getLength(), 62.47, 0.);
		Assert.assertEquals(bridgeLink1.getCapacity(), 1500, 0.);
		Assert.assertEquals(bridgeLink1.getFreespeed(), 12.50, 0.);
		Assert.assertEquals(bridgeLink1.getNumberOfLanes(), 1, 0.);
		Assert.assertEquals((double) bridgeLink1.getAttributes().getAttribute("allowed_speed"), 13.89, 0.);
		Assert.assertEquals(bridgeLink1.getAttributes().getAttribute("type"), "highway.primary");
		Assert.assertEquals(bridgeLink1.getAttributes().getAttribute("name"), "Richard-Lehmann-Straße");

		Link bridgeLink2 = outputNetwork.getLinks().get(Id.createLinkId("-24020319"));

		Assert.assertNotNull(bridgeLink2);
		Assert.assertEquals("From node", bridgeLink2.getFromNode().getId().toString(), "206313940");
		Assert.assertEquals("To node", bridgeLink2.getToNode().getId().toString(), "260443657");
		Assert.assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.car));
		Assert.assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.bike));
		Assert.assertTrue(bridgeLink2.getAllowedModes().contains(TransportMode.ride));
		Assert.assertTrue(bridgeLink2.getAllowedModes().contains("freight"));
		Assert.assertEquals(bridgeLink2.getLength(), 62.47, 0.);
		Assert.assertEquals(bridgeLink2.getCapacity(), 1500, 0.);
		Assert.assertEquals(bridgeLink2.getFreespeed(), 12.50, 0.);
		Assert.assertEquals(bridgeLink2.getNumberOfLanes(), 1, 0.);
		Assert.assertEquals((double) bridgeLink2.getAttributes().getAttribute("allowed_speed"), 13.89, 0.);
		Assert.assertEquals(bridgeLink2.getAttributes().getAttribute("type"), "highway.primary");
		Assert.assertEquals(bridgeLink2.getAttributes().getAttribute("name"), "Richard-Lehmann-Straße");
	}
}
