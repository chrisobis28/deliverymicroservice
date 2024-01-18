package nl.tudelft.sem.template.delivery.services;


import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.ErrorType;
import nl.tudelft.sem.template.model.Statistics;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class StatisticsService {

    private final transient DeliveryRepository deliveryRepository;

    /**
     * Constructor.
     *
     * @param deliveryRepository the repository storing delivery information
     */
    public StatisticsService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }


    /**
     * Gets the restaurant rating of a given order.
     *
     * @param deliveryId - the order/delivery id
     * @return an integer from 1 to 5 that represents the rating if no rating is given, returns null
     */
    public Integer getOrderRating(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        return delivery.getRatingRestaurant();
    }

    /**
     * Get all fulfilled deliveries of a specific vendor.
     *
     * @param userId vendor's email
     * @return list of deliveries ordered by delivery time
     */
    public List<Delivery> getOrdersOfVendor(String userId) {
        List<Delivery> delivered = deliveryRepository.findAllByRestaurantIDAndStatus(userId, DeliveryStatus.DELIVERED);
        return delivered.stream().sorted(Comparator.comparing(Delivery::getDeliveredTime)).collect(Collectors.toList());
    }

    /**
     * Calculates the trend of deliveries per hour.
     *
     * @param deliveries list of all deliveries of a specific courier
     * @return list of doubles representing avg deliveries in each hr bracket
     */
    public List<Double> getDeliveriesPerHour(List<Delivery> deliveries) {
        List<Double> count = new ArrayList<>();
        List<List<Delivery>> deliveriesByHr = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            deliveriesByHr.add(new ArrayList<>());
        }

        for (Delivery d : deliveries) {
            int hrDelivered = d.getDeliveredTime().getHour();
            deliveriesByHr.get(hrDelivered).add(d);
        }

        int n = deliveries.size() - 1;
        int days = (deliveries.get(n).getDeliveredTime().getDayOfYear()
                - deliveries.get(0).getDeliveredTime().getDayOfYear()) + 1;
        for (List<Delivery> del : deliveriesByHr) {
            double d = del.size() / ((double) days);
            count.add(d);
        }

        return count;
    }

    /**
     * Gets a statistic overview of a given courier.
     *
     * @param courierId ID of a courier
     * @param startTime start time of statistic scope
     * @param endTime end time of statistic scope
     * @return a statistic object with different stats parameters
     */
    public Statistics getCourierStatistics(String courierId, OffsetDateTime startTime, OffsetDateTime endTime) {
        List<Delivery> courierDeliveries = deliveryRepository.findAllByCourierIDAndStatus(courierId,
            DeliveryStatus.DELIVERED, startTime, endTime);
        Statistics statistics = new Statistics();
        double averageRating = courierDeliveries.stream()
            .mapToInt(Delivery::getRatingCourier).average().orElse(0);
        double averageDeliveryTime = courierDeliveries.stream()
                .mapToDouble(delivery -> delivery.getDeliveredTime().getOffset().getTotalSeconds() / 60.0)
                .average()
                .orElse(0.0);
        List<Delivery> filteredRejectedDeliveries = deliveryRepository.findAllByCourierIDAndStatus(courierId,
            DeliveryStatus.REJECTED, startTime, endTime);
        long totalDeliveries = courierDeliveries.size() + filteredRejectedDeliveries.size();
        double averageSuccessRate = totalDeliveries > 0
                ? (double) courierDeliveries.size() / totalDeliveries : 0.0;
        statistics.setAverageRating(averageRating);
        statistics.setSuccessRate(averageSuccessRate);
        statistics.setDeliveryTimeRatio(averageDeliveryTime);
        return statistics;
    }

    /**
     * Calculates the rate of the specific error type in a time frame.
     *
     * @param unexpectedEvent the type of the error
     * @param startTime the beginning of the period
     * @param endTime the end of the period
     * @return the rate
     */
    public Double getUnexpectedEventStatistics(ErrorType unexpectedEvent, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more fields were null");
        }
        if (startTime.isAfter(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a correct input");
        }
        List<ErrorType> list = deliveryRepository.findAllByOrderTime(startTime, endTime)
            .stream().map(d -> d.getError().getType()).collect(Collectors.toList());
        double count = (double) list.stream().filter(e -> Objects.equals(e, unexpectedEvent)).count();
        return Double.isNaN(count / (double) list.size()) ? 0.0 : count / (double) list.size();
    }
}
