package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.RestaurantsApi;
import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
//import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.mockito.Mockito.mock;

@RestController
public class RestaurantController implements RestaurantsApi {

    private final RestaurantService restaurantService;

    private GPS mockGPS = mock(GPS.class);

    /**
     * Constructor
     * @param restaurantService the restaurant service
     */
    @Autowired
    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    /**
     * Inserts the restaurant into the repo
     * @param restaurant the restaurant to insert
     * @return the entity
     */
    public ResponseEntity<Void> insert(@RequestBody Restaurant restaurant) {
        try {
            restaurantService.insert(restaurant);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<Restaurant> restaurantsPost(@Valid RestaurantsPostRequest restaurantsPostRequest) {
        if (restaurantsPostRequest == null || isNullOrEmpty(restaurantsPostRequest.getRestaurantID())) {
            return ResponseEntity.badRequest().build();
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantsPostRequest.getRestaurantID());
        restaurant.setLocation(mockGPS.getCoordinatesOfAddress(restaurantsPostRequest.getLocation()));
        restaurantService.insert(restaurant);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Checks if a string is null or empty
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.equals(" ");
    }
}
