package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateService {

    private final transient DeliveryRepository deliveryRepository;

    /**
    * Constructor.
    *
    * @param deliveryRepository delivery repository
    */
    public UpdateService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Given a new courier ID, assign it to a given delivery.
     *
     * @param deliveryId ID of a delivery to be updated
     * @param courierId ID of a courier to be assigned
     */
    public Delivery updateDeliveryCourier(UUID deliveryId, String courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setCourierID(courierId);
        return deliveryRepository.save(delivery);
    }

    /**
     * Update the rating of a restaurant.
     *
     * @param deliveryId the delivery to be updated
     * @param rating new rating of restaurant
     */
    public Delivery updateRestaurantRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setRatingRestaurant(rating);
        return deliveryRepository.save(delivery);
    }

    /**
     * Update the rating of a courier.
     *
     * @param deliveryId delivery to update
     * @param rating new rating score
     */
    public Delivery updateCourierRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setRatingCourier(rating);
        return deliveryRepository.save(delivery);
    }

    /**
     * Updates the delivery status.
     *
     * @param deliveryId     a delivery to update the status of
     * @param deliveryStatus new status
     */
    public Delivery updateDeliveryStatus(UUID deliveryId, DeliveryStatus deliveryStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setStatus(deliveryStatus);

        if (delivery.getStatus().equals(DeliveryStatus.DELIVERED)) {
            delivery.setDeliveredTime(OffsetDateTime.now());
        }
        return deliveryRepository.save(delivery);
    }

    /**
     * Update the delivery address of an order.
     *
     * @param deliveryId ID of delivery to be updated
     * @param newAddress the new address
     */
    public Delivery updateDeliveryAddress(UUID deliveryId, List<Double> newAddress) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setDeliveryAddress(new ArrayList<>(newAddress));
        return deliveryRepository.save(delivery);
    }
}
