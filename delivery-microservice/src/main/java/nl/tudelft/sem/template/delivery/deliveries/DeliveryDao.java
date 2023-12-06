package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;

import java.util.Optional;
import java.util.UUID;

public class DeliveryDao {

    private final DeliveryRepository deliveryRepository;

    public DeliveryDao(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
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

    public static class DeliveryNotFoundException extends RuntimeException {}
}
