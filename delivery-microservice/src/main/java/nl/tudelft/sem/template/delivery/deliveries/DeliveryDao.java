package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryDao {

    @Autowired
    private DeliveryRepository deliveryRepository;

    public Delivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
    }

    public DeliveryStatus getDeliveryStatus(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getStatus).orElseThrow(DeliveryNotFoundException::new);
    }

    public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus deliveryStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setStatus(deliveryStatus);
        deliveryRepository.save(delivery);
    }

    public Integer getOrderRating(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(x -> x.getRatingRestaurant()).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Exception to be used when a Delivery entity with a given ID is not found.
     */
    static public class DeliveryNotFoundException extends ResponseStatusException {
        public DeliveryNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Delivery with specified id not found");
        }
    }
}
