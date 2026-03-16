FROM bahmni/odoo-connect:latest
COPY target/patient-allergy-extension.jar /var/run/bahmni-erp-connect/bahmni-erp-connect/WEB-INF/lib/patient-allergy-extension.jar

#
