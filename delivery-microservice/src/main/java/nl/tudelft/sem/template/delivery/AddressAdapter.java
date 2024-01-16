package nl.tudelft.sem.template.delivery;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class AddressAdapter {

    private final GPS gps;

    public AddressAdapter(GPS gps) {
        this.gps = gps;
    }

    /**
     * Converts a string representation of address to coordinates.
     *
     * @param address a list of string fields (country, city, street, etc.)
     * @return a list of latitude and longitude
     */
    public List<Double> convertStringAddressToDouble(List<String> address) {
        if (address == null || address.size() == 0) {
            return null;
        }
        String location = String.join(" ", address);
        location = location.trim();
        return gps.getCoordinatesFromAddress(location).getLeft();
    }

    /**
     * Converts coordinates to address.
     *
     * @param coords a list of latitude and longitude
     * @return a string (human-readable) representation of a location
     */
    public List<String> convertDoubleToStringAddress(List<Double> coords) {
        if (coords == null || coords.size() == 0) {
            return null;
        }
        String location = gps.getAddressFromCoordinates(coords);
        return Arrays.asList(location.split(" "));
    }
}
