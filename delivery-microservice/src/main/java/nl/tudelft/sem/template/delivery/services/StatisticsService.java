package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    /**
     * Constructor
     * @param deliveryRepository the repository storing delivery information
     */
    @Autowired
    private DeliveryRepository deliveryRepository;

    public StatisticsService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public Delivery insert(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException();
        }
        return deliveryRepository.save(delivery);
    }

    /**
     * Gets the restaurant rating of a given order
     * @param deliveryId - the order/delivery id
     * @return an integer from 1 to 5 that represents the rating
     * if no rating is given, returns null
     */
    public Integer getOrderRating(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        return delivery.getRatingRestaurant();
    }

    /**
     * Get all fulfilled deliveries of a specific vendor
     * @param userId vendor's email
     * @return list of deliveries ordered by delivery time
     */
    public List<Delivery> getOrdersOfAVendor(String userId) {
        List<Delivery> vendorDeliveries = deliveryRepository.findAll().stream()
            .filter(d -> userId.equals(d.getRestaurantID())).collect(Collectors.toList());
        List<Delivery> delivered = vendorDeliveries.stream().filter(d -> d.getStatus() != null)
            .filter(d -> d.getStatus().equals(DeliveryStatus.DELIVERED)).collect(Collectors.toList());
        return delivered.stream().sorted(Comparator.comparing(Delivery::getDeliveredTime)).collect(Collectors.toList());
    }
}
