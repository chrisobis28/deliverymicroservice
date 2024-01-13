package nl.tudelft.sem.template.delivery.controllers;


import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class StatisticsControllerTest {
    @Mock
    private UsersAuthenticationService usersCommunication;

    private StatisticsController sut;

    String userId;
    UsersAuthenticationService.AccountType userType;
    UUID orderId1;
    UUID orderId2;
    UUID orderId3;
    UUID orderId4;
    UUID orderId5;
    UUID orderId6;
    List<UUID> orderIds;
    @Autowired
    private DeliveryRepository repo1;

    Delivery d1 = new Delivery();
    Delivery d2 = new Delivery();
    Delivery d3 = new Delivery();
    Delivery d4 = new Delivery();
    Delivery d5 = new Delivery();
    Delivery d6 = new Delivery();

    @BeforeEach
    void setUp() {
        // Mock data
        userId = "user@example.org";
        orderId1 = UUID.randomUUID();
        orderId2 = UUID.randomUUID();
        orderId3 = UUID.randomUUID();
        orderId4 = UUID.randomUUID();
        orderId5 = UUID.randomUUID();
        orderId6 = UUID.randomUUID();
        orderIds = List.of(orderId1, orderId2);
        d1.setDeliveryID(orderId1);
        d2.setDeliveryID(orderId2);
        d3.setDeliveryID(orderId3);
        d4.setDeliveryID(orderId4);
        d5.setDeliveryID(orderId5);
        d6.setDeliveryID(orderId6);
        usersCommunication = mock(UsersAuthenticationService.class);
        StatisticsService statisticsService = new StatisticsService(repo1);
        sut = new StatisticsController(statisticsService,usersCommunication);
    }

    @Test
    void testForDeliveriesPerHrEmptyID() {

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet("", "");
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
    }

    @Test
    void testForDeliveriesPerHrUnauthorized() {
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId, userId);
        assertEquals(HttpStatus.UNAUTHORIZED, actual.getStatusCode());
    }

    @Test
    void testForDeliveriesPerHrUnauthorized2() {
        String userId2 = userId.concat("impostor");
        when(usersCommunication.getUserAccountType(userId2)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId2, userId);
        assertEquals(HttpStatus.UNAUTHORIZED, actual.getStatusCode());
    }

    @Test
    void testForDeliveriesPerHrNotVendor() {
        d1.setStatus(DeliveryStatus.DELIVERED);
        d2.setStatus(DeliveryStatus.DELIVERED);
        d3.setStatus(DeliveryStatus.DELIVERED);
        d4.setStatus(DeliveryStatus.DELIVERED);
        d5.setStatus(DeliveryStatus.DELIVERED);
        d6.setStatus(DeliveryStatus.DELIVERED);
        sut.insert(d1);
        sut.insert(d2);
        sut.insert(d3);
        sut.insert(d4);
        sut.insert(d5);
        sut.insert(d6);

        String userId2 = userId.concat("vendor");
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.ADMIN);

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId, userId2);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(new ArrayList<>(), actual.getBody());
    }

    @Test
    void testForDeliveriesPerHrNoDeliveredDeliveries() {
        d1.setRestaurantID(userId);
        d2.setRestaurantID(userId);
        d3.setRestaurantID(userId);
        d4.setRestaurantID(userId);
        d5.setRestaurantID(userId);
        d6.setRestaurantID(userId);
        sut.insert(d1);
        sut.insert(d2);
        sut.insert(d3);
        sut.insert(d4);
        sut.insert(d5);
        sut.insert(d6);

        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId, userId);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(new ArrayList<>(), actual.getBody());
    }

    @Test
    void testForDeliveriesPerHr() {
        //Mock data
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date3 = OffsetDateTime.of(2023, 12, 13, 14, 3, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date4 = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date5 = OffsetDateTime.of(2023, 12, 12, 18, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date6 = OffsetDateTime.of(2023, 12, 12, 19, 32, 23, 0, ZoneOffset.ofHours(0));
        List<Double> expected = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0);
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
        d6.setRestaurantID(userId);
        d1.setDeliveredTime(date1);
        d2.setDeliveredTime(date2);
        d3.setDeliveredTime(date3);
        d4.setDeliveredTime(date4);
        d5.setDeliveredTime(date5);
        d6.setDeliveredTime(date6);
        sut.insert(d1);
        sut.insert(d2);
        sut.insert(d3);
        sut.insert(d4);
        sut.insert(d5);
        sut.insert(d6);

        //Set-up
        when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId, userId);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(expected, actual.getBody());
    }

    @Test
    void statisticsRatingsForOrdersGet() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        d1.setRatingRestaurant(4);
        d2.setStatus(DeliveryStatus.ACCEPTED);
        // Mock ratings and user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        sut.insert(d1);
        sut.insert(d2);

        // Call the method
        ResponseEntity<Map<String, Integer>> responseEntity = sut.statisticsRatingsForOrdersGet(userId, orderIds);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned ratings
        Map<String, Integer> ratings = responseEntity.getBody();
        assert ratings != null;
        assertEquals(2, ratings.size());
        assertEquals(4, ratings.get(orderId1.toString())); // Rating for orderId1
        assertNull(ratings.get(orderId2.toString())); // Rating for orderId2
    }


    @Test
    void statisticsRatingsForOrdersGetUnauthorized() {
        userType = UsersAuthenticationService.AccountType.INVALID;

        // Mock user type
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);

        // Verify it throws an error
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsRatingsForOrdersGet(userId, orderIds));

        // Verify the status code and error message
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"", exception.getMessage());
    }
}