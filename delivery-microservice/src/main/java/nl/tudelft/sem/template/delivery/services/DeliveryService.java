package nl.tudelft.sem.template.delivery.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;



/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryService {

    private final transient DeliveryRepository deliveryRepository;

    private final transient GPS gps;
    @Lazy
    private final transient RestaurantRepository restaurantRepository;

    /**
     * Constructor for DeliveryService.
     *
     * @param deliveryRepository   database for deliveries
     * @param restaurantRepository database for restaurants
     */
    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository, GPS gps, RestaurantRepository restaurantRepository) {
        this.deliveryRepository = deliveryRepository;
        this.gps = gps;
        this.restaurantRepository = restaurantRepository;
    }

    //    /**
    //     * Check if restaurant uses own couriers.
    //     *
    //     * @param delivery - Delivery being assigned
    //     * @return boolean value showing whether restaurant uses own couriers
    //     */
    //    public boolean restaurantUsesOwnCouriers(Delivery delivery) {
    //        List<String> couriers = getRestaurant(delivery.getRestaurantID()).getCouriers();
    //        return !(couriers == null || couriers.isEmpty());
    //    }

    public Delivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Adds a delivery object to the database.
     *
     * @param delivery object to be persisted
     * @return the saved object
     */
    public Delivery insert(Delivery delivery) {
        if (delivery == null || delivery.getDeliveryID() == null) {
            throw new IllegalArgumentException();
        }
        return deliveryRepository.save(delivery);
    }

    public DeliveryStatus getDeliveryStatus(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getStatus).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Function that returns the address where the food needs to be delivered.
     *
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getDeliveryAddress(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getDeliveryAddress).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Function that returns the address where the food needs to be picked up.
     *
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getPickupLocation(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        Restaurant restaurant = restaurantRepository.findById(delivery.getRestaurantID())
                .orElseThrow(RestaurantService.RestaurantNotFoundException::new);

        return restaurant.getLocation();
    }

    /**
     * Exception to be used when a Delivery entity with a given ID is not found.
     */
    static public class DeliveryNotFoundException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        public DeliveryNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Delivery with specified id not found");
        }
    }

    /**
     * Exception to be used when a Delivery entity has a status Delivered,
     * but processing methods that affect its state are being called.
     */
    static public class OrderAlreadyDeliveredException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        public OrderAlreadyDeliveredException() {
            super(HttpStatus.CONFLICT, "Delivery with specified id has already been delivered.");
        }
    }

    /**
     * Exception to be used when one wants to process a rejected order.
     */
    static public class OrderRejectedException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        public OrderRejectedException() {
            super(HttpStatus.CONFLICT, "Delivery with specified id has been rejected.");
        }
    }

    public List<Delivery> getAcceptedDeliveries() {
        return deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getCourierID() == null).collect(Collectors.toList());
    }

    /**
     * Get a restaurant by a given ID.
     *
     * @param restaurantId ID of restaurant
     * @return Restaurant entity from the repo
     */
    public Restaurant getRestaurant(String restaurantId) {
        return restaurantRepository.findById(restaurantId).orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    }

}
