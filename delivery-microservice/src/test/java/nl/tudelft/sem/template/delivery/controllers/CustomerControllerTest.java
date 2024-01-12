package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.controllers.CustomerController;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.services.CustomersService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType;
import nl.tudelft.sem.template.model.Delivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@DataJpaTest
public class CustomerControllerTest {

    @Autowired
    private DeliveryRepository deliveryRepository;
    @Mock
    private UsersAuthenticationService usersAuth;
    private CustomersService customersService;
    private CustomerController customerController;

    @BeforeEach
    public void init() {
        this.customersService = new CustomersService(deliveryRepository);
        this.customerController = new CustomerController(customersService, usersAuth);
    }

    @Test
    void test_setup_works() {
        deliveryRepository.save(new Delivery().deliveryID(UUID.randomUUID()));
        assertThat(deliveryRepository.findAll()).hasSize(1);
    }

    @Test
    void returns_delivery_ids_for_user_id() {
        List<UUID> deliveryUUIDs = Stream.generate(UUID::randomUUID).limit(3).collect(Collectors.toList());
        List<Delivery> deliveries = deliveryUUIDs
                .stream()
                .map(x -> new Delivery().deliveryID(x).customerID("customer@essa.com"))
                .collect(Collectors.toList());
        deliveryRepository.saveAll(deliveries);
        when(usersAuth.getUserAccountType("customer@essa.com")).thenReturn(AccountType.CLIENT);

        assertThat(customerController.customersCustomerIdOrdersGet("customer@essa.com").getBody())
                .containsExactlyInAnyOrderElementsOf(deliveryUUIDs);
    }

    @Test
    void returns_not_found_when_user_is_not_a_customer() {
        when(usersAuth.getUserAccountType("somebody@essa.com")).thenReturn(AccountType.INVALID);

        assertThatThrownBy(() -> customerController.customersCustomerIdOrdersGet("somebody@essa.com"))
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
