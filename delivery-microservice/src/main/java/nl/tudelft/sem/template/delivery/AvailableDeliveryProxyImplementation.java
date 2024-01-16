package nl.tudelft.sem.template.delivery;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvailableDeliveryProxyImplementation implements AvailableDeliveryProxy {

    private final transient DeliveryService deliveryService;

    private transient Queue<UUID> availableDeliveries = new LinkedList<>();

    /**
     * Constructor for Proxy design pattern that keeps track of available deliveries.
     *
     * @param deliveryService delivery service (for access to the Delivery database)
     */
    @Autowired
    public AvailableDeliveryProxyImplementation(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    /**
     * Check if delivery is available for courier.
     *
     * @param d delivery to be added/updated
     * @return return boolean value indicating whether it's available
     */
    public boolean checkStatus(Delivery d) {
        DeliveryStatus status = d.getStatus();
        if (status == null || isNullOrEmpty(d.getRestaurantID()) || !isNullOrEmpty(d.getCourierID())) {
            return false;
        }
        boolean ownCouriers = deliveryService.restaurantUsesOwnCouriers(d);
        if (ownCouriers) {
            return false;
        } else {
            return status.equals(DeliveryStatus.ACCEPTED) || status.equals(DeliveryStatus.PREPARING);
        }
    }

    /**
     * Checks if string is null, empty or only contains whitespace.
     *
     * @param str string being checked
     * @return boolean indicating if string is null/empty/contains only whitespace
     */
    boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank() || str.isEmpty();
    }

    /**
     * Check if delivery is available for couriers.
     *
     * @param delivery delivery to be added/updated
     */
    public void insertDelivery(Delivery delivery) {
        UUID deliveryId = delivery.getDeliveryID();
        boolean isAvailable = checkStatus(delivery);
        if (isAvailable && !availableDeliveries.contains(deliveryId)) {
            availableDeliveries.offer(deliveryId);
        }
        if (!isAvailable) {
            availableDeliveries.remove(deliveryId);
        }
    }

    /**
     * Update and return queue of available delivery ids.
     *
     * @return updated queue of delivery ids
     */
    public UUID getAvailableDeliveryId() {
        Delivery delivery;
        while (!availableDeliveries.isEmpty()) {
            UUID deliveryId = availableDeliveries.poll();
            delivery = deliveryService.getDelivery(deliveryId);
            if (checkStatus(delivery)) {
                return deliveryId;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There are no available deliveries");
    }
}
