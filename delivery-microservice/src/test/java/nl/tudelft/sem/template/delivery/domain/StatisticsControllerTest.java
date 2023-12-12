package nl.tudelft.sem.template.delivery.domain;

import nl.tudelft.sem.template.delivery.controllers.StatisticsController;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
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
    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    @Test
    void statisticsRatingsForOrdersGet() {

        // Mock data
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        List<UUID> orderIds = List.of(orderId1, orderId2);

        // Mock ratings
        when(statisticsService.getOrderRating(orderId1)).thenReturn(4);
        when(statisticsService.getOrderRating(orderId2)).thenReturn(null); // Simulate no rating for orderId2

        // Call the method
        ResponseEntity<List<Integer>> responseEntity = statisticsController.statisticsRatingsForOrdersGet(/*"user123"*/ orderIds);

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify the returned ratings
        List<Integer> ratings = responseEntity.getBody();
        assertEquals(2, ratings.size());
        assertEquals(4, ratings.get(0).intValue()); // Rating for orderId1
        assertEquals(null, ratings.get(1)); // No rating for orderId2

        // Verify that getOrderRating was called for each orderId
        verify(statisticsService, times(1)).getOrderRating(orderId1);
        verify(statisticsService, times(1)).getOrderRating(orderId2);
    }
}