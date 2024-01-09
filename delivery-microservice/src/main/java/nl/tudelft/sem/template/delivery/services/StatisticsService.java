package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Statistics;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final DeliveryRepository deliveryRepository;

    /**
     * Constructor
     *
     * @param deliveryRepository the repository storing delivery information
     */
    public StatisticsService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Internal method for inserting delivery into repository (used in testing)
     *
     * @param delivery - Delivery object saved in repo
     * @return Delivery object
     */
    public Delivery insert(Delivery delivery) {
        if (delivery == null) {
            throw new IllegalArgumentException();
        }
        return deliveryRepository.save(delivery);
    }

    /**
     * Gets the restaurant rating of a given order
     *
     * @param deliveryId - the order/delivery id
     * @return an integer from 1 to 5 that represents the rating
     * if no rating is given, returns null
     */
    public Integer getOrderRating(UUID deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(DeliveryService.DeliveryNotFoundException::new);
        return delivery.getRatingRestaurant();
    }

    /**
     * Get all fulfilled deliveries of a specific vendor
     *
     * @param userId vendor's email
     * @return list of deliveries ordered by delivery time
     */
    public List<Delivery> getOrdersOfAVendor(String userId) {
        List<Delivery> vendorDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> userId.equals(d.getRestaurantID())).collect(Collectors.toList());
        List<Delivery> delivered = vendorDeliveries.stream().filter(d -> d.getStatus() != null)
                .filter(d -> d.getStatus().equals(DeliveryStatus.DELIVERED)).collect(Collectors.toList());
        return delivered.stream().sorted(Comparator.comparing(Delivery::getDeliveredTime)).collect(Collectors.toList());
    }

    /**
     * Calculates the trend of deliveries per hour
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
            int hr_delivered = d.getDeliveredTime().getHour();
            deliveriesByHr.get(hr_delivered).add(d);
        }

        int n = deliveries.size() - 1;
        int days = (deliveries.get(n).getDeliveredTime().getDayOfYear() - deliveries.get(0).getDeliveredTime().getDayOfYear()) + 1;
        for (List<Delivery> del : deliveriesByHr) {
            //double days = (double) del.stream().map(d -> d.getDeliveredTime().getDayOfMonth()).distinct().count();
            double d = del.size() / ((double) days);
            count.add(d);
        }

        return count;
    }

    public Statistics getCourierStatistics(String courierID, OffsetDateTime startTime,OffsetDateTime endTime) {
        List<Delivery> courierDeliveries = getSuccessfulDeliveries(courierID)
                .stream()
                .filter(delivery -> delivery.getDeliveredTime().isAfter(startTime) &&
                        delivery.getDeliveredTime().isBefore(endTime)).collect(Collectors.toList());
        Statistics statistics = new Statistics();
        //Average Rating
        double averageRating = 0.0;
        averageRating = courierDeliveries.stream().mapToInt(Delivery::getRatingCourier).average().orElse(0);

        //DeliveryTimeRatio
        double averageDeliveryTime = 0.0;
        averageDeliveryTime = courierDeliveries.stream()
                .mapToDouble(delivery -> delivery.getDeliveredTime().getOffset().getTotalSeconds() / 60.0)
                .average()
                .orElse(0.0);

        //SuccessRate
        List<Delivery> successfulDeliveries = getSuccessfulDeliveries(courierID);
        List<Delivery> rejectedDeliveries = getRejectedDeliveries(courierID);


        List<Delivery> filteredSuccessfulDeliveries = successfulDeliveries.stream()
                .filter(delivery -> delivery.getDeliveredTime().isAfter(startTime) &&
                        delivery.getDeliveredTime().isBefore(endTime))
                .collect(Collectors.toList());


        List<Delivery> filteredRejectedDeliveries = rejectedDeliveries.stream()
                .filter(delivery -> delivery.getDeliveredTime().isAfter(startTime) &&
                        delivery.getDeliveredTime().isBefore(endTime))
                .collect(Collectors.toList());


        // Calculate total deliveries
        long totalDeliveries = filteredSuccessfulDeliveries.size() + filteredRejectedDeliveries.size();

        // Calculate average success rate
        double averageSuccessRate = totalDeliveries > 0 ?
                (double) filteredSuccessfulDeliveries.size() / totalDeliveries :
                0.0;


        statistics.setAverageRating(averageRating);
        statistics.setSuccessRate(averageSuccessRate);
        statistics.setDeliveryTimeRatio(averageDeliveryTime);

        return statistics;
    }

    /**
     * Returns the successful deliveries of a courier
     *
     * @param courierId of the courier
     * @return the deliveries
     */
    public List<Delivery> getSuccessfulDeliveries(String courierId) {
        return deliveryRepository.findAllByCourierID(courierId).stream().filter(x -> x.getStatus().equals(DeliveryStatus.DELIVERED)).collect(Collectors.toList());
    }
   /**
     * Returns the rejected deliveries of a courier
     * @param courierId of the courier
     * @return the deliveries
     */
public List<Delivery> getRejectedDeliveries(String courierId)
{
    return deliveryRepository.findAllByCourierID(courierId).stream().filter(x->x.getStatus().equals(DeliveryStatus.REJECTED)).collect(Collectors.toList());
}

}
