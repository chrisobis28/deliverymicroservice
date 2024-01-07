package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.models.Response;
import java.time.OffsetDateTime;
import java.util.UUID;
import nl.tudelft.sem.template.api.RestaurantsApi;
import nl.tudelft.sem.template.delivery.AddressAdapter;
//import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
import org.h2.engine.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

//import static org.mockito.Mockito.mock;

@RestController
public class RestaurantController implements RestaurantsApi {

    private final RestaurantService restaurantService;

    //GPS mockGPS = mock(GPS.class);
    private final AddressAdapter addressAdapter;
    private final UsersAuthenticationService usersCommunication;

    /**
     * Constructor
     * @param restaurantService the restaurant service
     */
    @Autowired
    public RestaurantController(RestaurantService restaurantService, AddressAdapter addressAdapter, UsersAuthenticationService usersCommunication) {
        this.restaurantService = restaurantService;
        this.addressAdapter = addressAdapter;
        this.usersCommunication = usersCommunication;
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
     * The put method for updating the location of a restaurant
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId User ID for authorization (required)
     * @param requestBody Coordinates of the new location of the restaurant
     * @return updated location of a restaurant
     */
    @Override
    @PutMapping("/restaurants/{restaurantId}/location")
    public ResponseEntity<Restaurant> restaurantsRestaurantIdLocationPut(@PathVariable String restaurantId, @RequestHeader String userId,
                                                                         @RequestBody List<Double> requestBody) {
        try{
            UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
            switch(accountType){
                case ADMIN: {
                    restaurantService.updateLocation(restaurantId, requestBody);
                    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                }
                case VENDOR: {
                    if (userId.equals(restaurantId)){
                        restaurantService.updateLocation(restaurantId, requestBody);
                        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
                case COURIER:
                case CLIENT:
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        catch (RestaurantService.RestaurantNotFoundException e){
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * The put method for updating the delivery zone of a restaurant
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId User ID for authorization (required)
     * @param requestBody Radius of the new delivery zone of the Restaurant
     * @return updated delivery zone of a restaurant
     */
    @Override
    @PutMapping("/restaurants/{restaurantId}/deliver-zone")
    public ResponseEntity<Restaurant> restaurantsRestaurantIdDeliverZonePut(@PathVariable String restaurantId, @RequestHeader String userId,
                                                                         @RequestBody Double requestBody) {
        try{
            UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
            switch(accountType){
                case ADMIN: {
                    restaurantService.updateDeliverZone(restaurantId, requestBody);
                    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                }
                case VENDOR: {
                    if (userId.equals(restaurantId)){
                        restaurantService.updateDeliverZone(restaurantId, requestBody);
                        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
                case COURIER:
                case CLIENT:
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        catch (RestaurantService.RestaurantNotFoundException e){
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * The get method for getting the orders of a restaurant that were not assigned yet
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId User ID for authorization (required)
     * @return list of orders ready to be taken by couriers
     */
    @Override
    @GetMapping("/restaurants/{restaurantId}/new-orders")
    public ResponseEntity<List<Delivery>> restaurantsRestaurantIdNewOrdersGet(@PathVariable String restaurantId, @RequestHeader String userId){
        try {
            UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
            switch(accountType){
                case ADMIN: {
                    return ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
                }
                case VENDOR: {
                    if(userId.equals(restaurantId))
                        ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
                case COURIER:
                case CLIENT:
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        catch(RestaurantService.RestaurantNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
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
