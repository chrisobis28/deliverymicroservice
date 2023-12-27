package nl.tudelft.sem.template.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//Testing AddressAdapter and GPS
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
    List<String> addr2 = List.of("NL","1334AB","Amsterdam","Kalverstraat","36");
    aa.convertStringAddressToDouble(addr);
    List<Double> res = aa.convertStringAddressToDouble(addr2);
    assertTrue(res.get(0) > 52.3);
    assertTrue(res.get(1) > 4.97);
    List<String> loc = aa.convertDoubleToStringAddress(res);
    assertEquals(addr2, loc);
  }

  @Test
  void convertDoubleToStringAddress2() {
    List<String> addr = List.of("NL","1034AB","Delft","Kalverstraat","36B");
    List<String> addr2 = List.of("NL","1334AB","Amsterdam","Kalverstraat","36");
    List<String> addr3 = List.of("NL","1334AB","Rotterdam","Kalverstraat","36");
    List<String> addr4 = List.of("NL","1334AB","Utrecht","Kalverstraat","36");
    List<String> addr5 = List.of("NL","1334AB","Utrecht","Kalverstraat","36B");
    List<List<Double>> res = new ArrayList<>();
    res.add(aa.convertStringAddressToDouble(addr));
    res.add(aa.convertStringAddressToDouble(addr2));
    res.add(aa.convertStringAddressToDouble(addr3));
    res.add(aa.convertStringAddressToDouble(addr4));
    res.add(aa.convertStringAddressToDouble(addr5));
    List<String> loc = aa.convertDoubleToStringAddress(res.get(4));
    assertEquals(addr5, loc);
    loc = aa.convertDoubleToStringAddress(res.get(2));
    assertEquals(addr3, loc);
  }

  @Test
  void convertDoubleToStringAddress3() {
    List<String> addr = List.of("NL","1034AB","Delft","Kalverstraat","36B");
    List<String> addr2 = List.of("NL","1334AB","Amsterdam","Kalverstraat","36");
    List<String> addr3 = List.of("FR","1334AB","Paris","Kalverstraat","36");
    List<String> addr4 = List.of("HU","1334AB","Budapest","Kalverstraat","36");
    List<List<Double>> res = new ArrayList<>();
    res.add(aa.convertStringAddressToDouble(addr));
    res.add(aa.convertStringAddressToDouble(addr2));
    res.add(aa.convertStringAddressToDouble(addr3));
    res.add(aa.convertStringAddressToDouble(addr4));
    List<String> loc = aa.convertDoubleToStringAddress(res.get(0));
    assertEquals(addr, loc);
    loc = aa.convertDoubleToStringAddress(res.get(1));
    assertEquals(addr2, loc);
    loc = aa.convertDoubleToStringAddress(res.get(2));
    assertEquals(addr3, loc);
    loc = aa.convertDoubleToStringAddress(res.get(3));
    assertEquals(addr4, loc);
  }
}