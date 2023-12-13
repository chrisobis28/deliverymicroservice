package nl.tudelft.sem.template.delivery.domain.PickupDropOff;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.controllers.DeliveryController;
import nl.tudelft.sem.template.delivery.controllers.RestaurantController;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestDeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.TestRepos.TestRestaurantRepository;
import nl.tudelft.sem.template.delivery.services.DeliveryService;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class DeliveryAddressTest {

    private TestDeliveryRepository repo1;

    private DeliveryController sut1;
    private UsersCommunication userCommunication;

    private TestRestaurantRepository repo2;

    private RestaurantController sut2;

    @BeforeEach
    public void setup() {
        repo1 = new TestDeliveryRepository();
        userCommunication = new UsersCommunication();
        sut1 = new DeliveryController(new DeliveryService(repo1,repo2), userCommunication);
    }

    @Test
    void Returns_delivery_status_when_getDeliveryStatus_called() {
        Delivery delivery = new Delivery();;
        UUID deliveryID = UUID.randomUUID();
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        delivery.setDeliveryID(deliveryID);
        sut1.insert(delivery);
        List<Double> deliveryAddress = sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(), null).getBody();
        assertThat(deliveryAddress).isEqualTo(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(delivery.getDeliveryID(),null).getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void Throws_deliveryNotFound_when_deliveryId_is_invalid() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setDeliveryAddress(new ArrayList<>(Arrays.asList(100.0, 100.0)));
        sut1.insert(delivery);
        UUID invalidDeliveryId = UUID.randomUUID();
        assertThat(sut1.deliveriesDeliveryIdDeliveryAddressGet(invalidDeliveryId,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}