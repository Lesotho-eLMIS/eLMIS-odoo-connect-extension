package org.bahmni.custom.extension;

import org.springframework.stereotype.Component;
import org.bahmni.odooconnect.extensions.SaleOrderContext;
import org.bahmni.odooconnect.extensions.SaleOrderParameterProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class PatientDemographicsProvider implements SaleOrderParameterProvider {

  @Override
  public Map<String, Object> getAdditionalParams(SaleOrderContext context,
      BiFunction<String, Class<?>, Object> openMrsGetFunction) {

    Map<String, Object> customParams = new HashMap<>();

    try {
      String patientUuid = context.getPatientUuid();

      String apiUrl = "/openmrs/ws/rest/v1/person/" + patientUuid;

      @SuppressWarnings("unchecked")
      Map<String, Object> response = (Map<String, Object>) openMrsGetFunction.apply(apiUrl, Map.class);

      if (response != null) {
        // Grab Age (Returns an integer)
        if (response.containsKey("age") && response.get("age") != null) {
          customParams.put("patient_age", response.get("age"));
        }

        // Grab Sex/Gender (Returns "M" or "F")
        if (response.containsKey("gender") && response.get("gender") != null) {
          customParams.put("patient_sex", response.get("gender"));
        }
      }

    } catch (Exception e) {
      System.err.println("Error fetching demographics for patient: " + context.getPatientUuid());
    }

    return customParams;
  }
}
