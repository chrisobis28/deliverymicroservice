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

  //List<Double> getCoordinatesOfAddress(List<String> address);
  List<Pair<List<Double>, String>> addresses = new ArrayList<>();

  /**
   * Return a pair containing mocked coordinates and actual location in parameter
   * @param location address to be converted to coordinates
   * @return pair of coordinates and string location
   */
  public Pair<List<Double>, String> getCoordinatesFromAddress(String location) {
    DecimalFormat df = new DecimalFormat("#.#####");
    DecimalFormat df2 = new DecimalFormat("##.#####");
    double i = addresses.size();
    if (location.contains("NL")) {
      if (location.contains("Delft")) {
        Pair<List<Double>, String> p = Pair.of(List.of(Double.parseDouble(df2.format((52.0115+(i/10000)))), Double.parseDouble(df.format((4.3586+(i/10000))))), location);
        addresses.add(p);
        return p;
      }
      else if (location.contains("Rotterdam")) {
        Pair<List<Double>, String> p = Pair.of(List.of(Double.parseDouble(df2.format((51.9227 + (i / 100000)))), Double.parseDouble(df.format((4.4792 + (i / 100000))))), location);
        addresses.add(p);
        return p;
      }
      else if (location.contains("Amsterdam")) {
        Pair<List<Double>, String> p = Pair.of(List.of(Double.parseDouble(df2.format((52.3212 + (i / 100000)))), Double.parseDouble(df.format((4.9707 + (i / 100000))))), location);
        addresses.add(p);
        return p;
      }
      else {
        Pair<List<Double>, String> p = Pair.of(List.of(Double.parseDouble(df2.format((51.4355+(i/100000)))), Double.parseDouble(df.format((5.4803+(i/100000))))), location);
        addresses.add(p);
        return p;
      }
    } else {
      df = new DecimalFormat("##.###");
      Pair<List<Double>, String> p = Pair.of(List.of(Double.parseDouble(df.format((39.00+(i/1000)))), Double.parseDouble(df.format((34.00+(i/1000))))), location);
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
    if (coords.get(0) > 50.00) {
      int c = (int) (coords.get(0)*100000);
      int c2 = (int) (coords.get(1)*100000);
      if ((c - 5201150) == (c2 - 435860) && (c - 5201150) < addresses.size()) {
        loc = addresses.get((c - 5201150)).getRight();
      } else if ((c - 5192270) == (c2 - 447920) && (c - 5192270) < addresses.size()) {
        loc = addresses.get((c - 5192270)).getRight();
      } else if ((c - 5232120) == (c2 - 497070) && (c - 5232120) < addresses.size()) {
        loc = addresses.get((c - 5232120)).getRight();
      } else {
        loc = addresses.get((c - 5143550)).getRight();
      }
    } else {
      int c = (int) (coords.get(0)*1000);
      loc = addresses.get((c - 39000)).getRight();
    }
    return loc;
  }
}
