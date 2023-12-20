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

import java.util.*;

@RequestMapping("/statistics")
@RestController
public class StatisticsController implements StatisticsApi {

    private final StatisticsService statisticsService;

    private final UsersCommunication usersCommunication;

    /**
     * Constructor for statistics controller
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

    /**
     * Get no. of deliveries made in each hour bracket of a day averaged over no. of days delivered for
     * e.g., if there is only data for 13/12/2023 and 12/12/2023 then delivery count is averaged over 2 days
     * @param userId User ID for authorization
     * @param vendorId ID of the vendor whose delivery stats are being queried
     * @return list of doubles representing avg deliveries in each hr bracket
     */
    @Override
    public ResponseEntity<List<Double>> statisticsDeliveriesPerHourGet(@Parameter String userId, @Parameter String vendorId) {
        if (isNullOrEmpty(userId) || isNullOrEmpty(vendorId)) {
            return ResponseEntity.badRequest().build();
        }
        String type = usersCommunication.getAccountType(userId);
        boolean isVendor = type.equals("vendor") && vendorId.equals(userId);
        if (!isVendor && !type.equals("admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Delivery> deliveries = statisticsService.getOrdersOfAVendor(vendorId);
        if (deliveries == null || deliveries.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<Double> count = getDeliveriesPerHour(deliveries);
        return ResponseEntity.ok(count);
    }

    /**
     * Calculates the trend of deliveries per hour
     * @param deliveries list of all deliveries of a specific courier
     * @return list of doubles representing avg deliveries in each hr bracket
     */
    public List<Double> getDeliveriesPerHour(List<Delivery> deliveries) {
        List<Double> count = new ArrayList<>();
        List<List<Delivery>> deliveriesByHr = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            deliveriesByHr.add(new ArrayList<>());
        }

        for (Delivery d: deliveries) {
            int hr_delivered = d.getDeliveredTime().getHour();
            deliveriesByHr.get(hr_delivered).add(d);
        }

        int n = deliveries.size()-1;
        int days = (deliveries.get(n).getDeliveredTime().getDayOfYear() - deliveries.get(0).getDeliveredTime().getDayOfYear()) + 1;
        for (List<Delivery> del: deliveriesByHr) {
            //double days = (double) del.stream().map(d -> d.getDeliveredTime().getDayOfMonth()).distinct().count();
            double d = del.size()/((double)days);
            count.add(d);
        }

        return count;
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
