package nl.tudelft.sem.template.delivery;

import java.text.DecimalFormat;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Mock of Geocoding.
 */
@Service
public class GPS {
    private final transient List<Double> other = List.of(39.00, 34.00);

    /**
     * Get current coordinates as a list of latitude and longitude.
     *
     * @return list of current coordinates
     */
    public List<Double> getCurrentCoordinates() {
        // Mock the current coordinates as the "Other" coordinates for simplicity
        DecimalFormat df = new DecimalFormat("##.###");
        List<Double> currentCoords = List.of(
                Double.parseDouble(df.format((other.get(0)))),
                Double.parseDouble(df.format((other.get(1))))
        );
        return currentCoords;
    }


}
