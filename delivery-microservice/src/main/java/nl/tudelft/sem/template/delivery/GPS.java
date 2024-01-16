package nl.tudelft.sem.template.delivery;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Mock of Geocoding
 *
 */
@Service
public class GPS {
  List<Pair<List<Double>, String>> addresses = new ArrayList<>();

  int amsterdam_lat = 5232120;
  int amsterdam_long = 497070;
  int eindhoven_lat = 5143550;
  int delft_lat = 5201150;
  int delft_long = 435860;
  int other_lat = 39000;
  int rotterdam_lat = 5192270;
  int rotterdam_long = 447920;
  List<Double> delft_coords = List.of(52.0115, 4.3586);
  List<Double> rotterdam_coords = List.of(51.9227, 4.4792);
  List<Double> amsterdam_coords = List.of(52.3212, 4.9707);
  List<Double> eindhoven_coords = List.of(51.4355, 5.4803);

  List<Double> other = List.of(39.00, 34.00);

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
