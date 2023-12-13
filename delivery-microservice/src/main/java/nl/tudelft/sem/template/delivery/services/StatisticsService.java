package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class StatisticsService {

    /**
     * Constructor
     * @param deliveryRepository the repository storing delivery information
     */
    @Autowired
    private DeliveryRepository deliveryRepository;

    /**
     * Gets the restaurant rating of a given order
     * @param deliveryId - the order/delivery id
     * @return an integer from 1 to 5 that represents the rating
     * if no rating is given, returns null
     */
    public Integer getOrderRating(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(x -> x.getRatingRestaurant()).orElseThrow(DeliveryService.DeliveryNotFoundException::new);
    }
}
