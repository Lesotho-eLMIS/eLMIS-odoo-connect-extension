package org.bahmni.custom.extension;

import org.springframework.stereotype.Component;
import org.bahmni.odooconnect.extensions.SaleOrderContext;
import org.bahmni.odooconnect.extensions.SaleOrderParameterProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Component
public class PatientAllergyParameterProvider implements SaleOrderParameterProvider {

  // Jackson mapper to handle inner JSON strings (like reactions_json)
  private static final ObjectMapper mapper = new ObjectMapper();

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

        // This list will hold the structured data for Odoo
        List<Map<String, Object>> structuredAllergies = new ArrayList<>();

        for (Map<String, Object> allergyRecord : results) {
          Map<String, Object> allergyData = new HashMap<>();

          // Core Identifiers
          allergyData.put("allergy_uuid", allergyRecord.get("uuid"));
          allergyData.put("patient_uuid", patientUuid);

          // Allergen Name & Type
          String allergyName = "Unknown Allergen";
          String allergenType = "OTHER";
          if (allergyRecord.containsKey("allergen") && allergyRecord.get("allergen") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> allergen = (Map<String, Object>) allergyRecord.get("allergen");
            if (allergen.containsKey("display"))
              allergyName = (String) allergen.get("display");
            if (allergen.containsKey("allergenType"))
              allergenType = (String) allergen.get("allergenType");
          } else if (allergyRecord.containsKey("display")) {
            allergyName = (String) allergyRecord.get("display");
          }
          allergyData.put("allergen_name", allergyName);
          allergyData.put("allergen_type", allergenType);

          // Severity (Converted to UPPERCASE to match your Odoo Selection field)
          if (allergyRecord.containsKey("severity") && allergyRecord.get("severity") != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> severityObj = (Map<String, Object>) allergyRecord.get("severity");
            if (severityObj.containsKey("display")) {
              allergyData.put("severity", ((String) severityObj.get("display")).toUpperCase());
            }
          }

          // Reactions (Both String and Raw JSON format for Odoo)
          if (allergyRecord.containsKey("reactions") && allergyRecord.get("reactions") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reactionsList = (List<Map<String, Object>>) allergyRecord.get("reactions");

            allergyData.put("reactions_json", mapper.writeValueAsString(reactionsList));

            List<String> reactionNames = new ArrayList<>();
            for (Map<String, Object> reactionRecord : reactionsList) {
              @SuppressWarnings("unchecked")
              Map<String, Object> reactionObj = (Map<String, Object>) reactionRecord.get("reaction");
              if (reactionObj != null && reactionObj.containsKey("display")) {
                reactionNames.add((String) reactionObj.get("display"));
              }
            }
            if (!reactionNames.isEmpty()) {
              allergyData.put("reactions", String.join(", ", reactionNames));
            }
          }

          // Comments & Voided Status
          if (allergyRecord.containsKey("comment")) {
            allergyData.put("comments", allergyRecord.get("comment"));
          }
          allergyData.put("voided", allergyRecord.getOrDefault("voided", false));

          structuredAllergies.add(allergyData);
        }

        // Pass the structured array list. Bahmni will serialize it into a JSON Array.
        if (!structuredAllergies.isEmpty()) {
          customParams.put("patient_allergies_payload", structuredAllergies);
        }
      }

    } catch (Exception e) {
      String errorMsg = e.getMessage() != null ? e.getMessage() : "";
      if (errorMsg.contains("500") || errorMsg.contains("ResourceDoesNotSupportOperationException")) {
        System.out.println("No allergy profile found for patient: " + context.getPatientUuid());
      } else {
        System.err.println("Unexpected error fetching allergies for patient: " + context.getPatientUuid());
        e.printStackTrace();
      }
    }

    return customParams;
  }
}
