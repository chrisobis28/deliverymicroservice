package nl.tudelft.sem.template.delivery;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
@Service
public class AddressAdapter {

  private final GPS gps;

  public AddressAdapter(GPS gps) {
    this.gps = gps;
  }

  public List<Double> convertStringAddressToDouble(List<String> address) {
    if (address == null || address.size() == 0) return null;
    StringBuilder location = new StringBuilder();
    for (String s : address) {
      location.append(s).append(" ");
    }
    location = new StringBuilder(location.toString().trim());
    return gps.getCoordinatesFromAddress(location.toString()).getLeft();
  }

  public List<String> convertDoubleToStringAddress(List<Double> coords) {
    if (coords == null || coords.size() == 0) return null;
    String location = gps.getAddressFromCoordinates(coords);
    return Arrays.asList(location.split(" "));
  }
}
