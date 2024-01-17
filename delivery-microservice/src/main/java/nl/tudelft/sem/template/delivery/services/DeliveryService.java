package nl.tudelft.sem.template.delivery.services;

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

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;



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
        return deliveryRepository.save(delivery);
    }

    /**
     * Updates the delivery status.
     *
     * @param deliveryId a delivery to update the status of
     * @param deliveryStatus new status
     */
    public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus deliveryStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setStatus(deliveryStatus);
        deliveryRepository.save(delivery);
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
     * Update the rating of a courier.
     *
     * @param deliveryId delivery to update
     * @param rating new rating score
     */
    public void updateCourierRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setRatingCourier(rating);
        deliveryRepository.save(delivery);
    }

    /**
     * Update the rating of a restaurant.
     *
     * @param deliveryId the delivery to be updated
     * @param rating new rating of restaurant
     */
    public void updateRestaurantRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setRatingRestaurant(rating);
        deliveryRepository.save(delivery);
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
     * Given a new courier ID, assign it to a given delivery.
     *
     * @param deliveryId ID of a delivery to be updated
     * @param courierId ID of a courier to be assigned
     */
    public void updateDeliveryCourier(UUID deliveryId, String courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setCourierID(courierId);
        deliveryRepository.save(delivery);
    }

    /**
     * Updates the estimated preparation time of a delivery object
     * and persists in the database.
     *
     * @param deliveryId ID of delivery to be updated
     * @param prepTime   new value for the update
     */
    @Transactional
    public void updateEstimatedPrepTime(UUID deliveryId, Integer prepTime) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setEstimatedPrepTime(prepTime);
        deliveryRepository.save(delivery);
    }

    /**
     * Updated a delivery pickup time.
     *
     * @param deliveryId the delivery id
     * @param pickupTime the new pickup time
     */
    public void updatePickupTime(UUID deliveryId, OffsetDateTime pickupTime) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setPickupTime(pickupTime);
        deliveryRepository.save(delivery);
    }

    /**
     * Compute an estimate of the delivery time of an order.
     *
     * @param deliveryId ID of the order
     * @return date indicating the estimated delivery time
     */
    public OffsetDateTime computeEstimatedDeliveryTime(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        OffsetDateTime pickup = delivery.getPickupTime();
        OffsetDateTime order = delivery.getOrderTime();
        DeliveryStatus status = delivery.getStatus();

        Integer minutes = 60; // default value
        OffsetDateTime estimate = order.plusMinutes(minutes);

        switch (status) {
            case PENDING, ACCEPTED, PREPARING -> {
                Restaurant restaurant = getRestaurant(delivery.getRestaurantID());
                List<Double> restaurantCoordinates = restaurant.getLocation();

                minutes = this.computeTimeStillInRestaurant(delivery.getEstimatedPrepTime(),
                        restaurantCoordinates, delivery.getDeliveryAddress());
                estimate = order.plusMinutes(minutes);
            }
            case GIVEN_TO_COURIER -> {
                Restaurant restaurant = getRestaurant(delivery.getRestaurantID());
                List<Double> restaurantCoordinates = restaurant.getLocation();

                minutes = this.computeTransitTime(restaurantCoordinates, delivery.getDeliveryAddress());
                estimate = pickup.plusMinutes(minutes);
            }
            case ON_TRANSIT -> {
                List<Double> currentCoordinates = gps.getCurrentCoordinates();
                minutes = this.computeTransitTime(currentCoordinates, delivery.getDeliveryAddress());
                estimate = OffsetDateTime.now().plusMinutes(minutes);
            }
            case DELIVERED -> throw new OrderAlreadyDeliveredException();
            case REJECTED -> throw new OrderRejectedException();
        }

        Integer unexpectedDelay = delivery.getError().getValue();
        if (unexpectedDelay == null) {
            unexpectedDelay = 0;
        }

        return estimate.plusMinutes(unexpectedDelay);
    }

    /**
     * Compute an estimate for the delivery time of an order
     * that is pending, accepted, or being prepared.
     *
     * @param prepTime              time for preparation
     * @param restaurantCoordinates coordinates of the restaurant
     * @param deliveryAddress       address where the order needs to be delivered
     * @return an integer indicating the estimated time to delivery
     */
    public Integer computeTimeStillInRestaurant(Integer prepTime,
                                                List<Double> restaurantCoordinates, List<Double> deliveryAddress) {
        if (prepTime == null || prepTime == 0) {
            // Assign a default value if no such is provided by the vendor
            prepTime = 30;
        }
        Integer transitTime = this.computeTransitTime(restaurantCoordinates, deliveryAddress);
        return prepTime + transitTime;
    }

    /**
     * Compute an estimate for the transit time of an order.
     * That is how much time it takes for the order to be brought
     * from the current location (might be the restaurant) to the delivery address.
     *
     * @param coordA provide coordinates of starting point
     * @param coordB provide coordinates of finishing point
     * @return minutes indicating the estimated time for bringing the food
     */
    public Integer computeTransitTime(List<Double> coordA, List<Double> coordB) {
        double lat1 = coordA.get(0);
        double lon1 = coordA.get(1);
        double lat2 = coordB.get(0);
        double lon2 = coordB.get(1);
        double distance = computeHaversine(lat1, lon1, lat2, lon2);
        // Average car speed worldwide km/h
        double avgVelocity = 30;
        // Return minutes
        return (int) Math.round(60 * distance / avgVelocity);
    }

    /**
     * Compute the distance between two coordinates.
     *
     * @param lat1 latitude of 1st coordinate
     * @param lon1 longitude of 1st coordinate
     * @param lat2 latitude of 2nd coordinate
     * @param lon2 longitude of 2nd coordinate
     * @return the distance in kilometers as a double number
     */
    public Double computeHaversine(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);

        // Calculate differences
        double diffLat = lat2 - lat1;
        double diffLon = lon2 - lon1;

        // Haversine formula
        double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(diffLon / 2) * Math.sin(diffLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        double RADIUS_OF_EARTH = 6371;
        return RADIUS_OF_EARTH * c;
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

    /**
     * Update the delivery address of an order.
     *
     * @param deliveryId ID of delivery to be updated
     * @param newAddress the new address
     */
    public void updateDeliveryAddress(UUID deliveryId, List<Double> newAddress) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        delivery.setDeliveryAddress(newAddress);
        deliveryRepository.save(delivery);
    }

}
