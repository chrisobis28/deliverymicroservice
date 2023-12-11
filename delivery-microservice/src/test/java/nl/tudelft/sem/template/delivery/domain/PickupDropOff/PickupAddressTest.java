package nl.tudelft.sem.template.delivery.domain.PickupDropOff;

import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.delivery.services.RestaurantService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class PickupAddressTest {

    private TestDeliveryRepository repo1;

    private DeliveryController sut1;

    private TestRestaurantRepository repo2;
    private RestaurantController sut2;


    @BeforeEach
    public void setup() {
        repo2 = new TestRestaurantRepository();
        sut2 = new RestaurantController(new RestaurantService(repo2));
        repo1 = new TestDeliveryRepository();
        sut1 = new DeliveryController(new DeliveryService(repo1,repo2));
    }
    @Test
    void Returns_delivery_status_when_getDeliveryStatus_called() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);

        String restaurantId = "pizzahut@yahoo.com";
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantID(restaurantId);
        restaurant.setLocation(new ArrayList<>(Arrays.asList(100.0, 100.0)));

        delivery.setRestaurantID(restaurantId);
        sut2.insert(restaurant);
        sut1.insert(delivery);


        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdPickupLocationGet(delivery.getDeliveryID(), null).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
    }

    @Test
    void Throws_deliveryNotFound_when_deliveryId_is_invalid() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut1.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThatExceptionOfType(DeliveryService.DeliveryNotFoundException.class)
                .isThrownBy(() -> sut1.deliveriesDeliveryIdPickupLocationGet(invalidDeliveryId,null));
    }
}