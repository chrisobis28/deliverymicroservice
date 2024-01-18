package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

@Service
@Scope("singleton")
public class AvailableDeliveryProxyImplementation implements AvailableDeliveryProxy {

    private final transient DeliveryService deliveryService;

    private final transient Queue<UUID> availableDeliveries = new LinkedList<>();

    /**
     * Constructor for Proxy design pattern that keeps track of available deliveries.
     *
     * @param deliveryService delivery service (for access to the Delivery database)
     */
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
        if (status == null || d.getRestaurantID() == null) {
            return false;
        }
        boolean courierAssigned = d.getCourierID() != null;
        boolean restaurantUsesOwnCouriers = deliveryService.restaurantUsesOwnCouriers(d);
        if (courierAssigned || restaurantUsesOwnCouriers) {
            return false;
        }
        return status.equals(DeliveryStatus.ACCEPTED) || status.equals(DeliveryStatus.PREPARING);
    }

    /**
     * Check if delivery should be added to the queue.
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
     * Get the first available Delivery ID in the queue.
     *
     * @return UUID of first available Delivery
     */
    public UUID getAvailableDeliveryId() {

        while (!availableDeliveries.isEmpty()) {
            UUID deliveryId = availableDeliveries.poll();
            Delivery delivery = deliveryService.getDelivery(deliveryId);
            if (checkStatus(delivery)) {
                return deliveryId;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There are no available deliveries at the moment.");
    }
}
