package org.bahmni.custom.extension;

import org.springframework.stereotype.Component;
import org.bahmni.odooconnect.extensions.SaleOrderContext;
import org.bahmni.odooconnect.extensions.SaleOrderParameterProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class PatientVitalsProvider implements SaleOrderParameterProvider {

  // ⚠️ REPLACE THESE WITH THE ACTUAL CONCEPT UUIDs FROM YOUR CLINIC'S DICTIONARY
  // IF CHANGED
  // ⚠️
  private static final String SYSTOLIC_UUID = "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  private static final String DIASTOLIC_UUID = "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  private static final String WEIGHT_UUID = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  private static final String HEIGHT_UUID = "5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  @Override
  public Map<String, Object> getAdditionalParams(SaleOrderContext context,
      BiFunction<String, Class<?>, Object> openMrsGetFunction) {

    Map<String, Object> customParams = new HashMap<>();
    Map<String, Object> regVitals = new HashMap<>();
    String patientUuid = context.getPatientUuid();

    try {
      // Fetch the latest value for each vital sign
      regVitals.put("systolic", getLatestObsValue(patientUuid, SYSTOLIC_UUID, openMrsGetFunction));
      regVitals.put("diastolic", getLatestObsValue(patientUuid, DIASTOLIC_UUID, openMrsGetFunction));
      regVitals.put("weight", getLatestObsValue(patientUuid, WEIGHT_UUID, openMrsGetFunction));
      regVitals.put("height", getLatestObsValue(patientUuid, HEIGHT_UUID, openMrsGetFunction));

      if (!regVitals.isEmpty()) {
        customParams.put("reg_vitals", regVitals);
      }

    } catch (Exception e) {
      System.err.println("Error grouping vitals for patient: " + patientUuid);
    }

    return customParams;
  }

  /**
   * Helper method to safely fetch the latest single Observation for a specific
   * concept
   */
  private Double getLatestObsValue(String patientUuid, String conceptUuid,
      BiFunction<String, Class<?>, Object> openMrsGetFunction) {
    try {
      // Ask OpenMRS for only the 1 most recent observation of this specific concept
      String apiUrl = "/openmrs/ws/rest/v1/obs?patient=" + patientUuid + "&concept=" + conceptUuid
          + "&v=custom:(value)&limit=1";

      @SuppressWarnings("unchecked")
      Map<String, Object> response = (Map<String, Object>) openMrsGetFunction.apply(apiUrl, Map.class);

      if (response != null && response.containsKey("results")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        if (!results.isEmpty()) {
          Object valueObj = results.get(0).get("value");

          // Standard numeric observation
          if (valueObj instanceof Number) {
            return ((Number) valueObj).doubleValue();
          }
          // Sometimes OpenMRS wraps numbers in a display map
          else if (valueObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> valueMap = (Map<String, Object>) valueObj;
            if (valueMap.containsKey("display")) {
              return Double.parseDouble(valueMap.get("display").toString());
            }
          }
        }
      }
    } catch (Exception e) {
      // Fails silently if the patient just hasn't had this vital taken yet
    }
    return null;
  }
}
