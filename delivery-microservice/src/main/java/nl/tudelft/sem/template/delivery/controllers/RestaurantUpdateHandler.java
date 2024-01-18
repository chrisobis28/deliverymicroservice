package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.RestaurantsApi;
import nl.tudelft.sem.template.delivery.services.UpdateRestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Restaurant;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.COURIER;
import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.VENDOR;

@RestController
public class RestaurantUpdateHandler implements RestaurantsApi {
    private final transient UpdateRestaurantService restaurantService;

    private final transient UsersAuthenticationService usersCommunication;

    /**
    * Constructor.
    *
    * @param restaurantService service for restaurants
    * @param usersCommunication service for authenticating users
    */
    @Autowired
    public RestaurantUpdateHandler(UpdateRestaurantService restaurantService,
                                   UsersAuthenticationService usersCommunication) {
        this.restaurantService = restaurantService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * The put method for updating the location of a restaurant.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId       User ID for authorization (required)
     * @param requestBody  Coordinates of the new location of the restaurant
     * @return updated location of a restaurant
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdLocationPut(@PathVariable String restaurantId,
                                                                         @RequestHeader String userId,
                                                                         @RequestBody List<Double> requestBody) {
        Pair<HttpStatus, String> result = usersCommunication.checkUserAccessToRestaurant(userId, restaurantId, "Location");
        if (!(result.getLeft()).equals(HttpStatus.OK)) {
            throw new ResponseStatusException(result.getLeft(), result.getRight());
        }
        restaurantService.updateLocation(restaurantId, requestBody);
        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
    }

    /**
     * The put method for updating the delivery zone of a restaurant.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId       User ID for authorization (required)
     * @param requestBody  Radius of the new delivery zone of the Restaurant
     * @return updated delivery zone of a restaurant
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdDeliverZonePut(@PathVariable String restaurantId,
                                                                            @RequestHeader String userId,
                                                                            @RequestBody Double requestBody) {
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        Pair<HttpStatus, String> result = usersCommunication.checkUserAccessToRestaurant(userId, restaurantId,
            "Delivery Zone");
        if (!(result.getLeft()).equals(HttpStatus.OK)) {
            throw new ResponseStatusException(result.getLeft(), result.getRight());
        }
        Restaurant r = restaurantService.getRestaurant(restaurantId);
        if (accountType.equals(VENDOR) && r.getCouriers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
        }
        restaurantService.updateDeliverZone(restaurantId, requestBody);
        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
    }

    /**
     * Sets the list of couriers and returns the correct response codes.
     *
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId       User ID for authorization (required)
     * @param requestBody  Put a preferred set of couriers for the restaurant (required)
     * @return the modified restaurant entity
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdCouriersPut(@PathVariable String restaurantId,
                                                                         @RequestHeader String userId,
                                                                         @RequestBody @Valid List<String> requestBody) {
        Pair<HttpStatus, String> result = usersCommunication.checkUserAccessToRestaurant(userId, restaurantId,
            "Couriers");
        if (!(result.getLeft()).equals(HttpStatus.OK)) {
            throw new ResponseStatusException(result.getLeft(), result.getRight());
        }
        // check couriers
        if (requestBody != null) {
            for (String id : requestBody) {
                UsersAuthenticationService.AccountType account = usersCommunication.getUserAccountType(id);
                if (!Objects.equals(account, COURIER)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "List contains the id of someone who isn't a courier.");
                }
            }
        }
        Restaurant r = restaurantService.setListOfCouriers(restaurantId, requestBody);
        return ResponseEntity.ok(r);
    }
}
