package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class is a Service for accessing and modifying Delivery entities.
 */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RestaurantRepository restaurantRepository;

    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository, RestaurantRepository restaurantRepository) {
        this.deliveryRepository = deliveryRepository;
        this.restaurantRepository = restaurantRepository;
    }

//    /**
//     * Get all deliveries in db
//     * @return list of deliveries
//     */
//    public List<Delivery> findAll() {
//        return deliveryRepository.findAll();
//    }

    /**
     * Check if restaurant uses own couriers
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

    public void updateDeliveryStatus(UUID deliveryId, DeliveryStatus deliveryStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setStatus(deliveryStatus);
        deliveryRepository.save(delivery);
    }

    /**
     * Function that returns the address where the food needs to be delivered
     *
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getDeliveryAddress(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);
        return delivery.map(Delivery::getDeliveryAddress).orElseThrow(DeliveryNotFoundException::new);
    }

    /**
     * Function that returns the address where the food needs to be picked up
     *
     * @param deliveryId the delivery entity
     * @return the address
     */
    public List<Double> getPickupLocation(UUID deliveryId) {
        Optional<Delivery> delivery = deliveryRepository.findById(deliveryId);

        if (delivery.isEmpty()) {
            throw new DeliveryNotFoundException();
        }

        Optional<Restaurant> restaurant = restaurantRepository.findById(delivery.get().getRestaurantID());

        return restaurant.map(Restaurant::getLocation).orElseThrow(DeliveryNotFoundException::new);
    }



    public void updateCourierRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setRatingCourier(rating);
        deliveryRepository.save(delivery);
    }

    public void updateRestaurantRating(UUID deliveryId, Integer rating) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setRatingRestaurant(rating);
        deliveryRepository.save(delivery);
    }

    /**
     * Exception to be used when a Delivery entity with a given ID is not found.
     */
    static public class DeliveryNotFoundException extends ResponseStatusException {
        public DeliveryNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Delivery with specified id not found");
        }
    }

    /**
     * Exception to be used when a Delivery entity has a status Delivered,
     * but processing methods that affect its state are being called
     */
    static public class OrderAlreadyDeliveredException extends ResponseStatusException {
        public OrderAlreadyDeliveredException() {
            super(HttpStatus.CONFLICT, "Delivery with specified id has already been delivered.");
        }
    }

    /**
     * Exception to be used when one wants to process a rejected order
     */
    static public class OrderRejectedException extends ResponseStatusException {
        public OrderRejectedException() {
            super(HttpStatus.CONFLICT, "Delivery with specified id has been rejected.");
        }
    }

    public List<Delivery> getAcceptedDeliveries() {
        return deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getCourierID() == null).collect(Collectors.toList());
    }

    public void updateDeliveryCourier(UUID deliveryId, String courierId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setCourierID(courierId);
        deliveryRepository.save(delivery);
    }

    /**
     * Updates the estimated preparation time of a delivery object
     * and persists in the database
     *
     * @param deliveryId ID of delivery to be updated
     * @param prepTime   new value for the update
     */
    public void updateEstimatedPrepTime(UUID deliveryId, Integer prepTime) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        delivery.setEstimatedPrepTime(prepTime);
        deliveryRepository.save(delivery);
    }

    /**
     * Updated a delivery pickup time
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
     * Compute an estimate of the delivery time of an order
     *
     * @param deliveryId ID of the order
     * @param now        when was the estimate requested
     * @return date indicating the estimated delivery time
     */
    public OffsetDateTime computeEstimatedDeliveryTime(UUID deliveryId, OffsetDateTime now) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryNotFoundException::new);
        OffsetDateTime pickup = delivery.getPickupTime();
        DeliveryStatus status = delivery.getStatus();

        Integer minutes = 60; // default value
        OffsetDateTime estimate = now.plusMinutes(minutes);

        switch (status) {
            case PENDING, ACCEPTED, PREPARING -> {
                minutes = this.computeTimeStillInRestaurant(delivery);
                estimate = now.plusMinutes(minutes);
            }
            case GIVEN_TO_COURIER -> {
                // TODO: how to get restaurant coordinates
                List<Double> restaurantCoordinates = List.of(0.0, 0.0);
                minutes = this.computeTransitTime(restaurantCoordinates, delivery.getDeliveryAddress());
                estimate = pickup.plusMinutes(minutes);
            }
            case ON_TRANSIT -> {
                // TODO: how to get current coordinates
                List<Double> currentCoordinates = List.of(0.0, 0.0);
                minutes = this.computeTransitTime(currentCoordinates, delivery.getDeliveryAddress());
                estimate = now.plusMinutes(minutes);
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
     * that is pending, accepted, or being prepared
     *
     * @param delivery given delivery
     * @return an integer indicating the estimated time to delivery
     */
    public Integer computeTimeStillInRestaurant(Delivery delivery) {
        Integer prepTime = delivery.getEstimatedPrepTime();
        if (prepTime == null || prepTime == 0) {
            // Assign a default value if no such is provided by the vendor
            prepTime = 30;
        }
        // TODO: get restaurant Coordinates
        List<Double> restaurantCoordinates = List.of(0.0, 0.0);
        Integer transitTime = this.computeTransitTime(restaurantCoordinates, delivery.getDeliveryAddress());
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
        // Average car speed worldwide
        double velocity = 30;
        return (int) Math.round(distance / velocity);
    }

    /**
     * Compute the distance between two coordinates
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
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance in kilometers
        double RADIUS_OF_EARTH = 6371;
        return RADIUS_OF_EARTH * c;
    }

    /**
     * Get a restaurant by a given ID
     *
     * @param restaurantId ID of restaurant
     * @return Restaurant entity from the repo
     */
    public Restaurant getRestaurant(String restaurantId) {
        return restaurantRepository.findById(restaurantId).orElseThrow(RestaurantService.RestaurantNotFoundException::new);

    }
}
