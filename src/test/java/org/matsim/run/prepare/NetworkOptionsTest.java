package org.matsim.run.prepare;

import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.io.BufferedWriter;
import java.io.IOException;

public class NetworkOptionsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

    private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";
    private static final String shpPath = "./input/v1.2/drtServiceArea/leipzig_flexa_service_area_2021.shp";

	private Network network;
	private NetworkOptions options;

	@Before
	public void setUp() throws Exception {
		network = NetworkUtils.readNetwork(URL + "leipzig-v1.1-network.xml.gz");
		options = new NetworkOptions();
	}

	@Test
    public void runDrtAreaCreationTest() {

		new CommandLine(options).parseArgs( "--drt-area", shpPath);
		options.prepare(network);

		Assert.assertTrue(network.getLinks().get(Id.createLinkId("-10424519")).getAllowedModes().contains(TransportMode.drt));
		Assert.assertTrue(network.getLinks().get(Id.createLinkId("827435967#0")).getAllowedModes().contains(TransportMode.drt));
    }

    @Test
    public void runCarFreeAreaCreationTest() {

		new CommandLine(options).parseArgs( "--car-free-area", shpPath);
		options.prepare(network);

		Assert.assertFalse(network.getLinks().get(Id.createLinkId("-10424519")).getAllowedModes().contains(TransportMode.car));
		Assert.assertFalse(network.getLinks().get(Id.createLinkId("827435967#0")).getAllowedModes().contains(TransportMode.car));
    }

    @Test
    public void runParkingCapacitiesAreaCreationTest() {

		double capacity = 1.;

		String parkingCapacities = createParkingCapacityData(utils.getOutputDirectory(), capacity);

		new CommandLine(options).parseArgs( "--parking-capacities-area", shpPath, "--parking-capacities-input", parkingCapacities);
		options.prepare(network);


		Link link = network.getLinks().get(Id.createLinkId("-10424519"));

		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("-10424519"))
				.getAttributes().getAttribute("parkingCapacity").toString()),capacity, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("827435967#0"))
				.getAttributes().getAttribute("parkingCapacity").toString()),capacity, 0);
	}

	@Test
	public void runParkingCostAreaCreationTest() {

		new CommandLine(options).parseArgs( "--parking-cost-area", shpPath);
		options.prepare(network);

		ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(new Config(), ParkingCostConfigGroup.class);

		//parkingCost values (2.0 and 0.1) are defined in file shpPath
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("-10424519"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()).toString()),2.0, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("-10424519"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()).toString()),2.0, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("-10424519"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName()).toString()),0.1, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("827435967#0"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()).toString()),2.0, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("827435967#0"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()).toString()),2.0, 0);
		Assert.assertEquals(Double.parseDouble(network.getLinks().get(Id.createLinkId("827435967#0"))
				.getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName()).toString()),0.1, 0);

	}

	@Test
	public void runSlowSpeedAreaCreationTest() {

		//relative
		String relativeSpeedChange = "0.5";

		new CommandLine(options).parseArgs( "--slow-speed-area", shpPath, "--slow-speed-relative-change", relativeSpeedChange);
		options.prepare(network);

		Assert.assertEquals(network.getLinks().get(Id.createLinkId("-10424519")).getFreespeed(),
				12.501000000000001*Double.parseDouble(relativeSpeedChange), 0);
		Assert.assertEquals(network.getLinks().get(Id.createLinkId("827435967#0")).getFreespeed(),
				12.501000000000001*Double.parseDouble(relativeSpeedChange), 0);

	}

    private String createParkingCapacityData(String output, double capacity) {
        Network network = NetworkUtils.readNetwork(URL + "leipzig-v1.1-network.xml.gz");

        String parkingCapacityFile = output + "/parking-capacity-per-link.tsv";

        BufferedWriter capacityWriter = IOUtils.getBufferedWriter(parkingCapacityFile);

        try {
            capacityWriter.write("linkId" + "\t" + "maxParkedVehicles");
            capacityWriter.newLine();

            for(Id<Link> linkId : network.getLinks().keySet()) {
                if (linkId.toString().contains("pt_")) {
                    continue;
                }
                capacityWriter.write(linkId + "\t" + capacity);
                capacityWriter.newLine();
            }
            capacityWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return parkingCapacityFile;
    }
}
