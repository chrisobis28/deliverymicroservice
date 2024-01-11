package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class AvailableDeliveryProxy {

  private final DeliveryService deliveryService;

  Queue<UUID> available_deliveries = new LinkedList<>();
  /**
   * Constructor for Proxy design pattern that keeps track of available deliveries
   * @param deliveryService delivery service (for access to the Delivery database)
   */
  public AvailableDeliveryProxy(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  /**
   * Check if delivery is available for courier
   * @param delivery delivery to be added/updated
   * @return return boolean value indicating whether it's available
   */
  public boolean checkStatus(Delivery delivery) {
    DeliveryStatus status = delivery.getStatus();
    if (status == null || isNullOrEmpty(delivery.getRestaurantID())) return false;
    boolean ownCouriers = deliveryService.restaurantUsesOwnCouriers(delivery);
    if (ownCouriers) return false;
    else return status.equals(DeliveryStatus.ACCEPTED) || status.equals(DeliveryStatus.PREPARING);
  }

  /**
   * Checks if string is null, empty or only contains whitespace
   * @param str string being checked
   * @return boolean indicating if string is null/empty/contains only whitespace
   */
  boolean isNullOrEmpty(String str) {
    return str == null || str.isBlank() || str.isEmpty();
  }

  /**
   * Check if delivery is available for couriers
   * @param delivery delivery to be added/updated
   */
  public void checkIfAvailable(Delivery delivery) {
    UUID deliveryId = delivery.getDeliveryID();
    boolean isAvailable = checkStatus(delivery);
    if (isAvailable && !available_deliveries.contains(deliveryId)) {
      available_deliveries.offer(deliveryId);
    }
    if (!isAvailable && available_deliveries.contains(deliveryId)) {
      available_deliveries.remove(deliveryId);
    }
  }

  public Queue<UUID> getAvailableDeliveries() {
    return available_deliveries;
  }
//May not be necessary if we just call checkIfAvailable on a Delivery after its status is updated
//  /**
//   * Update queue of available deliveries.
//   */
//  public void updateQueue() {
//    List<Delivery> all = deliveryService.findAll();
//    all.forEach(delivery -> checkIfAvailable(delivery));
//  }
}
