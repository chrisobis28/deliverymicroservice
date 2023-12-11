package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequestMapping("/statistics")
@RestController
public class StatisticsController implements StatisticsApi {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/ratings-for-orders")
    @Override
    public ResponseEntity<List<Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId, @RequestBody List<UUID> orderIds) {

        // TODO: Authorize user id

        List<Integer> ratings = new ArrayList<>();
        for (UUID orderId : orderIds) {
            ratings.add(statisticsService.getOrderRating(orderId));
        }
        return ResponseEntity.ok(ratings);
    }
}
