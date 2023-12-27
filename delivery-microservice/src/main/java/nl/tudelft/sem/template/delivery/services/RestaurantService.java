package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
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

    @Autowired
    private final RestaurantRepository restaurantRepository;

    /**
     * Constructor
     *
     * @param restaurantRepository repository restaurant is stored in
     */
    public RestaurantService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
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
     * Exception to be used when a Restaurant entity with a given ID is not found.
     */
    static public class RestaurantNotFoundException extends ResponseStatusException {
        public RestaurantNotFoundException() {
            super(HttpStatus.NOT_FOUND, "Restaurant with specified id not found");
        }
    }
}
