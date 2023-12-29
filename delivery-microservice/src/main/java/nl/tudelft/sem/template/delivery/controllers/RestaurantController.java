package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.RestaurantsApi;
import nl.tudelft.sem.template.delivery.AddressAdapter;
//import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

//import static org.mockito.Mockito.mock;

@RestController
public class RestaurantController implements RestaurantsApi {

    private final RestaurantService restaurantService;

    //GPS mockGPS = mock(GPS.class);
    private final AddressAdapter addressAdapter;

    /**
     * Constructor
     * @param restaurantService the restaurant service
     */
    @Autowired
    public RestaurantController(RestaurantService restaurantService, AddressAdapter addressAdapter) {
        this.restaurantService = restaurantService;
        this.addressAdapter = addressAdapter;
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

    /**
     * Add a Restaurant to database
     * @param restaurantsPostRequest Request body for creating a new Restaurant entity. This is an internal endpoint that ensures consistency among microservices. Should be called by others when creating a restaurant. (optional)
     * @return response entity with added restaurant
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsPost(@Valid RestaurantsPostRequest restaurantsPostRequest) {
        if (restaurantsPostRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        String email = restaurantsPostRequest.getRestaurantID();
        List<String> address = restaurantsPostRequest.getLocation();
        if (isNullOrEmpty(email) || isInvalidAddress(address)) {
            return ResponseEntity.badRequest().build();
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(email);
        restaurant.setLocation(addressAdapter.convertStringAddressToDouble(address));
        restaurantService.insert(restaurant);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Checks if a string is null or empty
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.isBlank();
    }

    /**
     * Checks if an address is valid
     * @param str string to check
     * @return boolean value indicating whether address is invalid or not
     */
    public boolean isInvalidAddress(List<String> str) {
        return str == null || str.size() != 5 || str.contains("") || str.contains(" ");
    }
}
