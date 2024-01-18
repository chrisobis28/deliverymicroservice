package nl.tudelft.sem.template.delivery;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.CouriersController;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.DeliveriesPostRequest;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import nl.tudelft.sem.template.model.Restaurant;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class AvailableDeliveryProxyIntegrationTest {

    @MockBean
    private UsersAuthenticationService usersAuth;
    @MockBean
    private UsersCommunication usersCommunication;

    @Autowired
    private DeliveryRepository deliveryRepository;
    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private DeliveryController deliveryController;
    @Autowired
    private CouriersController couriersController;
    @Autowired
    private AvailableDeliveryProxy availableDeliveryProxy;

    private DeliveriesPostRequest createDeliveriesPostRequest() {
        return new DeliveriesPostRequest()
                .orderId(UUID.randomUUID().toString())
                .customerId("customer-id")
                .vendorId("restaurant-id")
                .deliveryAddress(List.of(10.0, 10.0))
                .status("pending");
    }

    private Restaurant insertSampleRestaurant() {
        Restaurant restaurant = new Restaurant()
                .restaurantID("restaurant-id")
                .deliveryZone(100.0)
                .location(List.of(10.0, 10.0));
        return restaurantRepository.save(restaurant);
    }

    @BeforeEach
    void setUp() {
        insertSampleRestaurant();

        when(usersAuth.getUserAccountType("restaurant-id")).thenReturn(AccountType.VENDOR);
        when(usersAuth.getUserAccountType("courier-id")).thenReturn(AccountType.COURIER);
        when(usersAuth.getUserAccountType("own-courier-id")).thenReturn(AccountType.COURIER);
        when(usersAuth.checkUserAccessToDelivery(eq("restaurant-id"), any())).thenReturn(true);
    }

    @Test
    void updatesAndReturnsDeliveryWhenCouriersNextOrderInvoked() {
        restaurantRepository.save(new Restaurant().restaurantID("restaurant-uno"));
        Delivery delivery = new Delivery()
                .deliveryID(UUID.randomUUID())
                .restaurantID("restaurant-uno")
                .status(DeliveryStatus.ACCEPTED);
        deliveryRepository.save(delivery);
        availableDeliveryProxy.insertDelivery(delivery);

        assertThat(couriersController.couriersCourierIdNextOrderPut("courier-id").getBody())
                .extracting("deliveryID", "courierID")
                .containsExactly(delivery.getDeliveryID(), "courier-id");
    }

    @Test
    void insertThreeDeliveriesChangeStatusAndQueryNextAvailable() {

        List<Delivery> insertedDeliveries = Stream
                .generate(() -> deliveryController.deliveriesPost(createDeliveriesPostRequest()))
                .map(HttpEntity::getBody)
                .map(x -> deliveryController.deliveriesDeliveryIdStatusPut(x.getDeliveryID(), "restaurant-id", "ACCEPTED"))
                .map(HttpEntity::getBody)
                .limit(3)
                .collect(Collectors.toList());

        List<Delivery> courierDeliveries = Stream
                .generate(() -> couriersController.couriersCourierIdNextOrderPut("courier-id"))
                .limit(3)
                .map(HttpEntity::getBody)
                .collect(Collectors.toList());

        assertThat(courierDeliveries)
                .extracting("deliveryID", "courierID")
                .containsExactlyInAnyOrderElementsOf(insertedDeliveries
                        .stream()
                        .map(x -> Tuple.tuple(x.getDeliveryID(), x.getCourierID()))
                        .collect(Collectors.toList())
                );
    }

    @Test
    void deliveryRemovedFromProxyWhenCourierSet() {

        Delivery delivery = deliveryController.deliveriesPost(createDeliveriesPostRequest().status("ACCEPTED")).getBody();

        deliveryController.deliveriesDeliveryIdCourierPut(delivery.getDeliveryID(), "courier-id", "courier-id");

        assertThatThrownBy(() -> availableDeliveryProxy.getAvailableDeliveryId())
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deliveryCourierSetByVendorWithOwnCouriers() {

        Restaurant restaurant = restaurantRepository.findById("restaurant-id").get();
        restaurant.setCouriers(new ArrayList<>(List.of("own-courier-id")));
        restaurantRepository.save(restaurant);

        Delivery delivery = deliveryController.deliveriesPost(createDeliveriesPostRequest().status("ACCEPTED")).getBody();

        // Delivery from restaurant with own couriers shouldn't be put into the queue
        assertThatThrownBy(() -> availableDeliveryProxy.getAvailableDeliveryId())
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Courier cannot assign themselves an order from restaurant with own couriers
        assertThatThrownBy(() -> deliveryController.deliveriesDeliveryIdCourierPut(delivery.getDeliveryID(), "courier-id", "courier-id"))
                .extracting("status")
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Vendor can assign their courier to the delivery
        assertThat(deliveryController.deliveriesDeliveryIdCourierPut(delivery.getDeliveryID(), "restaurant-id", "own-courier-id").getBody())
                .extracting("deliveryID", "courierID")
                .containsExactly(delivery.getDeliveryID(), "own-courier-id");
    }
}
