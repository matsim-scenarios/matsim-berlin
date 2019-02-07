package RunAbfall;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.TimeWindow;

public class StaticShipmentForCarrierBuilder {
	
	public static Carrier createShipments() {
		Carrier myCarrier = CarrierImpl.newInstance(Id.create("BSR", Carrier.class));

		// Shipments erstellen
		CarrierShipment shipment1 = CarrierShipment.Builder
				.newInstance(Id.create("Ship1", CarrierShipment.class), Id.createLinkId("i(1,8)"), Id.createLinkId("j(9,9)"), 10)
				.setPickupServiceTime(300).setPickupTimeWindow(TimeWindow.newInstance(21600, 54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600, 54000)).setDeliveryServiceTime(3600).build();
		CarrierShipment shipment2 = CarrierShipment.Builder
				.newInstance(Id.create("Ship2", CarrierShipment.class), Id.createLinkId("j(5,3)"), Id.createLinkId("j(9,9)"), 15)
				.setPickupServiceTime(300).setPickupTimeWindow(TimeWindow.newInstance(21600, 54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600, 54000)).setDeliveryServiceTime(3600).build();
		CarrierShipment shipment3 = CarrierShipment.Builder
				.newInstance(Id.create("Ship3", CarrierShipment.class), Id.createLinkId("j(1,5)"), Id.createLinkId("j(9,9)"), 8)
				.setPickupServiceTime(300).setPickupTimeWindow(TimeWindow.newInstance(21600, 54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600, 54000)).setDeliveryServiceTime(3600).build();
		CarrierShipment shipment4 = CarrierShipment.Builder
				.newInstance(Id.create("Ship4", CarrierShipment.class), Id.createLinkId("j(4,3)R"), Id.createLinkId("j(9,9)"), 12)
				.setPickupServiceTime(300).setPickupTimeWindow(TimeWindow.newInstance(21600, 54000))
				.setDeliveryTimeWindow(TimeWindow.newInstance(21600, 54000)).setDeliveryServiceTime(3600).build();

		myCarrier.getShipments().add(shipment1);
		myCarrier.getShipments().add(shipment2);
		myCarrier.getShipments().add(shipment3);
		myCarrier.getShipments().add(shipment4);
		return myCarrier;
	}
}
