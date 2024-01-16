package nl.tudelft.sem.template.delivery.services;

import javax.transaction.Transactional;
import nl.tudelft.sem.template.delivery.domain.DeliveryRepository;
import nl.tudelft.sem.template.delivery.domain.ErrorRepository;
import nl.tudelft.sem.template.model.Delivery;
import nl.tudelft.sem.template.model.Error;
import nl.tudelft.sem.template.model.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@EntityScan("nl.tudelft.sem.template.*")
@ExtendWith(MockitoExtension.class)
@Transactional
@DataJpaTest
class StatisticsServiceTest {
    @Autowired
    public DeliveryRepository deliveryRepository;
    @Autowired
    private ErrorRepository errorRepository;
    private StatisticsService statisticsService;
    Error e = new Error();
    Delivery delivery1 = new Delivery();
    OffsetDateTime date0;

    @BeforeEach
    void setUp() {
        date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));

        statisticsService = new StatisticsService(deliveryRepository);

        UUID dId = UUID.randomUUID();
        delivery1.setDeliveryID(dId);
        delivery1.setOrderTime(date0);
        e.errorId(dId).type(ErrorType.OTHER);
        errorRepository.save(e);
        delivery1.setError(e);
        deliveryRepository.save(delivery1);

    }

    @Test
    void getOrderRating() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = new Delivery();
        delivery.setDeliveryID(deliveryId);
        delivery.setRatingRestaurant(5);

        Delivery test = statisticsService.insert(delivery);
        assertEquals(test.getRatingRestaurant(), deliveryRepository.findById(deliveryId).get().getRatingRestaurant());
        assertThat(statisticsService.getOrderRating(deliveryId)).isEqualTo(5);
    }

    @Test
    void unexpectedEventTestNull1(){

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER,null,null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    }

    @Test
    void unexpectedEventTestNull2(){
        OffsetDateTime date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date0,null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    }

    @Test
    void unexpectedEventTestWrongOrder(){
        OffsetDateTime date0 = OffsetDateTime.of(2023, 12, 13, 14, 33, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date2,date1));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    }
    @Test
    void unexpectedEventTestNoDelivery(){

        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        delivery1.setOrderTime(date1);
        deliveryRepository.save(delivery1);
        Double d = statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date1,date2);
        assertEquals(0.0, d);

    }
    @Test
    void unexpectedEventTestDelivery(){
        OffsetDateTime date1 = OffsetDateTime.of(2023, 12, 13, 14, 32, 23, 0, ZoneOffset.ofHours(0));
        OffsetDateTime date2 = OffsetDateTime.of(2023, 12, 13, 15, 2, 23, 0, ZoneOffset.ofHours(0));
        Double d = statisticsService.getUnexpectedEventStatistics(ErrorType.OTHER, date1,date2);
        assertEquals(1.0, d);

    }

}