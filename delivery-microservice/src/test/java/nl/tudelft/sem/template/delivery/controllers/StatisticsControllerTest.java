package nl.tudelft.sem.template.delivery.controllers;


import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @Autowired
    private ErrorRepository repo2;

    Delivery d1 = new Delivery();
    Delivery d2 = new Delivery();
    Delivery d3 = new Delivery();
    Delivery d4 = new Delivery();
    Delivery d5 = new Delivery();
    Delivery d6 = new Delivery();
    Error error = new Error();



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
        error.setErrorId(orderId1);
        error.setType(ErrorType.OTHER);
        repo2.save(error);
        usersCommunication = mock(UsersAuthenticationService.class);
        StatisticsService statisticsService = new StatisticsService(repo1);
        sut = new StatisticsController(statisticsService, usersCommunication);
    }

    @Test
    void testForDeliveriesPerHrEmptyID() {
        String msg = "User ID or Restaurant ID is invalid.";
        when(usersCommunication.checkUserAccessToRestaurant(null, " ", "DPH"))
            .thenReturn(Pair.of(HttpStatus.BAD_REQUEST, msg));
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(null, " "))
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(null, " "))
            .message()
            .isEqualTo("400 BAD_REQUEST \"User ID or Restaurant ID is invalid.\"");
    }

    @Test
    void testForDeliveriesPerHrUnauthorized() {
        String msg = "User lacks necessary permissions.";
        when(usersCommunication.checkUserAccessToRestaurant(userId, userId, "DPH"))
            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));

        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId, userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void testForDeliveriesPerHrUnauthorized2() {
        String userId2 = userId.concat("impostor");
        String msg = "User lacks necessary permissions.";
        when(usersCommunication.checkUserAccessToRestaurant(userId2, userId, "DPH"))
            .thenReturn(Pair.of(HttpStatus.FORBIDDEN, msg));
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId2, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId2, userId))
            .message()
            .isEqualTo("403 FORBIDDEN \"User lacks necessary permissions.\"");
    }

    @Test
    void testForDeliveriesPerHrInvalid() {
        String userId2 = "impostor";
        String msg = "User lacks valid authentication credentials.";
        when(usersCommunication.checkUserAccessToRestaurant(userId2, userId, "DPH"))
            .thenReturn(Pair.of(HttpStatus.UNAUTHORIZED, msg));
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId2, userId))
            .extracting("status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThatThrownBy(() -> sut.statisticsDeliveriesPerHourGet(userId2, userId))
            .message()
            .isEqualTo("401 UNAUTHORIZED \"User lacks valid authentication credentials.\"");
    }

    @Test
    void testForDeliveriesPerHrNotVendor() {
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

        repo1.save(d1);
        repo1.save(d2);
        repo1.save(d3);
        repo1.save(d4);
        repo1.save(d5);
        repo1.save(d6);
        repo1.save(d7);
        repo1.save(d8);
        repo1.save(d9);

        when(usersCommunication.checkUserAccessToRestaurant(userId, userId2, "DPH"))
            .thenReturn(Pair.of(HttpStatus.OK, "OK"));

        ResponseEntity<List<Double>> actual = sut.statisticsDeliveriesPerHourGet(userId, userId2);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(6.0, Objects.requireNonNull(actual.getBody()).get(14));
    }

    @Test
    void testForDeliveriesPerHrNoDeliveredDeliveries() {
        d1.setRestaurantID(userId);
        d2.setRestaurantID(userId);
        d3.setRestaurantID(userId);
        d4.setRestaurantID(userId);
        d5.setRestaurantID(userId);
        d6.setRestaurantID(userId);
        repo1.save(d1);
        repo1.save(d2);
        repo1.save(d3);
        repo1.save(d4);
        repo1.save(d5);
        repo1.save(d6);

        //when(usersCommunication.getUserAccountType(userId)).thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersCommunication.checkUserAccessToRestaurant(userId, userId, "DPH"))
            .thenReturn(Pair.of(HttpStatus.OK, "OK"));

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
        repo1.save(d1);
        repo1.save(d2);
        repo1.save(d3);
        repo1.save(d4);
        repo1.save(d5);
        repo1.save(d6);

        //Set-up
        when(usersCommunication.checkUserAccessToRestaurant(userId, userId, "DPH"))
            .thenReturn(Pair.of(HttpStatus.OK, "OK"));

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
        repo1.save(d1);
        repo1.save(d2);

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

    @Test
    void statisticsRatingsForUnexpectedEventUnauthorized() {
        userType = UsersAuthenticationService.AccountType.INVALID;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsUnexpectedEventRateGet(userId, event, date1, date2));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void statisticsRatingsForUnexpectedEventForbiddenClient() {
        userType = UsersAuthenticationService.AccountType.CLIENT;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsUnexpectedEventRateGet(userId, event, date1, date2));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void statisticsRatingsForUnexpectedEventNullStartDate() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.OTHER;
        //OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsUnexpectedEventRateGet(userId, event, null, date2));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void statisticsRatingsForUnexpectedEventNullEndDate() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        //OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsUnexpectedEventRateGet(userId, event, date1, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void statisticsRatingsForUnexpectedEventWrongOrderOfDates() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> sut.statisticsUnexpectedEventRateGet(userId, event, date2, date1));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void statisticsRatingsForUnexpectedEventNoDeliveryInPeriod() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date0 = OffsetDateTime.of(2022, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        d1.setOrderTime(date0);
        d1.setError(error);
        repo1.save(d1);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseEntity<Double> r = sut.statisticsUnexpectedEventRateGet(userId, event, date1, date2);
        assertEquals(0.0, r.getBody());
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    void statisticsRatingsForUnexpectedEventOneDelivery() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.OTHER;
        OffsetDateTime date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        d1.setOrderTime(date0);
        d1.setError(error);
        repo1.save(d1);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseEntity<Double> r = sut.statisticsUnexpectedEventRateGet(userId, event, date1, date2);
        assertEquals(1.0, r.getBody());
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    void statisticsRatingsForUnexpectedEventNONE() {
        userType = UsersAuthenticationService.AccountType.ADMIN;
        ErrorType event = ErrorType.NONE;
        OffsetDateTime date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        d1.setOrderTime(date0);
        d1.setError(error);
        repo1.save(d1);
        when(usersCommunication.getUserAccountType(userId)).thenReturn(userType);
        ResponseEntity<Double> r = sut.statisticsUnexpectedEventRateGet(userId, event, date1, date2);
        assertEquals(0.0, r.getBody());
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }
}