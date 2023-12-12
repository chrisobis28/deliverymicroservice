package nl.tudelft.sem.template.delivery.services;

import java.util.List;
import java.util.stream.Collectors;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;
    private RestaurantRepository restaurantRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, RestaurantRepository restaurantRepository) {
        this.deliveryRepository = deliveryRepository;
        this.restaurantRepository = restaurantRepository;
    }
    public Delivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
    }
    public Delivery insert(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException();
        }
        return deliveryRepository.save(delivery);
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
    /**
     * Function that returns the address where the food needs to be delivered
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getDeliveryAddress(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getDeliveryAddress).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Function that returns the address where the food needs to be picked up
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getPickupLocation(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);

        if (delivery.isEmpty()) {
            throw new DeliveryNotFoundException();
        }

        Optional<Restaurant> restaurant = restaurantRepository.findById(delivery.get().getRestaurantID());

        return restaurant.map(Restaurant::getLocation).orElseThrow(() -> new DeliveryNotFoundException());
    }

    /**
     * Exception to be used when a Delivery entity with a given ID is not found.
     */
    static public class DeliveryNotFoundException extends ResponseStatusException {
        public DeliveryNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Delivery with specified id not found");
        }
    }

    public List<Delivery> getAcceptedDeliveries(){
        return deliveryRepository.findAll().stream()
            .filter(delivery -> delivery.getCourierID() == null).collect(Collectors.toList());
    }

    public void updateDeliveryCourier(UUID deliveryId, String courierId){
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setCourierID(courierId);
        deliveryRepository.save(delivery);
    }

}
