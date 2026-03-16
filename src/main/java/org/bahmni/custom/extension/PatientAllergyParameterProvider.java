package org.bahmni.custom.extension;

import org.springframework.stereotype.Component;
import org.bahmni.odooconnect.extensions.SaleOrderContext;
import org.bahmni.odooconnect.extensions.SaleOrderParameterProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class PatientAllergyParameterProvider implements SaleOrderParameterProvider {

  @Override
  public Map<String, Object> getAdditionalParams(SaleOrderContext context,
      BiFunction<String, Class<?>, Object> openMrsGetFunction) {

    Map<String, Object> customParams = new HashMap<>();

    try {
      String patientUuid = context.getPatientUuid();
      String apiUrl = "/openmrs/ws/rest/v1/patient/" + patientUuid + "/allergy";
      @SuppressWarnings("unchecked")
      Map<String, Object> response = (Map<String, Object>) openMrsGetFunction.apply(apiUrl, Map.class);

      if (response != null && response.containsKey("results")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        List<String> formattedAllergies = new ArrayList<>();

        for (Map<String, Object> allergyRecord : results) {

          // 1. Get the main allergy name
          String allergyName = (String) allergyRecord.get("display");
          if (allergyName == null)
            continue;

          StringBuilder allergyDetails = new StringBuilder(allergyName);

          // 2. Add Severity if it exists
          if (allergyRecord.containsKey("severity") && allergyRecord.get("severity") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> severityObj = (Map<String, Object>) allergyRecord.get("severity");
            if (severityObj.containsKey("display")) {
              allergyDetails.append(" (").append(severityObj.get("display")).append(")");
            }
          }

          // 3. Extract Reactions if they exist
          if (allergyRecord.containsKey("reactions") && allergyRecord.get("reactions") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reactionsList = (List<Map<String, Object>>) allergyRecord.get("reactions");

            if (!reactionsList.isEmpty()) {
              List<String> reactionNames = new ArrayList<>();
              for (Map<String, Object> reactionRecord : reactionsList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reactionObj = (Map<String, Object>) reactionRecord.get("reaction");
                if (reactionObj != null && reactionObj.containsKey("display")) {
                  reactionNames.add((String) reactionObj.get("display"));
                }
              }
              if (!reactionNames.isEmpty()) {
                allergyDetails.append(" - Reactions: ").append(String.join(", ", reactionNames));
              }
            }
          }

          // Add this fully formatted allergy string to our list
          formattedAllergies.add(allergyDetails.toString());
        }

        // Combine all allergies for this patient into one final string
        if (!formattedAllergies.isEmpty()) {
          String finalAllergyString = String.join(" | ", formattedAllergies);
          customParams.put("patient_allergies", finalAllergyString);
        }
      }

    } catch (Exception e) {
      System.err.println("Error fetching allergies for patient: " + context.getPatientUuid());
      e.printStackTrace();
    }

    return customParams;
  }
}
