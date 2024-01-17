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
}