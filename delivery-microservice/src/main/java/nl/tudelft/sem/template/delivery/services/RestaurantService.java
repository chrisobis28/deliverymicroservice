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

/**
 * This class is a Service for accessing and modifying Restaurant entities.
 */
@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final DeliveryRepository deliveryRepository;

    /**
     * Constructor
     *
     * @param restaurantRepository repository restaurant is stored in
     */
    public RestaurantService(RestaurantRepository restaurantRepository, DeliveryRepository deliveryRepository) {
        this.restaurantRepository = restaurantRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public Restaurant getRestaurant(String restaurantId) {
        return restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
    }

    /**
     * Inserts the restaurant into the repo
     *
     * @param restaurant restaurant object to be added
     * @return the entity
     */
    public Restaurant insert(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException();
        }
        return restaurantRepository.save(restaurant);
    }

    /**
     * Inserts the delivery into the repo
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
        public RestaurantNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Restaurant with specified id not found");
        }
    }

    public void updateLocation(String restaurantId, List<Double> requestBody){
        Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
        r.setLocation(requestBody);
        restaurantRepository.save(r);
    }

    public void updateDeliverZone(String restaurantId, Double requestBody){
        Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
        r.setDeliveryZone(requestBody);
        restaurantRepository.save(r);
    }

    public List<Delivery> getAllNewOrders(String restaurantId){
        Restaurant r = restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
        return deliveryRepository.findAllByrestaurantID(restaurantId).stream().filter(d -> d.getCourierID() == null)
            .filter(d -> List.of(DeliveryStatus.PREPARING, DeliveryStatus.ACCEPTED)
            .contains(d.getStatus())).collect(Collectors.toList());
    }
}
