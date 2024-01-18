package nl.tudelft.sem.template.delivery.services;

import nl.tudelft.sem.template.delivery.domain.RestaurantRepository;
import nl.tudelft.sem.template.model.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest

class UpdateRestaurantServiceTest {

    @Autowired
    private RestaurantRepository rr;
    private UpdateRestaurantService rs;

    /**
     * Set up.
     */
    @BeforeEach
    public void setup() {
        rs = new UpdateRestaurantService(rr);
    }

    @Test
    public void getRestaurantNullTest() {
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1, 12.2));
        rr.save(r);

        assertThrows(RestaurantService.RestaurantNotFoundException.class,
            () -> rs.getRestaurant(null));
    }

    @Test
    public void setListOfCouriersTest() {
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1, 12.2));
        List<String> list = new ArrayList<>();
        list.add("courier1@testmail.com");
        rr.save(r);
        rs.setListOfCouriers("bla", list);
        assertThat(rs.getRestaurant("bla").getCouriers()).isEqualTo(list);

        list.add("courier2@testmail.com");
        Restaurant rest = rs.setListOfCouriers("bla", list);
        assertEquals(rest.getCouriers(), list);
    }

    @Test
    public void setDeliveryZoneTest() {
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1, 12.2));
        r.setDeliveryZone(20.0);
        rr.save(r);
        assertThat(rs.getRestaurant("bla").getDeliveryZone()).isEqualTo(20.0);

        rs.updateDeliverZone("bla", 35.0);
        Restaurant rest = rs.getRestaurant("bla");
        assertEquals(rest.getDeliveryZone(), 35.0);
    }

    @Test
    public void setLocationTest() {
        Restaurant r = new Restaurant();
        r.setRestaurantID("bla");
        r.setLocation(List.of(11.1, 12.2));
        r.setDeliveryZone(20.0);
        rr.save(r);
        assertThat(rs.getRestaurant("bla").getLocation()).isEqualTo(List.of(11.1, 12.2));

        rs.updateLocation("bla", List.of(133.1, 121.2));
        Restaurant rest = rs.getRestaurant("bla");
        assertEquals(rest.getLocation(), List.of(133.1, 121.2));
    }
}