package nl.tudelft.sem.template.delivery;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

//import javax.persistence.Tuple;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

//Mock of Geocoding
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
   * Return a pair containing mocked coordinates and actual location in parameter
   * @param location address to be converted to coordinates
   * @return pair of coordinates and string location
   */
  public Pair<List<Double>, String> getCoordinatesFromAddress(String location) {
    DecimalFormat df = new DecimalFormat("#.#####");
    DecimalFormat df2 = new DecimalFormat("##.#####");
    List<Double> coords;
    Pair<List<Double>, String> coord_loc_pair;
    double i = addresses.size();

    if (location.contains("NL")) {
      switch (location.split(" ")[2]) {
        case ("Delft") -> coords = delft_coords;
        case ("Rotterdam") -> coords = rotterdam_coords;
        case ("Amsterdam") -> coords = amsterdam_coords;
        default -> coords = eindhoven_coords;
      }
      coord_loc_pair = Pair.of(List.of(Double.parseDouble(df2.format((coords.get(0)+(i/100000)))), Double.parseDouble(df.format((coords.get(1)+(i/100000))))), location);
    } else {
      df = new DecimalFormat("##.###");
      coord_loc_pair = Pair.of(List.of(Double.parseDouble(df.format((other.get(0)+(i/1000)))), Double.parseDouble(df.format((other.get(1)+(i/1000))))), location);
    }
    addresses.add(coord_loc_pair);
    return coord_loc_pair;
  }

  /**
   * Obtain string form of location from coordinates
   * @param coords latitude/longitude form of location
   * @return location in string format
   */
  public String getAddressFromCoordinates(List<Double> coords) {
    String loc;
    if (coords.get(0) > 50.00) {
      int c = (int) (coords.get(0)*100000);
      int c2 = (int) (coords.get(1)*100000);
      int index = isInNL(c, c2).indexOf(0);
      switch (index) {
        case 0 -> loc = addresses.get((c - delft_lat)).getRight();
        case 1 -> loc = addresses.get((c - rotterdam_lat)).getRight();
        case 2 -> loc = addresses.get((c - amsterdam_lat)).getRight();
        default -> loc = addresses.get((c - eindhoven_lat)).getRight();
      }

    } else {
      int c = (int) (coords.get(0)*1000);
      loc = addresses.get((c - other_lat)).getRight();
    }
    return loc;
  }

  /**
   * Checks if coordinates are in NL
   * @param c check value (from latitude coordinate)
   * @param c2 check value (from longitude coordinate)
   * @return list of values indicating if coordinates are in NL
   */
  public List<Integer> isInNL(int c, int c2) {
    int check = (c - amsterdam_lat) == (c2 - amsterdam_long) && (c - amsterdam_lat) < addresses.size()?0:1;
    return List.of(isInDelft(c, c2), isInRotterdam(c, c2), check);
  }

  /**
   * Checks if coordinates are in Delft
   * @param c check value (from latitude coordinate)
   * @param c2 check value (from longitude coordinate)
   * @return integer value indicating if coordinates are in Delft
   */
  public int isInDelft(int c, int c2) {
    return (c - delft_lat) == (c2 - delft_long) && (c - delft_lat) < addresses.size()?0:1;
  }

  /**
   * Checks if coordinates are in Rotterdam
   * @param c check value (from latitude coordinate)
   * @param c2 check value (from longitude coordinate)
   * @return integer value indicating if coordinates are in Rotterdam
   */
  public int isInRotterdam(int c, int c2) {
    return (c - rotterdam_lat) == (c2 - rotterdam_long) && (c - rotterdam_lat) < addresses.size()?0:1;
  }
}
