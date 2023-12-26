package nl.tudelft.sem.template.delivery;


import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

//import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

//Mock of Geocoding
@Service
public class GPS {

  //List<Double> getCoordinatesOfAddress(List<String> address);
  List<Pair<List<Double>, String>> addresses = new ArrayList<>();

  /**
   * Return a pair containing mocked coordinates and actual location in parameter
   * @param location address to be converted to coordinates
   * @return pair of coordinates and string location
   */
  public Pair<List<Double>, String> getCoordinatesFromAddress(String location) {
    double i = addresses.size();
    if (location.contains("NL")) {
      if (location.contains("Delft")) {
        Pair<List<Double>, String> p = Pair.of(List.of(52.0115+((i/10000)), 4.3586+((i/10000))), location);
        addresses.add(p);
        return p;
      }
      else if (location.contains("Rotterdam")) {
        Pair<List<Double>, String> p = Pair.of(List.of(51.9227 + ((i / 100000)), 4.4792 + ((i / 100000))), location);
        addresses.add(p);
        return p;
      }
      else if (location.contains("Amsterdam")) {
        Pair<List<Double>, String> p = Pair.of(List.of(52.3212 + ((i / 100000)), 4.9707 + ((i / 100000))), location);
        addresses.add(p);
        return p;
      }
      else {
        Pair<List<Double>, String> p = Pair.of(List.of(51.4355+((i/100000)), 5.4803+((i/100000))), location);
        addresses.add(p);
        return p;
      }
    } else {
      Pair<List<Double>, String> p = Pair.of(List.of(39.00+((i/1000)), 34.00+((i/1000))), location);
      addresses.add(p);
      return p;
    }
  }

  /**
   * Obtain string form of location from coordinates
   * @param coords latitude/longitude form of location
   * @return location in string format
   */
  public String getAddressFromCoordinates(List<Double> coords) {
    String loc;
    if (coords.get(0) > 39.00) {
      int c = (int) (coords.get(0)*100000);
      int c2 = (int) (coords.get(1)*100000);
      if ((c - 520115) == (c2 - 43586) && (c - 520115) < addresses.size()) {
        loc = addresses.get((c - 520115)).getRight();
      } else if ((c - 519227) == (c2 - 44792) && (c - 519227) < addresses.size()) {
        loc = addresses.get((c - 519227)).getRight();
      } else if ((c - 523212) == (c2 - 49707) && (c - 523212) < addresses.size()) {
        loc = addresses.get((c - 523212)).getRight();
      } else {
        loc = addresses.get((c - 514355)).getRight();
      }
    } else {
      int c = (int) (coords.get(0)*100);
      loc = addresses.get((c - 3900)).getRight();
    }
    return loc;
  }
}
