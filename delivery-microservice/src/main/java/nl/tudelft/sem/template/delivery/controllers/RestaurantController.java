package nl.tudelft.sem.template.delivery.controllers;


import nl.tudelft.sem.template.api.RestaurantsApi;


import java.util.List;
import javax.validation.Valid;

import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;



@RestController
public class RestaurantController implements RestaurantsApi {

    private final transient RestaurantService restaurantService;

    private final transient UsersAuthenticationService usersCommunication;

    /**
     * Constructor.
     *
     * @param restaurantService restaurant Service
     * @param usersCommunication user Communication
     */
    @Autowired
    public RestaurantController(RestaurantService restaurantService, UsersAuthenticationService usersCommunication) {

        this.restaurantService = restaurantService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * Inserts the restaurant into the repo.
     *
     * @param restaurant the restaurant to insert
     * @return the entity
     */
    public ResponseEntity<Void> insert(@RequestBody Restaurant restaurant) {
        try {
            restaurantService.insert(restaurant);
            return ResponseEntity.ok().build();
        } catch (RestaurantService.IllegalRestaurantParametersException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant is invalid.");
        }
    }

    /**
     * Inserts the delivery into the repo for testing purposes.
     *
     * @param delivery the delivery to insert
     * @return the entity
     */
    public ResponseEntity<Void> insert(@RequestBody Delivery delivery) {
        try {
            restaurantService.insert(delivery);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery is invalid.");
        }
    }

    /**
     * Add a Restaurant to database.
     *
     * @param restaurantsPostRequest Request body for creating a new Restaurant entity.
     *                               This is an internal endpoint that ensures consistency among microservices.
     *                               Should be called by others when creating a restaurant. (optional)
     * @return response entity with added restaurant
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsPost(@Valid RestaurantsPostRequest restaurantsPostRequest) {
        if (restaurantsPostRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant could not be created.");
        }

        String email = restaurantsPostRequest.getRestaurantID();
        List<Double> address = restaurantsPostRequest.getLocation();
        if (isNullOrEmpty(email) || isInvalidAddress(address)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant ID or location is invalid.");
        }

        Restaurant r = new Restaurant();
        r.setLocation(address);
        r.setRestaurantID(email);
        Restaurant restaurant = restaurantService.insert(r);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Retrieves the restaurant with the specified visibility.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId       User ID for authorization (required)
     * @return the restaurant entity adn the specified error codes
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdGet(@PathVariable("restaurantId") String restaurantId,
                                                                 @RequestHeader String userId) {
        Pair<HttpStatus, String> result = usersCommunication.checkUserAccessToRestaurant(userId, restaurantId,
            "Restaurant");
        if (!(result.getLeft()).equals(HttpStatus.OK)) {
            throw new ResponseStatusException(result.getLeft(), result.getRight());
        }
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        Restaurant r = restaurantService.getRestaurant(restaurantId);
        switch (accountType) {
            case CLIENT, COURIER -> {
                r.setDeliveryZone(null);
                r.setRestaurantID(null);
            }
            default -> { }
        }
        return ResponseEntity.ok(r);
    }

    /**
     * The get method for getting the orders of a restaurant that were not assigned yet.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId       User ID for authorization (required)
     * @return list of orders ready to be taken by couriers
     */
    @Override
    public ResponseEntity<List<Delivery>> restaurantsRestaurantIdNewOrdersGet(@PathVariable String restaurantId,
                                                                              @RequestHeader String userId) {
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        switch (accountType) {
            case ADMIN -> {
                return ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
            }
            case VENDOR -> {
                if (userId.equals(restaurantId)) {
                    return ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
                }
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            case COURIER, CLIENT ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            default ->
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
    }


    /**
     * deletes a restaurant from the system.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @return a string with a message
     */
    @Override
    public ResponseEntity<String> restaurantsRestaurantIdDelete(@PathVariable("restaurantId") String restaurantId) {
        restaurantService.delete(restaurantId);
        return ResponseEntity.status(HttpStatus.OK).body("deletion_successful");
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str string to check
     * @return boolean value indicating whether string is empty or not
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty() || str.isBlank();
    }

    /**
     * Checks if an address is valid.
     *
     * @param str address to check
     * @return boolean value indicating whether address is invalid or not
     */
    public boolean isInvalidAddress(List<Double> str) {
        return str == null || str.size() != 2;
    }
}
