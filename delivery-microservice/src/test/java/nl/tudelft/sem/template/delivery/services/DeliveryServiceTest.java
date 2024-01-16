package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.GPS;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    public DeliveryRepository deliveryRepositoryMock;
    @Mock
    public RestaurantRepository restaurantRepositoryMock;

    @Mock
    public GPS gpsMock;

    @InjectMocks
    public DeliveryService deliveryService;

    @Test
    void returnsDeliveryStatus() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setStatus(DeliveryStatus.ACCEPTED);

        when(deliveryRepositoryMock.findById(deliveryId)).thenReturn(Optional.of(delivery));
        assertThat(deliveryService.getDeliveryStatus(deliveryId)).isEqualTo(DeliveryStatus.ACCEPTED);
    }

    @Test
    void throwsDeliveryNotFoundInvalid() {
        UUID invalidDeliveryId = UUID.randomUUID();
        when(deliveryRepositoryMock.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatExceptionOfType(DeliveryService.DeliveryNotFoundException.class)
                .isThrownBy(() -> deliveryService.getDeliveryStatus(invalidDeliveryId));
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
    void getAcceptedDeliveriesCases() {
        Delivery d1 = new Delivery();
        d1.setDeliveryID(UUID.randomUUID());
        d1.setCourierID(null);
        Delivery d2 = new Delivery();
        d2.setDeliveryID(UUID.randomUUID());
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