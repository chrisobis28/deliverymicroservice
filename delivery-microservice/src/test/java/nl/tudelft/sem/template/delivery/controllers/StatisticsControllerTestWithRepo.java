package nl.tudelft.sem.template.delivery.controllers;

import nl.tudelft.sem.template.delivery.communication.UsersCommunication;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.delivery.services.StatisticsService;
import nl.tudelft.sem.template.delivery.services.UsersAuthenticationService;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import static nl.tudelft.sem.template.delivery.services.UsersAuthenticationService.AccountType.CLIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
public class StatisticsControllerTestWithRepo {
    private StatisticsService statisticsService;

    @Mock
    private UsersAuthenticationService usersAuthenticationService;
    @Autowired
    private DeliveryRepository repo1;
    @Autowired
    private RestaurantRepository repo2;
    private StatisticsController sut;

    @BeforeEach
    void setUp() {

        //usersCommunication = mock(UsersCommunication.class);
        statisticsService = new StatisticsService(repo1);
        //usersAuthenticationService = new UsersAuthenticationService(usersCommunication);
        sut = new StatisticsController(statisticsService, usersAuthenticationService);
    }

    @Test
    void unauthorizedTest() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        when(usersAuthenticationService.getUserAccountType(userID))
                .thenReturn(UsersAuthenticationService.AccountType.INVALID);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            sut.statisticsCourierOverviewGet(userID, courierId, start, end).getStatusCode();
        });

        // Assert specific status and message
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("User is unauthorized to access this method", exception.getReason());

    }

    @Test
    void authorizedEmptyTest() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        when(usersAuthenticationService.getUserAccountType(userID))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getAverageRating()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getSuccessRate()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getDeliveryTimeRatio()).isEqualTo(0);
    }
    @Test
    void authorizedEmptyTestAdmin() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        when(usersAuthenticationService.getUserAccountType(userID))
                .thenReturn(UsersAuthenticationService.AccountType.ADMIN);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getAverageRating()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getSuccessRate()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getDeliveryTimeRatio()).isEqualTo(0);
    }

    @Test
    void authorizedEmptyTestVendor() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime start = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        when(usersAuthenticationService.getUserAccountType(userID))
                .thenReturn(UsersAuthenticationService.AccountType.VENDOR);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getAverageRating()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getSuccessRate()).isEqualTo(0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start, end).getBody())
                .getDeliveryTimeRatio()).isEqualTo(0);
    }

    @Test
    void authorizedEmptyFunctional() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime end = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 30));

        OffsetDateTime start_interval = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end_interval = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));

        Delivery testDelivery = new Delivery();
        testDelivery.setCourierID(courierId);
        testDelivery.setRatingCourier(4);
        testDelivery.setStatus(DeliveryStatus.DELIVERED);
        testDelivery.setDeliveredTime(end);
        testDelivery.setDeliveryID(UUID.randomUUID());
        repo1.save(testDelivery);
        when(usersAuthenticationService.getUserAccountType(userID))
                .thenReturn(CLIENT);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);


        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getAverageRating()).isEqualTo(4);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getDeliveryTimeRatio()).isEqualTo(30);
    }

    @Test
    void authorizedFunctionalMultiple() {
        String userID = "user@user.com";
        String courierId = "dominos@dominos.com";
        OffsetDateTime outTime = OffsetDateTime.of(2022, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));

        //OffsetDateTime start1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 30));

        //OffsetDateTime start2 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end2 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 10));

        //OffsetDateTime start3 = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end3 = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHoursMinutes(0, 40));

        OffsetDateTime start_interval = OffsetDateTime.of(2023, 12, 12, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime end_interval = OffsetDateTime.of(2023, 12, 13, 15, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime second_end_interval = OffsetDateTime.of(2023, 12, 13, 14, 50, 23, 0, ZoneOffset.ofHours(0));

        Delivery testDelivery1 = new Delivery();
        testDelivery1.setCourierID(courierId);
        testDelivery1.setRatingCourier(4);
        testDelivery1.setStatus(DeliveryStatus.DELIVERED);
        testDelivery1.setDeliveredTime(end1);
        testDelivery1.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery2 = new Delivery();
        testDelivery2.setCourierID(courierId);
        testDelivery2.setRatingCourier(5);
        testDelivery2.setStatus(DeliveryStatus.DELIVERED);
        testDelivery2.setDeliveredTime(end2);
        testDelivery2.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery3 = new Delivery();
        testDelivery3.setCourierID(courierId);
        testDelivery3.setRatingCourier(3);
        testDelivery3.setStatus(DeliveryStatus.REJECTED);
        testDelivery3.setDeliveredTime(end3);
        testDelivery3.setDeliveryID(UUID.randomUUID());

        Delivery testDelivery4 = new Delivery();
        testDelivery4.setCourierID(courierId);
        testDelivery4.setRatingCourier(4);
        testDelivery4.setStatus(DeliveryStatus.DELIVERED);
        testDelivery4.setDeliveredTime(outTime);
        testDelivery4.setDeliveryID(UUID.randomUUID());


        when(usersAuthenticationService.getUserAccountType(userID)).thenReturn(CLIENT);
        when(usersAuthenticationService.getUserAccountType(courierId))
                .thenReturn(UsersAuthenticationService.AccountType.COURIER);

        repo1.save(testDelivery4);

        repo1.save(testDelivery1);
        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getAverageRating()).isEqualTo(4);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getDeliveryTimeRatio()).isEqualTo(30);

        repo1.save(testDelivery2);
        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end2)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getDeliveryTimeRatio()).isEqualTo(20);

        repo1.save(testDelivery3);
        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end3)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getSuccessRate()).isEqualTo(2.0 / 3.0);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, end_interval)
                .getBody()).getDeliveryTimeRatio()).isEqualTo(20);


        assertThat(sut.statisticsCourierOverviewGet(userID, courierId, start_interval, second_end_interval)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(
                userID, courierId, start_interval, second_end_interval)
                .getBody()).getAverageRating()).isEqualTo(4.5);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(
                userID, courierId, start_interval, second_end_interval)
                .getBody()).getSuccessRate()).isEqualTo(1);
        assertThat(Objects.requireNonNull(sut.statisticsCourierOverviewGet(
                userID, courierId, start_interval, second_end_interval)
                .getBody()).getDeliveryTimeRatio()).isEqualTo(20);

    }
}
