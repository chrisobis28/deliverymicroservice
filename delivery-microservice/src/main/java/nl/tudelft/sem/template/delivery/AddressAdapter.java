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
    String location = String.join(" ", address);
    location = location.trim();
    return gps.getCoordinatesFromAddress(location).getLeft();
  }

  public List<String> convertDoubleToStringAddress(List<Double> coords) {
    if (coords == null || coords.size() == 0) return null;
    String location = gps.getAddressFromCoordinates(coords);
    return Arrays.asList(location.split(" "));
  }
}
