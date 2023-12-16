package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {
    private StatisticsService statisticsService;
    private UsersCommunication usersCommunication;

    private StatisticsController sut;

    String userId, userType;
    UUID orderId1;
    UUID orderId2;
    List<UUID> orderIds;

    private TestDeliveryRepository repo1;

    Delivery d1 = new Delivery();
    Delivery d2 = new Delivery();

    @BeforeEach
    void setUp() {
        // Mock data
        userId = "user@example.org";
        orderId1 = UUID.randomUUID();
        orderId2 = UUID.randomUUID();
        orderIds = List.of(orderId1, orderId2);
        d1.setDeliveryID(orderId1);
        d2.setDeliveryID(orderId2);
        repo1 = new TestDeliveryRepository();
        usersCommunication = mock(UsersCommunication.class);
        statisticsService = new StatisticsService(repo1);
        sut = new StatisticsController(statisticsService, usersCommunication);
    }


    @Test
    void statisticsRatingsForOrdersGet() {

        userType = "admin";
        d1.setRatingCourier(4);
        // Mock ratings and user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);
        //when(statisticsService.getOrderRating(orderId1)).thenReturn(4);
        //when(statisticsService.getOrderRating(orderId2)).thenReturn(null); // Simulate no rating for orderId2
        sut.insert(d1);
        sut.insert(d2);

        // Call the method
        ResponseEntity<List<Integer>> responseEntity = sut.statisticsRatingsForOrdersGet(userId, orderIds);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned ratings
        List<Integer> ratings = responseEntity.getBody();
        assert ratings != null;
        assertEquals(2, ratings.size());
        assertEquals(4, ratings.get(0).intValue()); // Rating for orderId1
        assertNull(ratings.get(1)); // No rating for orderId2

        // Verify that getOrderRating was called for each orderId
        verify(statisticsService, times(1)).getOrderRating(orderId1);
        verify(statisticsService, times(1)).getOrderRating(orderId2);
    }

    @Test
    void statisticsRatingsForOrdersGetForbidden() {

        userType = "client";

        // Mock user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<List<Integer>> responseEntity = sut.statisticsRatingsForOrdersGet(userId, orderIds);

        // Verify the response
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        // Verify the empty list is returned
        List<Integer> response = responseEntity.getBody();
        assert response != null;
        assertTrue(response.isEmpty());

        // Verify that getOrderRating was not called for any orderId
        verify(statisticsService, never()).getOrderRating(orderId1);
        verify(statisticsService, never()).getOrderRating(orderId2);
    }

    @Test
    void statisticsRatingsForOrdersGetUnauthorized() {

        userType = "non-existent";

        // Mock user type
        when(usersCommunication.getAccountType(userId)).thenReturn(userType);

        // Call the method
        ResponseEntity<List<Integer>> responseEntity = sut.statisticsRatingsForOrdersGet(userId, orderIds);

        // Verify the response
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        // Verify the empty list is returned
        List<Integer> response = responseEntity.getBody();
        assert response != null;
        assertTrue(response.isEmpty());

        // Verify that getOrderRating was not called for any orderId
        verify(statisticsService, never()).getOrderRating(orderId1);
        verify(statisticsService, never()).getOrderRating(orderId2);
    }
}