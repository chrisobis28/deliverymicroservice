package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.ErrorType;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import nl.tudelft.sem.template.model.Statistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;


@RestController
public class StatisticsController implements StatisticsApi {

    private final transient StatisticsService statisticsService;

    private final transient UsersAuthenticationService usersCommunication;

    /**
     * Constructor for statistics controller.
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
     * Gets the restaurant rating given to orders.
     *
     * @param userId   used for authorization
     * @param orderIds the orders for which we want to retrieve the ratings
     * @return a list of ratings that has the same size as the orderIds list
     *         if an order doesn't have a rating, we insert null instead
     */
    @Override
    public ResponseEntity<Map<String, Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId,
                                                                              @RequestBody List<UUID> orderIds) {
        UsersAuthenticationService.AccountType userType = usersCommunication.getUserAccountType(userId);
        // After second consideration I changed the permission levels of this endpoint,
        // so that everyone can see the ratings for transparency reasons.
        if (!userType.equals(UsersAuthenticationService.AccountType.INVALID)) {
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
    public ResponseEntity<List<Double>> statisticsDeliveriesPerHourGet(@Parameter String userId,
                                                                       @Parameter String vendorId) {
        Pair<HttpStatus, String> result = usersCommunication.checkUserAccessToRestaurant(userId, vendorId,
            "DPH");
        if (!(result.getLeft()).equals(HttpStatus.OK)) {
            throw new ResponseStatusException(result.getLeft(), result.getRight());
        }
        List<Delivery> deliveries = statisticsService.getOrdersOfVendor(vendorId);
        if (deliveries.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<Double> count = statisticsService.getDeliveriesPerHour(deliveries);
        return ResponseEntity.ok(count);
    }

    /**
     * Returns the statistics for a specific courier.
     *
     * @param userId    User ID for authorization (required)
     * @param courierId User ID for authorization (optional)
     * @param startTime (optional)
     * @param endTime   (optional)
     * @return the statistics
     */
    @Override
    public ResponseEntity<Statistics> statisticsCourierOverviewGet(@Parameter String userId,
                                                                   @Parameter String courierId,
                                                                   @Parameter OffsetDateTime startTime,
                                                                   @Parameter OffsetDateTime endTime) {
        if (!usersCommunication.getUserAccountType(courierId)
                .equals(UsersAuthenticationService.AccountType.COURIER)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such courier");
        }
        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userId);

        if (!accountType.equals(UsersAuthenticationService.AccountType.INVALID)) {
            Statistics statistics = statisticsService.getCourierStatistics(courierId, startTime, endTime);
            return ResponseEntity.ok(statistics);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is unauthorized to access this method");
    }

    /**
     * Statistics for the rate of a certain unexpected event in a time period.
     *
     * @param userID User ID for authorization (required)
     * @param unexpectedEvent Enum type of the unexpected event (required)
     * @param startTime  (optional)
     * @param endTime  (optional)
     * @return the rate of that event
     */
    @Override
    public ResponseEntity<Double> statisticsUnexpectedEventRateGet(@RequestHeader
                                                                   @NotNull String userID,
                                                               @RequestParam
                                                                   @NotNull @Valid ErrorType unexpectedEvent,
                                                               @RequestParam
                                                                   @DateTimeFormat @Valid OffsetDateTime startTime,
                                                               @RequestParam
                                                                   @DateTimeFormat @Valid OffsetDateTime endTime) {

        UsersAuthenticationService.AccountType accountType = usersCommunication.getUserAccountType(userID);
        if (accountType.equals(UsersAuthenticationService.AccountType.ADMIN)) {

            Double statistics = statisticsService.getUnexpectedEventStatistics(unexpectedEvent, startTime, endTime);
            return ResponseEntity.status(HttpStatus.OK).body(statistics);

        }
        if (accountType.equals(UsersAuthenticationService.AccountType.INVALID)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is unauthorized to access this method");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User doesn't have the necessary role to view this");

    }

}
