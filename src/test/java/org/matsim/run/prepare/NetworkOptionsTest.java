package org.matsim.run.prepare;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.RunLeipzigScenario;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.io.BufferedWriter;
import java.io.IOException;

public class NetworkOptionsTest {

    private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";
    private static final String shpPath = "./input/v1.2/drtServiceArea/preliminary-serviceArea-leipzig-utm32n.shp";
	Config config = ConfigUtils.createConfig();

    @Test
    public void runDrtAreaCreationTest() {

        String output = "test/output/networkOptionsTest/drtArea";
        String outputNetworkPath = "/leipzig-v1.1-network-with-drt.xml.gz";

		config.controler().setOutputDirectory(output);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--drt-area", shpPath, "--output", output  + outputNetworkPath);

        Network outputNetwork = NetworkUtils.readNetwork(output + outputNetworkPath);
		Assert.assertTrue(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAllowedModes().contains(TransportMode.drt));
		Assert.assertTrue(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAllowedModes().contains(TransportMode.drt));
    }

    @Test
    public void runCarFreeAreaCreationTest() {

        String output = "test/output/networkOptionsTest/carFreeArea";
        String outputNetworkPath = "/leipzig-v1.1-network-carFreeArea.xml.gz";

		config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--car-free-area", shpPath, "--output", output  + outputNetworkPath);

		Network outputNetwork = NetworkUtils.readNetwork(output + outputNetworkPath);

		Assert.assertFalse(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAllowedModes().contains(TransportMode.car));
		Assert.assertFalse(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAllowedModes().contains(TransportMode.car));
    }

    @Test
    public void runParkingCapacitiesAreaCreationTest() {

        String output = "test/output/networkOptionsTest/parkingCapacitiesArea";
        String outputNetworkPath = "/leipzig-v1.1-network-parkingCapacitiesArea.xml.gz";

        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

		int capacity = 1;

        String parkingCapacities = createParkingCapacityData(output, capacity);

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--parking-capacities-area", shpPath, "--parking-capacities-input", parkingCapacities, "--output", output  + outputNetworkPath);

		Network outputNetwork = NetworkUtils.readNetwork(output + outputNetworkPath);
		Assert.assertTrue(Integer.parseInt(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAttributes().getAttribute("parkingCapacity").toString())==capacity);
		Assert.assertTrue(Integer.parseInt(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAttributes().getAttribute("parkingCapacity").toString())==capacity);
	}

	@Test
	public void runParkingCostAreaCreationTest() {

		String output = "test/output/networkOptionsTest/parkingCostArea";
		String outputNetworkPath = "/leipzig-v1.1-network-parkingCostArea.xml.gz";

		config.controler().setOutputDirectory(output);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();

		MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
				"--parking-cost-area", shpPath, "--output", output  + outputNetworkPath);

		ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);

		Network outputNetwork = NetworkUtils.readNetwork(output + outputNetworkPath);

		//parkingCost values (2.0 and 0.1) are defined in file shpPath
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()).toString())==2.0);
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()).toString())==2.0);
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName()).toString())==0.1);
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()).toString())==2.0);
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()).toString())==2.0);
		Assert.assertTrue(Double.parseDouble(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName()).toString())==0.1);

	}

	@Test
	public void runSlowSpeedAreaCreationTest() {

		String output = "test/output/networkOptionsTest/slowSpeedArea";
		String outputNetworkPath = "/leipzig-v1.1-network-with-slowSpeed-relative.xml.gz";

		config.controler().setOutputDirectory(output);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.network().setInputFile(URL + "leipzig-v1.1-network-with-pt.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();

		//relative
		String relativeSpeedChange = "0.5";

		MATSimApplication.execute(RunLeipzigScenario.class, scenario.getConfig(), "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
				"--slow-speed-area", shpPath, "--slow-speed-relative-change", relativeSpeedChange, "--output", output  + outputNetworkPath);

		Network outputNetwork = NetworkUtils.readNetwork(output + outputNetworkPath);

		Assert.assertTrue(outputNetwork.getLinks().get(Id.createLinkId("-10424519")).getFreespeed()
				==12.501000000000001*Double.parseDouble(relativeSpeedChange));
		Assert.assertTrue(outputNetwork.getLinks().get(Id.createLinkId("827435967#0")).getFreespeed()
				==12.501000000000001*Double.parseDouble(relativeSpeedChange));

	}

    private String createParkingCapacityData(String output, int capacity) {
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
