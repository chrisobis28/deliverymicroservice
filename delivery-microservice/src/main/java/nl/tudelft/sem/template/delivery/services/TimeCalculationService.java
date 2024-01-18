package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TimeCalculationService {

    private final transient DeliveryRepository deliveryRepository;

    private final transient GPS gps;

    private final transient RestaurantRepository restaurantRepository;

    /**
     * Constructor.
     *
     * @param deliveryRepository    delivery repository
     * @param gps                   mock for gps
     * @param restaurantRepository  restaurant repository
     */
    public TimeCalculationService(DeliveryRepository deliveryRepository, GPS gps,
                                  RestaurantRepository restaurantRepository) {
        this.deliveryRepository = deliveryRepository;
        this.gps = gps;
        this.restaurantRepository = restaurantRepository;
    }

    /**
     * Updates the estimated preparation time of a delivery object
     * and persists in the database.
     *
     * @param deliveryId ID of delivery to be updated
     * @param prepTime   new value for the update
     */
    public void updateEstimatedPrepTime(UUID deliveryId, Integer prepTime) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
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
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
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
        Delivery delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        OffsetDateTime pickup = delivery.getPickupTime();
        OffsetDateTime order = delivery.getOrderTime();
        DeliveryStatus status = delivery.getStatus();

        Integer minutes = 60; // default value
        OffsetDateTime estimate = order.plusMinutes(minutes);

        switch (status) {
            case PENDING, ACCEPTED, PREPARING -> {
                Restaurant restaurant = restaurantRepository.findById(delivery.getRestaurantID())
                    .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
                List<Double> restaurantCoordinates = restaurant.getLocation();

                minutes = this.computeTimeStillInRestaurant(delivery.getEstimatedPrepTime(),
                    restaurantCoordinates, delivery.getDeliveryAddress());
                estimate = order.plusMinutes(minutes);
            }
            case GIVEN_TO_COURIER -> {
                Restaurant restaurant = restaurantRepository.findById(delivery.getRestaurantID())
                    .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
                List<Double> restaurantCoordinates = restaurant.getLocation();

                minutes = this.computeTransitTime(restaurantCoordinates, delivery.getDeliveryAddress());
                estimate = pickup.plusMinutes(minutes);
            }
            case ON_TRANSIT -> {
                List<Double> currentCoordinates = gps.getCurrentCoordinates();
                minutes = this.computeTransitTime(currentCoordinates, delivery.getDeliveryAddress());
                estimate = OffsetDateTime.now().plusMinutes(minutes);
            }
            case DELIVERED -> throw new DeliveryService.OrderAlreadyDeliveredException();
            case REJECTED -> throw new DeliveryService.OrderRejectedException();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown delivery status.");
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
}
