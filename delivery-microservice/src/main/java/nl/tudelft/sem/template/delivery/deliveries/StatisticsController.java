package nl.tudelft.sem.template.delivery.deliveries;

import nl.tudelft.sem.template.api.StatisticsApi;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequestMapping("/statistics")
@RestController
public class StatisticsController implements StatisticsApi {

    private final DeliveryDao deliveryDao;

    public StatisticsController(DeliveryDao deliveryDao) {
        this.deliveryDao = deliveryDao;
    }

    @GetMapping("/ratings-for-orders")
    @Override
    public ResponseEntity<List<Integer>> statisticsRatingsForOrdersGet(@RequestHeader String userId, @RequestBody List<UUID> orderIds) {

        // TODO: Authorize user id

        List<Integer> ratings = new ArrayList<>();
        for (UUID orderId : orderIds) {
            ratings.add(deliveryDao.getOrderRating(orderId));
        }
        return ResponseEntity.ok(ratings);
    }
}
