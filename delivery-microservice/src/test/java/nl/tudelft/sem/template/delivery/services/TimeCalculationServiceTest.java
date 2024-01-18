package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.*;
import nl.tudelft.sem.template.model.Error;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeCalculationServiceTest {

    @Mock
    public DeliveryRepository deliveryRepositoryMock;
    @Mock
    public RestaurantRepository restaurantRepositoryMock;

    @Mock
    public GPS gpsMock;

    @InjectMocks
    private TimeCalculationService sut;


    @Test
    void updateEstimatedPrepTime() {
        UUID deliveryId = UUID.randomUUID();
        Integer prepTime = 25;

        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setCourierID("courier@example.org");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setCustomerID("customer@example.org");

        Delivery expected = new Delivery();
        expected.setDeliveryID(deliveryId);
        expected.setEstimatedPrepTime(25);
        expected.setRatingRestaurant(5);
        expected.setCourierID("courier@example.org");
        expected.setStatus(DeliveryStatus.DELIVERED);
        expected.setCustomerID("customer@example.org");

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        sut.updateEstimatedPrepTime(deliveryId, prepTime);
        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
            x.getDeliveryID().equals(deliveryId)
                && x.getEstimatedPrepTime().equals(prepTime)));

        // Assert that only prep time field changed
        assertEquals(expected, delivery);
    }

    @Test
    public void testComputeEstimatedDeliveryTimePendingStatus() {
        UUID deliveryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        String restaurantID = "vendor@example.org";
        String courierID = "courier@example.org";
        String customerID = "customer@example.org";

        // Coordinates
        List<Double> coordA = List.of(1.1, 2.2);
        List<Double> coordB = List.of(3.3, 4.4);

        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.NONE);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(restaurantID);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(coordB);
        delivery.setOrderTime(now);
        delivery.setCourierID(courierID);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setCustomerID(customerID);
        delivery.setError(error);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantID);
        restaurant.setLocation(coordA);

        // Partially mock delivery service
        TimeCalculationService timeServiceSpy = spy(sut);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(restaurantRepositoryMock.findById(restaurantID)).thenReturn(Optional.of(restaurant));
        doReturn(60).when(timeServiceSpy).computeTimeStillInRestaurant(0, coordA, coordB);

        OffsetDateTime result = timeServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = delivery.getOrderTime().plusMinutes(60);

        // Asserting based on the expected behavior for PENDING status
        assertEquals(expected, result);
        verify(timeServiceSpy, times(1)).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
        verify(timeServiceSpy, never()).computeTransitTime(anyList(), anyList());
    }

    @Test
    public void testComputeEstimatedDeliveryTimeGivenToCourierStatus() {
        UUID deliveryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime then = OffsetDateTime.of(2020, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        String restaurantID = "vendor@example.org";
        String courierID = "courier@example.org";
        String customerID = "customer@example.org";

        // Coordinates
        List<Double> coordA = List.of(1.1, 2.2);
        List<Double> coordB = List.of(3.3, 4.4);

        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.OTHER);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(restaurantID);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(coordB);
        delivery.setOrderTime(now);
        delivery.setCourierID(courierID);
        delivery.setStatus(DeliveryStatus.GIVEN_TO_COURIER);
        delivery.setCustomerID(customerID);
        delivery.setPickupTime(then);
        delivery.setError(error);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantID);
        restaurant.setLocation(coordA);

        // Partially mock delivery service
        TimeCalculationService timeServiceSpy = spy(sut);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(restaurantRepositoryMock.findById(restaurantID)).thenReturn(Optional.of(restaurant));
        doReturn(60).when(timeServiceSpy).computeTransitTime(coordA, coordB);

        OffsetDateTime result = timeServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = then.plusMinutes(60);

        // Asserting based on the expected behavior for GIVEN_TO_COURIER status
        assertEquals(expected, result);
        verify(timeServiceSpy, times(1)).computeTransitTime(anyList(), anyList());
        verify(timeServiceSpy, never()).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
    }

    @Test
    public void testComputeEstimatedDeliveryTimeOnTransitStatus() {
        UUID deliveryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime then = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);
        String restaurantID = "vendor@example.org";
        String courierID = "courier@example.org";
        String customerID = "customer@example.org";

        // Coordinates
        List<Double> coordA = List.of(1.1, 2.2);
        List<Double> coordB = List.of(3.3, 4.4);
        List<Double> coordC = List.of(2.2, 3.3);

        // Define default delivery error
        Error error = new Error();
        error.setType(ErrorType.DELIVERY_DELAYED);
        error.setValue(20);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRestaurantID(restaurantID);
        delivery.setEstimatedPrepTime(0);
        delivery.setRatingRestaurant(5);
        delivery.setDeliveryAddress(coordB);
        delivery.setOrderTime(then.plusMinutes(20));
        delivery.setCourierID(courierID);
        delivery.setStatus(DeliveryStatus.ON_TRANSIT);
        delivery.setCustomerID(customerID);
        delivery.setPickupTime(then);
        delivery.setError(error);

        // Create a corresponding restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantID);
        restaurant.setLocation(coordA);

        // Partially mock delivery service
        TimeCalculationService timeServiceSpy = spy(sut);

        when(gpsMock.getCurrentCoordinates()).thenReturn(coordC);
        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        doReturn(60).when(timeServiceSpy).computeTransitTime(coordC, coordB);

        OffsetDateTime result = timeServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = now.plusMinutes(80);

        // Asserting based on the expected behavior for ON_TRANSIT status
        assertThat(expected).isEqualToIgnoringSeconds(result);
        verify(timeServiceSpy, times(1)).computeTransitTime(anyList(), anyList());
        verify(timeServiceSpy, never()).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
    }

    @Test
    public void testComputeEstimatedDeliveryTimeDeliveredStatus() {
        UUID deliveryId = UUID.randomUUID();
        OffsetDateTime then = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setOrderTime(then.plusMinutes(20));
        delivery.setPickupTime(then);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));

        DeliveryService.OrderAlreadyDeliveredException exception = assertThrows(
            DeliveryService.OrderAlreadyDeliveredException.class,
            () -> sut.computeEstimatedDeliveryTime(deliveryId)
        );
    }

    @Test
    void updateEstimatedPrepTimeNotFound() {
        UUID invalidDeliveryId = UUID.randomUUID();
        Integer prepTime = 25;

        when(deliveryRepositoryMock.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatExceptionOfType(DeliveryService.DeliveryNotFoundException.class)
            .isThrownBy(() -> sut.updateEstimatedPrepTime(invalidDeliveryId, prepTime));
    }

    @Test
    public void testComputeEstimatedDeliveryTimeRejectedStatus() {
        UUID deliveryId = UUID.randomUUID();
        OffsetDateTime then = OffsetDateTime.of(2023, 12, 13, 14, 0, 0, 0, ZoneOffset.UTC);

        // Create a Delivery object
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.REJECTED);
        delivery.setOrderTime(then.plusMinutes(20));
        delivery.setPickupTime(then);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));

        DeliveryService.OrderRejectedException exception = assertThrows(
            DeliveryService.OrderRejectedException.class,
            () -> sut.computeEstimatedDeliveryTime(deliveryId)
        );
    }

    @Test
    void computeTimeStillInRestaurant() {
        TimeCalculationService timeServiceSpy = spy(sut);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = 20;

        doReturn(10).when(timeServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = timeServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(prepTime + 10, result);
    }

    @Test
    public void computeTimeStillInRestaurantNull() {
        TimeCalculationService timeServiceSpy = spy(sut);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = null;

        doReturn(10).when(timeServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = timeServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(40, result);
    }

    @Test
    public void computeTimeStillInRestaurantZero() {
        TimeCalculationService timeServiceSpy = spy(sut);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = 0;

        doReturn(10).when(timeServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = timeServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(40, result);
    }

    @Test
    void computeTransitTime() {
        Double distance = 4.72;
        // Partially mock delivery service
        TimeCalculationService timeServiceSpy = spy(sut);
        doReturn(4.72).when(timeServiceSpy).computeHaversine(1.1, 2.2, 3.3, 4.4);

        Integer expected = 9;
        assertEquals(expected, timeServiceSpy.computeTransitTime(List.of(1.1, 2.2), List.of(3.3, 4.4)));
    }

    @Test
    void computeHaversine() {
        // Coordinates
        List<Double> coordA = List.of(1.1, 2.2);
        List<Double> coordB = List.of(3.3, 4.4);

        Double expected = 345.82;
        assertEquals(expected, sut
            .computeHaversine(coordA.get(0), coordA.get(1), coordB.get(0), coordB.get(1)), 0.01);
    }

    @Test
    void updatePickUpTime() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRatingRestaurant(5);
        delivery.setCourierID("courier@example.org");
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setCustomerID("customer@example.org");

        OffsetDateTime pickUpTime = now();

        Delivery expected = new Delivery();
        expected.setDeliveryID(deliveryId);
        expected.setPickupTime(pickUpTime);
        expected.setRatingRestaurant(5);
        expected.setCourierID("courier@example.org");
        expected.setStatus(DeliveryStatus.DELIVERED);
        expected.setCustomerID("customer@example.org");

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        sut.updatePickupTime(deliveryId, pickUpTime);

        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
            x.getDeliveryID().equals(deliveryId)
                && x.getPickupTime().equals(pickUpTime)));

        assertEquals(expected, delivery);
    }
}