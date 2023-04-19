package org.matsim.run.prepare;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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

import java.io.BufferedWriter;
import java.io.IOException;

public class NetworkOptionsTest {

    private static final String URL = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/leipzig/leipzig-v1.1/input/";
    private static final String shpPath = "./input/v1.1/drtServiceArea/preliminary-serviceArea-leipzig-utm32n.shp";

    @Test
    public void runDrtAreaCreationTest() {

        String output = "test/output/networkOptionsTest/drtArea";
        String outputNetwork = "/leipzig-v1.1-network-with-drt.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--drt-area", shpPath, "--output", output  + outputNetwork);

        Assert.assertNotNull(NetworkUtils.readNetwork(output + outputNetwork));
    }

    @Test
    public void runCityAreaCreationTest() {

        String output = "test/output/networkOptionsTest/cityArea";
        String outputNetwork = "/leipzig-v1.1-network-cityArea.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--city-area", shpPath, "--output", output  + outputNetwork);

        Assert.assertNotNull(NetworkUtils.readNetwork(output + outputNetwork));
    }

    @Test
    public void runCarFreeAreaCreationTest() {

        String output = "test/output/networkOptionsTest/carFreeArea";
        String outputNetwork = "/leipzig-v1.1-network-carFreeArea.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--car-free-area", shpPath, "--output", output  + outputNetwork);

        Assert.assertNotNull(NetworkUtils.readNetwork(output + outputNetwork));
    }

    @Test
    public void runParkingAreaCreationTest() {

        String output = "test/output/networkOptionsTest/parkingArea";
        String outputNetwork = "/leipzig-v1.1-network-parkingArea.xml.gz";
        String outputNetwork2 = "/leipzig-v1.1-network-parkingArea-noShp.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(output);
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        controler.run();

        String parkingCapacities = createParkingCapacityData(output);

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz",
                "--parking-area", shpPath, "--parking-capacities", parkingCapacities, "--output", output  + outputNetwork);

        MATSimApplication.execute(RunLeipzigScenario.class, config, "prepare", "network", "--network", URL + "leipzig-v1.1-network.xml.gz", "--parking-capacities", parkingCapacities, "--output", output  + outputNetwork2);

        Assert.assertNotNull(NetworkUtils.readNetwork(output + outputNetwork));
        Assert.assertNotNull(NetworkUtils.readNetwork(output + outputNetwork2));
    }

    private String createParkingCapacityData(String output) {
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
                capacityWriter.write(linkId + "\t" + 1);
                capacityWriter.newLine();
            }
            capacityWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return parkingCapacityFile;
    }
}
