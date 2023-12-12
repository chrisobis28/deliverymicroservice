package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class StatisticsService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    public Integer getOrderRating(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(x -> x.getRatingRestaurant()).orElseThrow(DeliveryService.DeliveryNotFoundException::new);
    }
}
