# Bahmni-Odoo Clinical Data Extension

## Overview
This repository contains a modular Spring extension for the Bahmni `openerp-atomfeed-service` (`odoo-connect`). It intercepts standard Sale Order (Encounter) synchronization events and enriches the outgoing XML-RPC/JSON payload with detailed clinical data from OpenMRS. 

---

## Features & Data Mapping

This extension injects four critical clinical domains into the payload before it leaves the Java container. 

| Clinical Domain | Extracted Data | JSON Payload Key | Odoo Destination |
| :--- | :--- | :--- | :--- |
| **Demographics** | Age, Sex | `patient_age`, `patient_sex` | `res.partner` (Profile) |
| **Vitals** | Height, Weight, Systolic, Diastolic | `reg_vitals` | `res.partner` (Profile) |
| **Allergies** | Allergen, Severity, Reactions | `patient_allergies_payload` | `patient.allergy` (Custom) |
| **Prescriptions** | Dose, Route, Frequency, Duration | `extended_prescription_details` | `sale.order.line` (Invoice) |

---

## Project Architecture

The code follows a clean, modular `@Component` design. Each data domain has its own provider class implementing the `SaleOrderParameterProvider` interface. Spring automatically compiles and executes all of them during the sync event.

```text
src/main/java/org/bahmni/custom/extension/
├── PatientDemographicsProvider.java   # Fetches Age & Sex from /person API
├── PatientVitalsProvider.java         # Fetches Height/Weight/BP via Concept UUIDs
├── PatientAllergyProvider.java        # Formats allergy arrays from /allergy API
└── PrescriptionDetailsProvider.java   # Grabs clinical dosing from /encounter?v=full
```
---

## Prerequisites
- Java 17
- Maven 3.x
- Target Bahmni Environment running odoo-connect-extensions:1.1.0-SNAPSHOT
- Jackson Databind (com.fasterxml.jackson.core:jackson-databind) defined in pom.xml with <scope>provided</scope>.


---

## Build Instructions
1. Open the terminal in the root of this project.
2. Run the following Maven command to download dependencies and compile the extension(the `-U` flag forces fresh updates):
``` bash
mvn clean package -U
```
3. A compiled JAR file (e.g., `clinical-data-extension.jar`) will be generated in the `/target` directory.

---

## Deployment (Docker)
The `odoo-connect` Docker image extracts its Tomcat `.war` file upon boot. Your custom JAR must be placed in the extracted `WEB-INF/lib`folder.

### Option A: Development (Docker Compose)
Inject the JAR into a running container via volume mapping in your `docker-compose.yml`:
``` YAML
services:
  odoo-connect:
    image: bahmni/odoo-connect:latest
    volumes:
      - ./target/clinical-data-extension.jar:/var/run/bahmni-erp-connect/bahmni-erp-connect/WEB-INF/lib/clinical-data-extension.jar
```
### Option B: Production (Custom Dockerfile)
For immutable production environments, build a custom image:
```Dockerfile
FROM bahmni/odoo-connect:latest 
COPY target/clinical-data-extension.jar /var/run/bahmni-erp-connect/bahmni-erp-connect/WEB-INF/lib/clinical-data-extension.jar
```
