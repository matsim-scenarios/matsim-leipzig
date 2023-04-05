package org.matsim.run;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Simon Meinhardt (simei94)
 */

public class CarfreeMultiModalLinkChooserTest {

    @Test
    public void testLinkChooserMultimodalNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.car);
        modes.add(TransportMode.bike);

        Network network = createAndAddNetwork(scenario, modes);
        Link carLink = network.getLinks().get(Id.createLinkId("1_" + TransportMode.car));

        ActivityFacilitiesFactory fac = new ActivityFacilitiesFactoryImpl();
        ActivityFacility facility = fac.createActivityFacility(Id.create("fac1", ActivityFacility.class), new Coord((double) 0, (double) 0),
                carLink.getId());

        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
        Network bikeOnlyNetwork = NetworkUtils.createNetwork();
        Set<String> modesToFilter = new HashSet<>();
        modesToFilter.add(TransportMode.bike);
        filter.filter(bikeOnlyNetwork, modesToFilter);

        MultimodalLinkChooser linkChooser = new CarfreeMultimodalLinkChooser();
        Link bikeLink = linkChooser.decideOnLink(facility, bikeOnlyNetwork);

        Assert.assertFalse(carLink.getId() == bikeLink.getId());
    }

    @Test
    public void testLinkChooserSinglemodalNetwork() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.car);
        modes.add(TransportMode.bike);

        Network network = createAndAddNetwork(scenario, modes);
        Link carLink = network.getLinks().get(Id.createLinkId("1_" + TransportMode.car));

        ActivityFacilitiesFactory fac = new ActivityFacilitiesFactoryImpl();
        ActivityFacility facility = fac.createActivityFacility(Id.create("fac1", ActivityFacility.class), new Coord((double) 0, (double) 0),
                carLink.getId());

        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
        Network carOnlyNetwork = NetworkUtils.createNetwork();
        Set<String> modesToFilter = new HashSet<>();
        modesToFilter.add(TransportMode.car);
        filter.filter(carOnlyNetwork, modesToFilter);

        MultimodalLinkChooser linkChooser = new CarfreeMultimodalLinkChooser();
        Link sameLink = linkChooser.decideOnLink(facility, carOnlyNetwork);

        Assert.assertTrue(carLink.getId() == sameLink.getId());
    }

    private Network createAndAddNetwork(Scenario sc, Set<String> modes) {
        Network net = sc.getNetwork();
        Link l1;
        {
            NetworkFactory nf = net.getFactory();
            Set<String> allowedModes = new HashSet<>();
            Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
            Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
            net.addNode(n1);
            net.addNode(n2);

            for(String mode : modes) {
                l1 = nf.createLink(Id.createLinkId("1_" + mode), n1, n2);
                allowedModes.add(mode);
                l1.setAllowedModes(allowedModes);
                net.addLink(l1);
                allowedModes.clear();
            }
        }
        return net;
    }
}
