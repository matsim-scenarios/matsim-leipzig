package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.facilities.Facility;

/**
 * Chooses link and respects the car fre zones.
 * @author Simon Meinhardt (simei94)
 */

public class CarfreeMultimodalLinkChooser implements MultimodalLinkChooser {

	@Override
	@SuppressWarnings("IllegalCatch")
	public Link decideOnLink(Facility facility, Network network) {

		Id<Link> originalLinkId = null;

		try {
			originalLinkId = facility.getLinkId();
		} catch (Exception ee) {
			// there are implementations that throw an exception here although "null" is, in fact, an interpretable value. kai, oct'18
		}

		if (facility.getCoord() == null) {
			throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given");
		}

		//This basically is the only difference to MultiModalLinkChooserDefaultImpl as here we are saying that we do not care about the originally
		//assigned link, we just assign it again. For most scenarios with filtered networks car = ride = bike e.g. it should always be originalLinkId = modefilteredLink.getId() -sm0622
		Link modeFilteredLink = NetworkUtils.getNearestLink(network, facility.getCoord());

		return modeFilteredLink;
	}
}
