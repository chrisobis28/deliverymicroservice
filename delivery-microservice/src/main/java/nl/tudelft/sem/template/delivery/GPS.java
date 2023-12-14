package nl.tudelft.sem.template.delivery;

import java.util.List;

public interface GPS {

  List<Double> getCoordinatesOfAddress(List<String> address);
}
