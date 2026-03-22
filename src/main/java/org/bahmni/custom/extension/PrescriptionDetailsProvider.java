package org.bahmni.custom.extension;

import org.springframework.stereotype.Component;
import org.bahmni.odooconnect.extensions.SaleOrderContext;
import org.bahmni.odooconnect.extensions.SaleOrderParameterProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class PrescriptionDetailsProvider implements SaleOrderParameterProvider {

  @Override
  public Map<String, Object> getAdditionalParams(SaleOrderContext context,
      BiFunction<String, Class<?>, Object> openMrsGetFunction) {
    Map<String, Object> customParams = new HashMap<>();
    Map<String, Map<String, Object>> prescriptionDetails = new HashMap<>();
    String encounterUuid = context.getEncounterUuid();

    if (encounterUuid == null)
      return customParams;

    try {
      // Ask OpenMRS for the FULL encounter so we get all the rich drug order details
      String apiUrl = "/openmrs/ws/rest/v1/encounter/" + encounterUuid + "?v=full";

      @SuppressWarnings("unchecked")
      Map<String, Object> response = (Map<String, Object>) openMrsGetFunction.apply(apiUrl, Map.class);

      if (response != null && response.containsKey("orders")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) response.get("orders");

        for (Map<String, Object> order : orders) {
          // Only process Drug Orders
          if ("drugorder".equals(order.get("type"))) {
            String orderUuid = (String) order.get("uuid");
            Map<String, Object> details = new HashMap<>();

            details.put("dose", order.get("dose"));
            details.put("doseUnits", extractDisplayName(order.get("doseUnits")));
            details.put("frequency", extractDisplayName(order.get("frequency")));
            details.put("route", extractDisplayName(order.get("route")));
            details.put("duration", order.get("duration"));
            details.put("durationUnits", extractDisplayName(order.get("durationUnits")));
            details.put("numRefills", order.get("numRefills"));
            details.put("asNeeded", order.get("asNeeded"));
            details.put("administrationInstructions", order.get("dosingInstructions"));

            prescriptionDetails.put(orderUuid, details);
          }
        }
      }

      // Add to the main payload if we found any drug orders
      if (!prescriptionDetails.isEmpty()) {
        customParams.put("extended_prescription_details", prescriptionDetails);
      }

    } catch (Exception e) {
      System.err.println("Error fetching prescription details for encounter: " + encounterUuid);
    }

    return customParams;
  }

  // Helper to safely extract the readable string out of OpenMRS concept
  // dictionaries
  private String extractDisplayName(Object openMrsObject) {
    if (openMrsObject instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) openMrsObject;
      return (String) map.get("display");
    }
    return null;
  }
}
