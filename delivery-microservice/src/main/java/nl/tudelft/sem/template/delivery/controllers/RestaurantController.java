package nl.tudelft.sem.template.delivery.controllers;

import java.util.Objects;

import nl.tudelft.sem.template.api.RestaurantsApi;
//import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import nl.tudelft.sem.template.model.RestaurantsPostRequest;
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
import org.springframework.web.server.ResponseStatusException;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.COURIER;

//import static org.mockito.Mockito.mock;

@RestController
public class RestaurantController implements RestaurantsApi {

    private final RestaurantService restaurantService;

    private final UsersAuthenticationService usersCommunication;

    /**
     * Constructor
     *
     * @param restaurantService the restaurant service
     */
    @Autowired
    public RestaurantController(RestaurantService restaurantService, UsersAuthenticationService usersCommunication) {
        this.restaurantService = restaurantService;
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
        } catch (RestaurantService.IllegalRestaurantParametersException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Inserts the delivery into the repo for testing purposes
     * @param delivery the delivery to insert
     * @return the entity
     */
    public ResponseEntity<Void> insert(@RequestBody Delivery delivery) {
        try {
            restaurantService.insert(delivery);
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
        List<Double> address = restaurantsPostRequest.getLocation();
        if (isNullOrEmpty(email) || isInvalidAddress(address)) {
            return ResponseEntity.badRequest().build();
        }
        Restaurant r = new Restaurant();
        r.setLocation(address);
        r.setRestaurantID(email);
        Restaurant restaurant = restaurantService.insert(r);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * Retrieves the restaurant with the specified visibility
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId User ID for authorization (required)
     * @return the restaurant entity adn the specified error codes
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdGet( @PathVariable("restaurantId") String restaurantId,@RequestHeader String userId) {
        //check user ID
        if(userId==null || restaurantId==null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
            Restaurant r = restaurantService.getRestaurant(restaurantId);
            switch (accountType) {
                case COURIER, CLIENT:
                    r.setDeliveryZone(null);
                    r.setRestaurantID(null);
                    return ResponseEntity.status(HttpStatus.OK).body(r);
                case VENDOR:
                    if (!Objects.equals(userId, restaurantId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                    } else {
                        return ResponseEntity.status(HttpStatus.OK).body(r);
                    }

                case ADMIN:
                    return ResponseEntity.status(HttpStatus.OK).body(r);

                default:
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

            }
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
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        switch (accountType) {
            case ADMIN -> {
                restaurantService.updateLocation(restaurantId, requestBody);
                return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
            }
            case VENDOR -> {
                if (userId.equals(restaurantId)) {
                    restaurantService.updateLocation(restaurantId, requestBody);
                    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                }
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
            }
            case COURIER, CLIENT ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
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
            UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
            switch (accountType) {
                case ADMIN -> {
                    restaurantService.updateDeliverZone(restaurantId, requestBody);
                    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                }
                case VENDOR -> {
                    if (userId.equals(restaurantId) && !restaurantService.getRestaurant(restaurantId).getCouriers().isEmpty()){
                        restaurantService.updateDeliverZone(restaurantId, requestBody);
                        return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
                    }
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
                }
                case COURIER, CLIENT ->
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
            }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
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
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        switch (accountType) {
            case ADMIN -> {
                return ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
            }
            case VENDOR -> {
                if (userId.equals(restaurantId))
                    return ResponseEntity.ok(restaurantService.getAllNewOrders(restaurantId));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
            }
            case COURIER, CLIENT ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    /**
     * Sets the list of couriers and returns the correct response codes
     * @param restaurantId ID of the Restaurant entity (required)
     * @param userId User ID for authorization (required)
     * @param requestBody Put a preferred set of couriers for the restaurant (required)
     * @return the modified restaurant entity
     */
    @Override
    public ResponseEntity<Restaurant> restaurantsRestaurantIdCouriersPut( @PathVariable("restaurantId") String restaurantId, @RequestHeader String userId,  @RequestBody @Valid List<String> requestBody)   {
        //check user ID
        if(userId==null || restaurantId==null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        switch(accountType){
            case COURIER,CLIENT: return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            case  VENDOR:
                if(!Objects.equals(userId, restaurantId)){
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
                else break;

            case ADMIN : break;
            default : return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

        }
        // check couriers
        if(requestBody!=null) {
            for(String id : requestBody){
                UsersAuthenticationService.AccountType account = usersCommunication.getUserAccountType(id);
                if(!Objects.equals(account,COURIER)){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
        }
        try{
            Restaurant r = restaurantService.setListOfCouriers(restaurantId,requestBody);
            return ResponseEntity.status(HttpStatus.OK).body(r);
        }catch(RestaurantService.RestaurantNotFoundException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }



    }

    /**
     * deletes a restaurant from the system
     * @param restaurantId ID of the Restaurant entity (required)
     * @return a string with a message
     */
    @Override
    public ResponseEntity<String> restaurantsRestaurantIdDelete(@PathVariable("restaurantId") String restaurantId) {
            restaurantService.delete(restaurantId);
            return ResponseEntity.status(HttpStatus.OK).body("deletion_successful");

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
     * @param str address to check
     * @return boolean value indicating whether address is invalid or not
     */
    public boolean isInvalidAddress(List<Double> str) {
        return str == null || str.size() != 2 ;
    }
}
