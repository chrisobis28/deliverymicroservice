package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.model.Delivery;

import java.util.UUID;

public interface AvailableDeliveryProxy {

    UUID getAvailableDeliveryId();

    void insertDelivery(Delivery delivery);
}
