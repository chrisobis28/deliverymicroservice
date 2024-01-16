package nl.tudelft.sem.template.delivery;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;


/**
 * Mock of Geocoding.
 */
@Service
public class GPS {
    private transient List<Pair<List<Double>, String>> addresses = new ArrayList<>();

    transient int amsterdamLat = 5232120;
    transient int amsterdamLong = 497070;
    transient int eindhovenLat = 5143550;
    transient int delftLat = 5201150;
    transient int delftLong = 435860;
    transient int otherLat = 39000;
    transient int rotterdamLat = 5192270;
    transient int rotterdamLong = 447920;
    transient List<Double> delftCoords = List.of(52.0115, 4.3586);
    transient List<Double> rotterdamCoords = List.of(51.9227, 4.4792);
    transient List<Double> amsterdamCoords = List.of(52.3212, 4.9707);
    transient List<Double> eindhovenCoords = List.of(51.4355, 5.4803);

    transient List<Double> other = List.of(39.00, 34.00);

    /**
     * Return a pair containing mocked coordinates and actual location in parameter.
     *
     * @param location address to be converted to coordinates
     * @return pair of coordinates and string location
     */
    public Pair<List<Double>, String> getCoordinatesFromAddress(String location) {
        DecimalFormat df = new DecimalFormat("#.#####");
        DecimalFormat df2 = new DecimalFormat("##.#####");
        List<Double> coords;
        Pair<List<Double>, String> coordLocPair;
        double i = addresses.size();

        if (location.contains("NL")) {
            switch (location.split(" ")[2]) {
                case ("Delft") -> coords = delftCoords;
                case ("Rotterdam") -> coords = rotterdamCoords;
                case ("Amsterdam") -> coords = amsterdamCoords;
                default -> coords = eindhovenCoords;
            }
            coordLocPair = Pair.of(List.of(Double.parseDouble(df2.format((coords.get(0) + (i / 100000)))),
                    Double.parseDouble(df.format((coords.get(1) + (i / 100000))))), location);
        } else {
            df = new DecimalFormat("##.###");
            coordLocPair = Pair.of(List.of(Double.parseDouble(df.format((other.get(0) + (i / 1000)))),
                    Double.parseDouble(df.format((other.get(1) + (i / 1000))))), location);
        }
        addresses.add(coordLocPair);
        return coordLocPair;
    }

    /**
     * Obtain string form of location from coordinates.
     *
     * @param coords latitude/longitude form of location
     * @return location in string format
     */
    public String getAddressFromCoordinates(List<Double> coords) {
        String loc;
        Double val = 50.00;
        if (coords.get(0) > val) {
            int c = (int) (coords.get(0) * 100000);
            int c2 = (int) (coords.get(1) * 100000);
            int index = isInTheNetherlands(c, c2).indexOf(0);
            switch (index) {
                case 0 -> loc = addresses.get((c - delftLat)).getRight();
                case 1 -> loc = addresses.get((c - rotterdamLat)).getRight();
                case 2 -> loc = addresses.get((c - amsterdamLat)).getRight();
                default -> loc = addresses.get((c - eindhovenLat)).getRight();
            }

        } else {
            int c = (int) (coords.get(0) * 1000);
            loc = addresses.get((c - otherLat)).getRight();
        }
        return loc;
    }


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

    /**
     * Checks if coordinates are in NL.
     *
     * @param c  check value (from latitude coordinate)
     * @param c2 check value (from longitude coordinate)
     * @return list of values indicating if coordinates are in NL
     */
    public List<Integer> isInTheNetherlands(int c, int c2) {
        int check = (c - amsterdamLat) == (c2 - amsterdamLong) && (c - amsterdamLat) < addresses.size() ? 0 : 1;
        return List.of(isInDelft(c, c2), isInRotterdam(c, c2), check);
    }

    /**
     * Checks if coordinates are in Delft.
     *
     * @param c  check value (from latitude coordinate)
     * @param c2 check value (from longitude coordinate)
     * @return integer value indicating if coordinates are in Delft
     */
    public int isInDelft(int c, int c2) {
        return (c - delftLat) == (c2 - delftLong) && (c - delftLat) < addresses.size() ? 0 : 1;
    }

    /**
     * Checks if coordinates are in Rotterdam.
     *
     * @param c  check value (from latitude coordinate)
     * @param c2 check value (from longitude coordinate)
     * @return integer value indicating if coordinates are in Rotterdam
     */
    public int isInRotterdam(int c, int c2) {
        return (c - rotterdamLat) == (c2 - rotterdamLong) && (c - rotterdamLat) < addresses.size() ? 0 : 1;
    }
}
