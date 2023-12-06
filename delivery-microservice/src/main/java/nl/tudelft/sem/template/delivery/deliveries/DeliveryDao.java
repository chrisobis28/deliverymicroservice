package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryDao {

    private final DeliveryRepository deliveryRepository;

    public DeliveryDao(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public Delivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
    }

    public DeliveryStatus getDeliveryStatus(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getStatus).orElseThrow(DeliveryNotFoundException::new);
    }

    public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus deliveryStatus) {
        Optional<Delivery> deliveryOptional = deliveryRepository.findById(deliveryId);
        if (deliveryOptional.isEmpty()) throw new DeliveryNotFoundException();

        Delivery delivery = deliveryOptional.get();
        delivery.setStatus(deliveryStatus);
        deliveryRepository.save(delivery);
    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Delivery entity with specified id not found")
    public static class DeliveryNotFoundException extends RuntimeException {}
}
