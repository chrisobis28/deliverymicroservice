package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.beans.factory.annotation.Autowired;
import nl.tudelft.sem.template.model.Statistics;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.time.OffsetDateTime;

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
    public StatisticsController(StatisticsService statisticsService,UsersAuthenticationService usersCommunication) {
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BAD REQUEST");
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
    public ResponseEntity<Map<String, Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId, @RequestBody List<UUID> orderIds) {
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        // After second consideration I changed the permission levels of this endpoint,
        // so that everyone can see the ratings for transparency reasons.
        if (userType.equals(UsersAuthenticationService.AccountType.ADMIN) ||
                userType.equals(UsersAuthenticationService.AccountType.VENDOR) ||
                userType.equals(UsersAuthenticationService.AccountType.COURIER) ||
                userType.equals(UsersAuthenticationService.AccountType.CLIENT)) {
            Map<String, Integer> ratings = new HashMap<>();
            for (UUID orderId : orderIds) {
                ratings.put(orderId.toString(), statisticsService.getOrderRating(orderId));
            }
            return ResponseEntity.ok(ratings);
        } else {
            // User lacks valid authentication credentials.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID or restaurant ID is invalid.");
        }
        UsersAuthenticationService.AccountType type = usersCommunication.getUserAccountType(userId);
//        boolean isVendor = type.equals(UsersAuthenticationService.AccountType.VENDOR) && ;
//        if (!isVendor && !type.equals(UsersAuthenticationService.AccountType.ADMIN)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
//        }
        switch (type) {
            case ADMIN: break;
            case COURIER, CLIENT: throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            case VENDOR: {
                if (vendorId.equals(userId)) break;
                else throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks necessary permissions.");
            }
            default: throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User lacks valid authentication credentials.");
        }
        List<Delivery> deliveries = statisticsService.getOrdersOfAVendor(vendorId);
        if (deliveries == null || deliveries.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<Double> count = statisticsService.getDeliveriesPerHour(deliveries);
        return ResponseEntity.ok(count);
    }

    /**
     * Returns the statistics for a specific courier
     * @param userId User ID for authorization (required)
     * @param courierId User ID for authorization (optional)
     * @param startTime  (optional)
     * @param endTime  (optional)
     * @return the statistics
     */
    @Override
    public ResponseEntity<Statistics> statisticsCourierOverviewGet(@Parameter String userId, @Parameter String courierId,@Parameter OffsetDateTime startTime, @Parameter OffsetDateTime endTime)
    {

        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);
        if(accountType.equals(UsersAuthenticationService.AccountType.CLIENT) || accountType.equals(UsersAuthenticationService.AccountType.VENDOR) || accountType.equals(UsersAuthenticationService.AccountType.ADMIN) || accountType.equals(UsersAuthenticationService.AccountType.COURIER))
        {

            Statistics statistics = statisticsService.getCourierStatistics(courierId,startTime,endTime);
            return ResponseEntity.ok(statistics);

        }

            throw  new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is unauthorized to access this method");
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
