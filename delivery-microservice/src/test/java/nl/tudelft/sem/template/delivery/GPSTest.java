package nl.tudelft.sem.template.delivery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GPSTest {

    @Test
    void getCurrentCoordinates() {
        GPS gps = new GPS();
        List<Double> expected = List.of(39.00, 34.00);

        List<Double> result = gps.getCurrentCoordinates();
        assertEquals(expected.get(0), result.get(0), 0.001);
        assertEquals(expected.get(1), result.get(1), 0.001);
    }
}