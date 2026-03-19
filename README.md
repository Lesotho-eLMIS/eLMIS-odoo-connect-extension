# Bahmni Odoo Connect: Structured Patient Allergy Extension

This repository contains a custom Spring extension for the Bahmni `openerp-atomfeed-service` (`odoo-connect`). It dynamically intercepts Sale Order (Encounter) synchronization events, fetches patient allergy profiles from OpenMRS, and attaches them as a structured JSON payload to the data sent to Odoo.

## Why This Exists
By default, the Bahmni-to-Odoo synchronization process does not include clinical allergy data. When a doctor creates a drug order or lab test, Odoo processes the billing without knowing the patient's allergies. 

This extension bridges that gap. It utilizes the `SaleOrderParameterProvider` interface to:
1. Intercept a Sale Order event right before it is transmitted to Odoo.
2. Securely query the OpenMRS REST API (`/ws/rest/v1/patient/{uuid}/allergy`) for the specific patient.
3. Parse the response and format it into a clean, structured List of Maps containing the UUID, allergen name, type, severity, and reactions.
4. Inject this structured data as a new parameter (`patient_allergies_payload`) into the final XML-RPC payload, serialized as a JSON array using Jackson.

## 📋 Prerequisites
* **Java 17** (Required to match the Bahmni `odoo-connect` environment)
* **Maven 3.x**
* Target Bahmni Environment utilizing `odoo-connect-extensions:1.1.0-SNAPSHOT`
* **Jackson Databind** (`com.fasterxml.jackson.core:jackson-databind`) defined in the `pom.xml` with `<scope>provided</scope>`.

## 🛠️ How to Build

1. Open a terminal in the root of this project.
2. Run the following Maven command to download dependencies and compile the extension:
   ```bash
   mvn clean package -U
