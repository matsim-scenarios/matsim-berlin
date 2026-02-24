package org.matsim.run.policies.autofrei;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.MultimodalLinkChooserDefaultImpl;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

public class MyMultimodalLinkChooser implements MultimodalLinkChooser {
	private static final Logger log = LogManager.getLogger(MyMultimodalLinkChooser.class);

	@Inject
	MultimodalLinkChooserDefaultImpl delegate;

	@Override
	public Link decideAccessLink(RoutingRequest routingRequest, String mode, Network network) {
		if (mode.equals("bike")) {
			return decideOnLink(routingRequest.getFromFacility(), network);
		}
		return delegate.decideAccessLink(routingRequest, mode, network);
	}

	@Override
	public Link decideEgressLink(RoutingRequest routingRequest, String mode, Network network) {
		if (mode.equals("bike")) {
			return decideOnLink(routingRequest.getToFacility(), network);
		}
		return delegate.decideEgressLink(routingRequest, mode, network);
	}

	private Link decideOnLink(Facility facility, Network network) {
		Link accessActLink = null;
//		Id<Link> accessActLinkId = null;

//		try {
//			accessActLinkId = facility.getLinkId();
//		} catch (Exception var8) {
//		}

//		if (accessActLinkId != null) {
//			accessActLink = (Link) network.getLinks().get(facility.getLinkId());
//		}

//		if (accessActLink == null) {
		if (facility.getCoord() == null) {
			throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given");
		}

		accessActLink = NetworkUtils.getNearestLink(network, facility.getCoord());
		if (accessActLink == null) {
			log.warn("Facility without link for which no nearest link on the respective network could be found. About to abort. Writing out the first 10 links to understand which subnetwork was used to help debugging.");
			int ii = 0;
			for (Link link : network.getLinks().values()) {
				if (ii == 10) {
					break;
				}
				++ii;
				log.warn(link);
			}
		}

		Gbl.assertNotNull(accessActLink);
//		}

		return accessActLink;
	}
}
