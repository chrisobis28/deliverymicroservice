package nl.tudelft.sem.template.delivery.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.model.Delivery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/statistics")
@RestController
public class StatisticsController implements StatisticsApi {

    private final StatisticsService statisticsService;

    private final UsersCommunication usersCommunication;

    /**
     * Constructor
     * @param statisticsService the statistics service
     */
    @Autowired
    public StatisticsController(StatisticsService statisticsService, UsersCommunication usersCommunication) {
        this.statisticsService = statisticsService;
        this.usersCommunication = usersCommunication;
    }

    /**
     * inserts an element into the repo (internal method)
     * @param delivery delivery being inserted
     * @return the entity
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
     * @param userId used for authorization
     * @param orderIds the orders for which we want to retrieve the ratings
     * @return a list of ratings that has the same size as the orderIds list
     * if an order doesn't have a rating, we insert null instead
     */
    @GetMapping("/ratings-for-orders")
    @Override
    public ResponseEntity<List<Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId, @RequestBody List<UUID> orderIds) {
        String userType = usersCommunication.getAccountType(userId);
        if (userType.equals("admin")) {
            List<Integer> ratings = new ArrayList<>();
            for (UUID orderId : orderIds) {
                ratings.add(statisticsService.getOrderRating(orderId));
            }
            return ResponseEntity.ok(ratings);
        } else if (userType.equals("vendor") ||
                userType.equals("courier") ||
                userType.equals("client")) {
            // User lacks necessary permission levels.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.emptyList());
        } else {
            // User lacks valid authentication credentials.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }
    }

    /*
    * @RequestMapping(
    method = {RequestMethod.GET},
    value = {"/statistics/ratings-for-orders"},
    produces = {"application/json"}
  )
  default ResponseEntity<List<Integer>> statisticsRatingsForOrdersGet(@Parameter(name = "orderIds",description = "Array of order ids",required = true,in = ParameterIn.HEADER) @RequestHeader(value = "orderIds",required = true) @NotNull List<UUID> orderIds) {
    * */

    /*
    * @RequestMapping(
        method = RequestMethod.GET,
        value = "/statistics/deliveries-per-hour",
        produces = { "application/json" }
    )
    default ResponseEntity<List<Integer>> statisticsDeliveriesPerHourGet(
        @Parameter(name = "userId", description = "User ID for authorization", in = ParameterIn.HEADER) @RequestHeader(value = "userId", required = false) String userId
    ) {
    * */

    /**
     * Get a list of integers representing number of deliveries per hour
     * @param userId User ID for authorization (optional)
     * @return list of number of deliveries per hour (limiting to most recent day)
     */
    @Override
    public ResponseEntity<List<Integer>> statisticsDeliveriesPerHourGet(@Parameter String userId) {
        if (isNullOrEmpty(userId)) {
            return ResponseEntity.badRequest().build();
        }
        String type = usersCommunication.getAccountType(userId);
        if (!type.equals("vendor") && !type.equals("admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Delivery> deliveries = statisticsService.getOrdersOfAVendor(userId);
        if (deliveries == null || deliveries.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        OffsetDateTime max = deliveries.get((deliveries.size()-1)).getDeliveredTime();
        List<Delivery> mostRecent = deliveries.stream().filter(d -> d.getDeliveredTime().isAfter(max.minusHours(24)))
            .sorted(Comparator.comparing(Delivery::getDeliveredTime)).collect(Collectors.toList());

        OffsetDateTime minT = mostRecent.get(0).getDeliveredTime();
        OffsetDateTime maxT = mostRecent.get((mostRecent.size()-1)).getDeliveredTime();
        int n = 0;
        if (maxT.getDayOfMonth() == minT.getDayOfMonth()) {
            n = (maxT.getHour() - minT.getHour()) + 1;
        } else {
            n = 24 - (minT.getHour() - maxT.getHour()) + 1;
        }
        List<Integer> count = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            count.add(0);
        }

        int j = 0;
        OffsetDateTime curr = minT;
        for (int i = 0; i < mostRecent.size(); i++) {
            OffsetDateTime t = mostRecent.get(i).getDeliveredTime();
            if (Math.abs((t.getHour() - curr.getHour())) < 1 || t.getMinute() == 0) {
                int currCount = count.get(j);
                count.set(j, currCount+1);
            } else {
                if (t.getDayOfMonth() == curr.getDayOfMonth()) {
                    j += (t.getHour() - curr.getHour());
                } else {
                    int diff = (24 - curr.getHour()) + t.getHour();
                    j += diff;
                }
                int currCount = count.get(j);
                count.set(j, currCount+1);
                curr = t;
            }
        }
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
