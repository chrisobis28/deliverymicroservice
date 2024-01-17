package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class StatisticsServiceTest {
    @Autowired
    public DeliveryRepository deliveryRepository;
    @Autowired
    private ErrorRepository errorRepository;
    private StatisticsService statisticsService;
    Error error = new Error();
    Delivery delivery1 = new Delivery();
    OffsetDateTime date0;

    Delivery d1 = new Delivery();
    Delivery d2 = new Delivery();
    Delivery d3 = new Delivery();
    Delivery d4 = new Delivery();
    Delivery d5 = new Delivery();
    Delivery d6 = new Delivery();
    String userId = "user@example.org";
    UUID orderId1 = UUID.randomUUID();
    UUID orderId2 = UUID.randomUUID();
    UUID orderId3 = UUID.randomUUID();
    UUID orderId4 = UUID.randomUUID();
    UUID orderId5 = UUID.randomUUID();
    UUID orderId6 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));

        statisticsService = new StatisticsService(deliveryRepository);

        UUID dId = UUID.randomUUID();
        delivery1.setDeliveryID(dId);
        delivery1.setOrderTime(date0);
        error.errorId(dId).type(ErrorType.OTHER);
        errorRepository.save(error);
        delivery1.setError(error);
        deliveryRepository.save(delivery1);

        // Mock data

        //orderIds = List.of(orderId1, orderId2);
        d1.setDeliveryID(orderId1);
        d2.setDeliveryID(orderId2);
        d3.setDeliveryID(orderId3);
        d4.setDeliveryID(orderId4);
        d5.setDeliveryID(orderId5);
        d6.setDeliveryID(orderId6);

    }

    @Test
    void getOrderRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRatingRestaurant(5);

        Delivery test = deliveryRepository.save(delivery);
        assertTrue(deliveryRepository.findById(deliveryId).isPresent());
        assertEquals(test.getRatingRestaurant(), deliveryRepository.findById(deliveryId).get().getRatingRestaurant());
        assertThat(statisticsService.getOrderRating(deliveryId)).isEqualTo(5);
    }

    @Test
    void deliveriesPerHr() {
        //Mock data
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date3 = OffsetDateTime.of(2023, 12, 13, 14, 3, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date4 = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date5 = OffsetDateTime.of(2023, 12, 12, 18, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date6 = OffsetDateTime.of(2023, 12, 12, 19, 32, 23, 0, ZoneOffset.ofHours(0));
        List<Double> expected = List
            .of(0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
                0.0, 0.5, 0.0, 0.0, 0.0, 0.0,
                0.0);
        d1.setStatus(DeliveryStatus.DELIVERED);
        d2.setStatus(DeliveryStatus.DELIVERED);
        d3.setStatus(DeliveryStatus.DELIVERED);
        d4.setStatus(DeliveryStatus.DELIVERED);
        d5.setStatus(DeliveryStatus.DELIVERED);
        d6.setStatus(DeliveryStatus.DELIVERED);
        d1.setRestaurantID(userId);
        d2.setRestaurantID(userId);
        d3.setRestaurantID(userId);
        d4.setRestaurantID(userId);
        d5.setRestaurantID(userId);
        d6.setRestaurantID("someNewUser@testmail.com");
        d1.setDeliveredTime(date1);
        d2.setDeliveredTime(date2);
        d3.setDeliveredTime(date3);
        d4.setDeliveredTime(date4);
        d5.setDeliveredTime(date5);
        d6.setDeliveredTime(date6);
        deliveryRepository.save(d1);
        deliveryRepository.save(d2);
        deliveryRepository.save(d3);
        deliveryRepository.save(d4);
        deliveryRepository.save(d5);
        deliveryRepository.save(d6);

        List<Delivery> input = statisticsService.getOrdersOfVendor(userId);
        assertEquals(List.of(d4, d5, d3, d1, d2), input);

        List<Double> result = statisticsService.getDeliveriesPerHour(input);
        assertEquals(expected, result);
    }

    @Test
    void deliveriesPerHr2() {
        String userId2 = userId.concat("vendor");

        d1.setStatus(DeliveryStatus.DELIVERED);
        d2.setStatus(DeliveryStatus.DELIVERED);
        d3.setStatus(DeliveryStatus.DELIVERED);
        d4.setStatus(DeliveryStatus.DELIVERED);
        d5.setStatus(DeliveryStatus.DELIVERED);
        d6.setStatus(DeliveryStatus.DELIVERED);

        Delivery d7 = new Delivery();
        d7.setDeliveryID(UUID.randomUUID());
        d7.setStatus(DeliveryStatus.ACCEPTED);
        Delivery d8 = new Delivery();
        d8.setDeliveryID(UUID.randomUUID());
        d8.setStatus(DeliveryStatus.DELIVERED);
        Delivery d9 = new Delivery();
        d9.setDeliveryID(UUID.randomUUID());
        d9.setStatus(null);

        d1.setRestaurantID(userId2);
        d2.setRestaurantID(userId2);
        d3.setRestaurantID(userId2);
        d4.setRestaurantID(userId2);
        d5.setRestaurantID(userId2);
        d6.setRestaurantID(userId2);
        d7.setRestaurantID(userId2);
        d8.setRestaurantID("some_other_vendor@testmail.com");
        d9.setRestaurantID(userId2);

        OffsetDateTime time = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));

        d1.setDeliveredTime(time);
        d2.setDeliveredTime(time);
        d3.setDeliveredTime(time);
        d4.setDeliveredTime(time);
        d5.setDeliveredTime(time);
        d6.setDeliveredTime(time);
        d7.setDeliveredTime(time);
        d8.setDeliveredTime(time);
        d9.setDeliveredTime(time);

        deliveryRepository.save(d1);
        deliveryRepository.save(d2);
        deliveryRepository.save(d3);
        deliveryRepository.save(d4);
        deliveryRepository.save(d5);
        deliveryRepository.save(d6);
        deliveryRepository.save(d7);
        deliveryRepository.save(d8);
        deliveryRepository.save(d9);

        List<Delivery> input = statisticsService.getOrdersOfVendor(userId2);
        assertTrue(input.contains(d1));
        assertTrue(input.contains(d2));
        assertTrue(input.contains(d3));
        assertTrue(input.contains(d4));
        assertTrue(input.contains(d5));
        assertTrue(input.contains(d6));

        List<Double> result = statisticsService.getDeliveriesPerHour(input);
        assertEquals(6.0, Objects.requireNonNull(result.get(14)));
    }

    @Test
    void unexpectedEventTestNull1() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void unexpectedEventTestNull2() {
        OffsetDateTime date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date0, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    }

    @Test
    void courierOverviewEmpty() {
        String courierId = "dominos@dominos.com";
        OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));

        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start, end))
            .getAverageRating()).isEqualTo(0);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start, end))
            .getSuccessRate()).isEqualTo(0);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start, end))
            .getDeliveryTimeRatio()).isEqualTo(0);
    }

    @Test
    void courierOverview() {
        String courierId = "dominos@dominos.com";
        //OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 30));

        OffsetDateTime start_interval = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end_interval = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));

        Delivery testDelivery = new Delivery();
        testDelivery.setCourierID(courierId);
        testDelivery.setRatingCourier(4);
        testDelivery.setStatus(DeliveryStatus.DELIVERED);
        testDelivery.setDeliveredTime(end);
        testDelivery.setDeliveryID(UUID.randomUUID());
        deliveryRepository.save(testDelivery);

        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval))
            .getAverageRating()).isEqualTo(4);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval))
            .getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval))
            .getDeliveryTimeRatio()).isEqualTo(30);
    }

    @Test
    void courierOverview2() {
        String courierId = "dominos@dominos.com";
        OffsetDateTime outTime = OffsetDateTime.of(2022, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));

        //OffsetDateTime start1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 30));

        //OffsetDateTime start2 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end2 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 10));

        //OffsetDateTime start3 = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end3 = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 40));

        OffsetDateTime start_interval = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end_interval = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime second_end_interval = OffsetDateTime.of(2023, 12, 13, 14, 50, 23, 0, ZoneOffset.ofHours(0));

        Delivery testDelivery1 = new Delivery();
        testDelivery1.setCourierID(courierId);
        testDelivery1.setRatingCourier(4);
        testDelivery1.setStatus(DeliveryStatus.DELIVERED);
        testDelivery1.setDeliveredTime(end1);
        testDelivery1.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery2 = new Delivery();
        testDelivery2.setCourierID(courierId);
        testDelivery2.setRatingCourier(5);
        testDelivery2.setStatus(DeliveryStatus.DELIVERED);
        testDelivery2.setDeliveredTime(end2);
        testDelivery2.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery3 = new Delivery();
        testDelivery3.setCourierID(courierId);
        testDelivery3.setRatingCourier(3);
        testDelivery3.setStatus(DeliveryStatus.REJECTED);
        testDelivery3.setDeliveredTime(end3);
        testDelivery3.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery4 = new Delivery();
        testDelivery4.setCourierID(courierId);
        testDelivery4.setRatingCourier(4);
        testDelivery4.setStatus(DeliveryStatus.DELIVERED);
        testDelivery4.setDeliveredTime(outTime);
        testDelivery4.setDeliveryID(UUID.randomUUID());

        deliveryRepository.save(testDelivery4);

        deliveryRepository.save(testDelivery1);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getAverageRating()).isEqualTo(4);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getDeliveryTimeRatio()).isEqualTo(30);

        deliveryRepository.save(testDelivery2);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getDeliveryTimeRatio()).isEqualTo(20);

        deliveryRepository.save(testDelivery3);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getSuccessRate()).isEqualTo(2.0 / 3.0);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval, end_interval)
            ).getDeliveryTimeRatio()).isEqualTo(20);


        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval,
                second_end_interval)).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(courierId, start_interval,
                second_end_interval)).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(statisticsService.getCourierStatistics(
                courierId, start_interval, second_end_interval))
            .getDeliveryTimeRatio()).isEqualTo(20);
    }

    @Test
    void unexpectedEventTestWrongOrder() {
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date2, date1));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    }

    @Test
    void unexpectedEventTestNoDelivery() {

        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        delivery1.setOrderTime(date1);
        deliveryRepository.save(delivery1);
        Double d = statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date1, date2);
        assertEquals(0.0, d);

    }

    @Test
    void unexpectedEventTestDelivery() {
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        Double d = statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date1, date2);
        assertEquals(1.0, d);
    }

}