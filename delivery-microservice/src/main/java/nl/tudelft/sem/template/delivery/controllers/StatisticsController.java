package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
}
