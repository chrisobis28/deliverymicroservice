package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }
    public ResponseEntity<Void> insert(@RequestBody Restaurant restaurant) {
        try {
            restaurantService.insert(restaurant);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
