# Bahmni-Odoo Clinical Data Extension

## 📖 Overview
This repository contains a modular Spring extension for the Bahmni `openerp-atomfeed-service` (`odoo-connect`). It intercepts standard Sale Order (Encounter) synchronization events and enriches the outgoing XML-RPC/JSON payload with detailed clinical data from OpenMRS. 

---

## 🌟 Features & Data Mapping

This extension injects four critical clinical domains into the payload before it leaves the Java container. 

| Clinical Domain | Extracted Data | JSON Payload Key | Odoo Destination |
| :--- | :--- | :--- | :--- |
| **Demographics** | Age, Sex | `patient_age`, `patient_sex` | `res.partner` (Profile) |
| **Vitals** | Height, Weight, Systolic, Diastolic | `reg_vitals` | `res.partner` (Profile) |
| **Allergies** | Allergen, Severity, Reactions | `patient_allergies_payload` | `patient.allergy` (Custom) |
| **Prescriptions** | Dose, Route, Frequency, Duration | `extended_prescription_details` | `sale.order.line` (Invoice) |

---

## 📁 Project Architecture

The code follows a clean, modular `@Component` design. Each data domain has its own provider class implementing the `SaleOrderParameterProvider` interface. Spring automatically compiles and executes all of them during the sync event.

```text
src/main/java/org/bahmni/custom/extension/
├── PatientDemographicsProvider.java   # Fetches Age & Sex from /person API
├── PatientVitalsProvider.java         # Fetches Height/Weight/BP via Concept UUIDs
├── PatientAllergyProvider.java        # Formats allergy arrays from /allergy API
└── PrescriptionDetailsProvider.java   # Grabs clinical dosing from /encounter?v=full
