package nl.tudelft.sem.template.delivery.services;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/**
 * This class is a Service for accessing and modifying Restaurant entities.
 */
@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public Restaurant getRestaurant(String restaurantId) {
        return restaurantRepository.findById(restaurantId).orElseThrow(RestaurantNotFoundException::new);
    }

    public Restaurant insert(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException();
        }
        return restaurantRepository.save(restaurant);
    }

    /**
     * Exception to be used when a Restaurant entity with a given ID is not found.
     */
    static public class RestaurantNotFoundException extends ResponseStatusException {
        public RestaurantNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Restaurant with specified id not found");
        }
    }
}
