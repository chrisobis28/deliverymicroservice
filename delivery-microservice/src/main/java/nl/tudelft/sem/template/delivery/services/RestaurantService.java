package nl.tudelft.sem.template.delivery.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


/**
 * This class is a Service for accessing and modifying Restaurant entities.
 */
@Service
public class RestaurantService {

    private final transient RestaurantRepository restaurantRepository;
    @Lazy
    private final transient DeliveryRepository deliveryRepository;

    /**
     * Constructor.
     *
     * @param restaurantRepository repository restaurant is stored in
     */
    public RestaurantService(RestaurantRepository restaurantRepository, DeliveryRepository deliveryRepository) {
        this.restaurantRepository = restaurantRepository;
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Inserts the restaurant into the repo.
     *
     * @param restaurant the restaurant to be saved in the repo
     * @return the entity
     */

    public Restaurant insert(Restaurant restaurant) {
        if (restaurant.getRestaurantID() == null || restaurant.getLocation() == null
                || restaurant.getLocation().size() != 2) {
            throw new IllegalRestaurantParametersException();
        }
        try {
            getRestaurant(restaurant.getRestaurantID());
        } catch (RestaurantNotFoundException e) {
            return restaurantRepository.save(restaurant);
        }
        throw new IllegalRestaurantParametersException();
    }


    /**
     * Inserts the delivery into the repo.
     *
     * @param delivery delivery object to be added
     * @return the entity
     */
    public Delivery insert(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException();
        }
        return deliveryRepository.save(delivery);
    }

    /**
     * Exception to be used when a Restaurant entity with a given ID is not found.
     */
    static public class RestaurantNotFoundException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        public RestaurantNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Restaurant with specified id not found");
        }
    }

    /**
     * Exception to be used when a Restaurant entity with a given ID already exists or has the same location.
     */
    static public class IllegalRestaurantParametersException extends ResponseStatusException {
        private static final long serialVersionUID = 1L;

        public IllegalRestaurantParametersException() {
            super(HttpStatus.BAD_REQUEST, "Restaurant with those parameters cannot be created");
        }
    }



    /**
     * Gets all orders with status Preparing or Accepted.
     *
     * @param restaurantId given restaurant for which we retrieve the new orders
     * @return a list of new orders
     */
    public List<Delivery> getAllNewOrders(String restaurantId) {
        Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
        return deliveryRepository.findAllByrestaurantID(restaurantId).stream().filter(d -> d.getCourierID() == null)
                .filter(d -> List.of(DeliveryStatus.PREPARING, DeliveryStatus.ACCEPTED)
                        .contains(d.getStatus())).collect(Collectors.toList());
    }

    /**
     * Deletes the restaurant.
     *
     * @param restaurantId the id of the restaurant
     */
    public void delete(String restaurantId) {
        restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByrestaurantID(restaurantId);

        for (Delivery d : deliveries) {
            d.setRestaurantID(null);
            deliveryRepository.save(d);
        }
        restaurantRepository.deleteById(restaurantId);

    }

    /**
     * Retrieve restaurant or throw an exception if not found.
     *
     * @param restaurantId restaurant id
     * @return the restaurant
     */
    public Restaurant getRestaurant(String restaurantId) {
        if (restaurantId == null) {
            throw new RestaurantService.RestaurantNotFoundException();
        }
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
    }


    /**
     * Updates the location.
     *
     * @param restaurantId the ID of the restaurant to be updated
     * @param requestBody location
     */
    public void updateLocation(String restaurantId, List<Double> requestBody) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
        r.location(new ArrayList<>(requestBody));
        restaurantRepository.save(r);
    }

    /**
     * Updates the delivery zone.
     *
     * @param restaurantId ID of restaurant to be updated
     * @param requestBody delivery zone
     */
    public void updateDeliverZone(String restaurantId, Double requestBody) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
        r.setDeliveryZone(requestBody);
        restaurantRepository.save(r);
    }

    /**
     * sets the new list of couriers or throws  an exception.
     *
     * @param restaurantId the id of the restaurant
     * @param couriers     the new couriers
     * @return the changed restaurant entity
     */
    public Restaurant setListOfCouriers(String restaurantId, List<String> couriers) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(RestaurantService.RestaurantNotFoundException::new);
        r.couriers(couriers);
        restaurantRepository.save(r);
        return r;
    }
}
