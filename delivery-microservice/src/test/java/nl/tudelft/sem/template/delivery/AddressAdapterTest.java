package nl.tudelft.sem.template.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddressAdapterTest {

  AddressAdapter aa;

  @BeforeEach
  void setUp() {
    aa = new AddressAdapter(new GPS());
  }

  @Test
  void convertStringAddressToDouble() {
    List<String> addr = List.of("NL","1234AB","Amsterdam","Kalverstraat","36B");
    List<Double> res = aa.convertStringAddressToDouble(addr);
    assertTrue(res.get(0) > 52.3);
    assertTrue(res.get(1) > 4.97);
  }

  @Test
  void convertDoubleToStringAddress() {
    List<String> addr = List.of("NL","1234AB","Amsterdam","Kalverstraat","36B");
    List<String> addr2 = List.of("NL","1334AB","Amsterdam","Kalverstraat","36B");
    aa.convertStringAddressToDouble(addr);
    List<Double> res = aa.convertStringAddressToDouble(addr2);
    assertTrue(res.get(0) > 52.3);
    assertTrue(res.get(1) > 4.97);
    List<String> loc = aa.convertDoubleToStringAddress(res);
    assertEquals(addr2, loc);
  }
}