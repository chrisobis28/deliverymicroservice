package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryService {

    private final transient DeliveryRepository deliveryRepository;
    @Lazy
    private final transient RestaurantRepository restaurantRepository;

    @Lazy
    private final transient ErrorRepository errorRepository;

    /**
     * Constructor for DeliveryService.
     *
     * @param deliveryRepository   database for deliveries
     * @param restaurantRepository database for restaurants
     * @param errorRepository      database for errors
     */
    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository,
                           RestaurantRepository restaurantRepository,
                           ErrorRepository errorRepository) {
        this.deliveryRepository = deliveryRepository;
        this.restaurantRepository = restaurantRepository;
        this.errorRepository = errorRepository;
    }

    /**
     * Check if restaurant uses own couriers.
     *
     * @param delivery delivery being assigned
     * @return boolean value showing whether restaurant uses own couriers
     */
    public boolean restaurantUsesOwnCouriers(Delivery delivery) {
        List<String> couriers = getRestaurant(delivery.getRestaurantID()).getCouriers();
        return !(couriers == null || couriers.isEmpty());
    }

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
        Error error = delivery.getError();
        if (error != null) {
            error.setErrorId(delivery.getDeliveryID());
            errorRepository.save(delivery.getError());
        }
        return deliveryRepository.save(delivery);
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

    /**
     * Retrieves all accepted deliveries without an assigned courier.
     *
     * @return a list of delivery objects
     */
    public List<Delivery> getAcceptedDeliveries() {
        return deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getCourierID() == null
                        && delivery.getStatus().equals(DeliveryStatus.ACCEPTED))
                .collect(Collectors.toList());
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
