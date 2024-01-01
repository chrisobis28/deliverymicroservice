package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequestMapping("/statistics")
@RestController
public class StatisticsController implements StatisticsApi {

    private final StatisticsService statisticsService;

    private final UsersAuthenticationService usersCommunication;

    /**
     * Constructor for statistics controller
     *
     * @param statisticsService  the statistics service
     * @param usersCommunication mock for users authorization
     */
    @Autowired
    public StatisticsController(StatisticsService statisticsService, UsersAuthenticationService usersCommunication) {
        this.statisticsService = statisticsService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * inserts an element into the repo (internal method)
     * @param delivery delivery being inserted
     * @return an empty response entity with a corresponding status code
     */
    public ResponseEntity<Void> insert(@RequestBody Delivery delivery) {
        try {
            statisticsService.insert(delivery);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets the restaurant rating given to orders
     *
     * @param userId   used for authorization
     * @param orderIds the orders for which we want to retrieve the ratings
     * @return a list of ratings that has the same size as the orderIds list
     * if an order doesn't have a rating, we insert null instead
     */
    @GetMapping("/ratings-for-orders")
    @Override
    public ResponseEntity<List<Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId, @RequestBody List<UUID> orderIds) {
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN)) {
            List<Integer> ratings = new ArrayList<>();
            for (UUID orderId : orderIds) {
                ratings.add(statisticsService.getOrderRating(orderId));
            }
            return ResponseEntity.ok(ratings);
        } else if (userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            // User lacks necessary permission levels.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        } else {
            // User lacks valid authentication credentials.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }
    }

    /**
     * Get no. of deliveries made in each hour bracket of a day averaged over no. of days delivered for
     * e.g., if there is only data for 13/12/2023 and 12/12/2023 then delivery count is averaged over 2 days
     *
     * @param userId   User ID for authorization
     * @param vendorId ID of the vendor whose delivery stats are being queried
     * @return list of doubles representing avg deliveries in each hr bracket
     */
    @Override
    public ResponseEntity<List<Double>> statisticsDeliveriesPerHourGet(@Parameter String userId, @Parameter String vendorId) {
        if (isNullOrEmpty(userId) || isNullOrEmpty(vendorId)) {
            return ResponseEntity.badRequest().build();
        }
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
        boolean isVendor = type.equals(UsersAuthenticationService.AccountType.VENDOR) && vendorId.equals(userId);
        if (!isVendor && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Delivery> deliveries = statisticsService.getOrdersOfAVendor(vendorId);
        if (deliveries == null || deliveries.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<Double> count = statisticsService.getDeliveriesPerHour(deliveries);
        return ResponseEntity.ok(count);
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
