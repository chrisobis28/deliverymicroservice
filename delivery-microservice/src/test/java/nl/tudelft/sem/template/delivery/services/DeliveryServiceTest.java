package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.*;
import org.junit.jupiter.api.Assertions;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    public transient DeliveryRepository deliveryRepositoryMock;
    @Mock
    public transient RestaurantRepository restaurantRepositoryMock;

    @Mock
    public transient GPS gpsMock;

    @InjectMocks
    public transient DeliveryService deliveryService;

    @Test
    void updatesStatusWhenUpdateDeliveryStatusCalled() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        deliveryService.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);
        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
                x.getDeliveryID().equals(deliveryId) && x.getStatus().equals(DeliveryStatus.DELIVERED)));
    }

    @Test
    void updatesStatus() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        deliveryService.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);
        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
                x.getDeliveryID().equals(deliveryId) && x.getStatus().equals(DeliveryStatus.DELIVERED)));
    }

    @Test
    void getDelivery() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        assertThat(deliveryService.getDelivery(deliveryId)).isEqualTo(delivery);
    }

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
        deliveryService.updateEstimatedPrepTime(deliveryId, prepTime);
        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
                x.getDeliveryID().equals(deliveryId)
                        && x.getEstimatedPrepTime().equals(prepTime)));

        // Assert that only prep time field changed
        assertEquals(expected, delivery);
    }

    @Test
    void updateEstimatedPrepTimeNotFound() {
        UUID invalidDeliveryId = UUID.randomUUID();
        Integer prepTime = 25;

        when(deliveryRepositoryMock.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatExceptionOfType(DeliveryService.DeliveryNotFoundException.class)
                .isThrownBy(() -> deliveryService.updateEstimatedPrepTime(invalidDeliveryId, prepTime));
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
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(restaurantRepositoryMock.findById(restaurantID)).thenReturn(Optional.of(restaurant));
        doReturn(60).when(deliveryServiceSpy).computeTimeStillInRestaurant(0, coordA, coordB);

        OffsetDateTime result = deliveryServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = delivery.getOrderTime().plusMinutes(60);

        // Asserting based on the expected behavior for PENDING status
        assertEquals(expected, result);
        verify(deliveryServiceSpy, times(1)).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
        verify(deliveryServiceSpy, never()).computeTransitTime(anyList(), anyList());
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
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(restaurantRepositoryMock.findById(restaurantID)).thenReturn(Optional.of(restaurant));
        doReturn(60).when(deliveryServiceSpy).computeTransitTime(coordA, coordB);

        OffsetDateTime result = deliveryServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = then.plusMinutes(60);

        // Asserting based on the expected behavior for GIVEN_TO_COURIER status
        assertEquals(expected, result);
        verify(deliveryServiceSpy, times(1)).computeTransitTime(anyList(), anyList());
        verify(deliveryServiceSpy, never()).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
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
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        when(gpsMock.getCurrentCoordinates()).thenReturn(coordC);
        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        doReturn(60).when(deliveryServiceSpy).computeTransitTime(coordC, coordB);

        OffsetDateTime result = deliveryServiceSpy.computeEstimatedDeliveryTime(deliveryId);
        OffsetDateTime expected = now.plusMinutes(80);

        // Asserting based on the expected behavior for ON_TRANSIT status
        assertThat(expected).isEqualToIgnoringSeconds(result);
        verify(deliveryServiceSpy, times(1)).computeTransitTime(anyList(), anyList());
        verify(deliveryServiceSpy, never()).computeTimeStillInRestaurant(anyInt(), anyList(), anyList());
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
                () -> deliveryService.computeEstimatedDeliveryTime(deliveryId)
        );
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
                () -> deliveryService.computeEstimatedDeliveryTime(deliveryId)
        );
    }

    @Test
    void computeTimeStillInRestaurant() {
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = 20;

        doReturn(10).when(deliveryServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = deliveryServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(prepTime + 10, result);
    }

    @Test
    public void computeTimeStillInRestaurantNull() {
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = null;

        doReturn(10).when(deliveryServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = deliveryServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(40, result);
    }

    @Test
    public void computeTimeStillInRestaurantZero() {
        DeliveryService deliveryServiceSpy = spy(deliveryService);

        List<Double> restaurantCoordinates = List.of(1.1, 2.2);
        List<Double> deliveryAddress = List.of(3.3, 4.4);
        Integer prepTime = 0;

        doReturn(10).when(deliveryServiceSpy).computeTransitTime(restaurantCoordinates, deliveryAddress);

        Integer result = deliveryServiceSpy.computeTimeStillInRestaurant(prepTime, restaurantCoordinates, deliveryAddress);

        assertEquals(40, result);
    }

    @Test
    void computeTransitTime() {
        Double distance = 4.72;
        // Partially mock delivery service
        DeliveryService deliveryServiceSpy = spy(deliveryService);
        doReturn(4.72).when(deliveryServiceSpy).computeHaversine(1.1, 2.2, 3.3, 4.4);

        Integer expected = 9;
        assertEquals(expected, deliveryServiceSpy.computeTransitTime(List.of(1.1, 2.2), List.of(3.3, 4.4)));
    }

    @Test
    void computeHaversine() {
        // Coordinates
        List<Double> coordA = List.of(1.1, 2.2);
        List<Double> coordB = List.of(3.3, 4.4);

        Double expected = 345.82;
        assertEquals(expected, deliveryService
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
        deliveryService.updatePickupTime(deliveryId, pickUpTime);

        verify(deliveryRepositoryMock, times(1)).save(argThat(x ->
                x.getDeliveryID().equals(deliveryId)
                        && x.getPickupTime().equals(pickUpTime)));

        assertEquals(expected, delivery);
    }

    @Test
    void getAcceptedDeliveriesCases() {
        Delivery d1 = new Delivery();
        d1.setDeliveryID(UUID.randomUUID());
        d1.setStatus(DeliveryStatus.ACCEPTED);
        d1.setCourierID(null);
        Delivery d2 = new Delivery();
        d2.setDeliveryID(UUID.randomUUID());
        d2.setStatus(DeliveryStatus.DELIVERED);
        d2.setCourierID("courier@testmail.com");
        when(deliveryRepositoryMock.findAll()).thenReturn(List.of(d1, d2));
        assertThat(deliveryService.getAcceptedDeliveries()).containsExactly(d1);
    }

    @Test
    void insertTest() {
        assertThrows(IllegalArgumentException.class, () -> deliveryService.insert(null));
        assertThrows(IllegalArgumentException.class, () -> deliveryService.insert(new Delivery()));
    }

    @Test
    void restaurantUsesOwnCouriersTest() {
        Delivery d = new Delivery();
        UUID deliveryId = UUID.randomUUID();
        d.setDeliveryID(deliveryId);
        Restaurant r1 = new Restaurant();
        r1.setCouriers(null);
        Restaurant r2 = new Restaurant();
        r1.setCouriers(List.of());
        Restaurant r3 = new Restaurant();
        r1.setCouriers(List.of());
        r2.setCouriers(null);
        r3.setCouriers(List.of("courier1@testmail.com"));
        when(restaurantRepositoryMock.findById("restaurant1@testmail.com")).thenReturn(Optional.of(r1));
        when(restaurantRepositoryMock.findById("restaurant2@testmail.com")).thenReturn(Optional.of(r2));
        when(restaurantRepositoryMock.findById("restaurant3@testmail.com")).thenReturn(Optional.of(r3));

        d.setRestaurantID("restaurant1@testmail.com");
        Assertions.assertFalse(deliveryService.restaurantUsesOwnCouriers(d));
        d.setRestaurantID("restaurant2@testmail.com");
        Assertions.assertFalse(deliveryService.restaurantUsesOwnCouriers(d));
        d.setRestaurantID("restaurant3@testmail.com");
        Assertions.assertTrue(deliveryService.restaurantUsesOwnCouriers(d));
    }
}