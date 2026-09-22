package com.sandbox.ph.generatejasper.ticket.controller;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sandbox.ph.generatejasper.ticket.daoImpl.SingleFilterDaoImpl;
import com.sandbox.ph.generatejasper.ticket.dto.SingleFilterTicketDto;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Named("generateSingleFilterBean")
@ViewScoped
public class GenerateSingleFilterBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testFilter;
    private boolean reportGenerated = false;
    private String pdfBase64;

    // Injects the DAO implementation class
    @Inject
    private SingleFilterDaoImpl singleFilterDaoImpl;

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public String getTestFilter() {
        return testFilter;
    }

    public void setTestFilter(String testFilter) {
        this.testFilter = testFilter;
    }

    public boolean isReportGenerated() {
        return reportGenerated;
    }

    public void setReportGenerated(boolean reportGenerated) {
        this.reportGenerated = reportGenerated;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    // Clear filters/preview method for the Close button
    public void clearFilters() {
        this.testFilter = null;
        this.reportGenerated = false;
        this.pdfBase64 = null;
    }

    // =========================================================
    // GENERATE TEST JASPER PDF
    // =========================================================
    public void generateSingleFilterBeanAction() {

        long startTime = System.currentTimeMillis();

        System.out.println("-------------------------------------------------");
        System.out.println("START generateSingleFilterBeanAction() - TEST JASPER");
        System.out.println("-------------------------------------------------");

        FacesContext context = FacesContext.getCurrentInstance();

        if (context == null) {
            System.err.println("FacesContext is NULL.");
            return;
        }

        // Reset state before generation
        reportGenerated = false;
        pdfBase64 = null;

        // =====================================================
        // VALIDATE FILTER
        // =====================================================

        if (testFilter == null || testFilter.isBlank()) {
            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Required",
                            "Test Filter is required."));
            return;
        }

        try {
            // =====================================================
            // CALL DAO IMPL TO FETCH DATA WITH FILTER
            // =====================================================
            List<SingleFilterTicketDto> rawData = singleFilterDaoImpl.findDataByTestFilter(testFilter.trim());

            if (rawData == null || rawData.isEmpty()) {
                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                "Warning",
                                "No data found for the test filter: " + testFilter));
                return;
            }

            // =====================================================
            // LOAD REPORT RESOURCE (.jrxml)
            // =====================================================

            String testReportPath = "/reports/Activity.jrxml";

            try (InputStream mainStream = GenerateSingleFilterBean.class.getResourceAsStream(testReportPath)) {

                if (mainStream == null) {
                    throw new IllegalStateException("Test report jrxml was not found in /reports/");
                }

                System.out.println("Compiling test report...");
                JasperReport testReport = JasperCompileManager.compileReport(mainStream);
                System.out.println("Test report compiled successfully.");

                // =================================================
                // PARAMETERS
                // =================================================

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("testFilter", testFilter.trim());

                System.out.println("Parameters prepared. Test Filter: " + testFilter);

                // =================================================
                // FILL REPORT (Using JRBeanCollectionDataSource from DAO result)
                // =================================================

                System.out.println("Filling test report...");

                JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rawData);
                JasperPrint jasperPrint = JasperFillManager.fillReport(testReport, parameters, dataSource);

                System.out.println("Report filled successfully.");

                if (jasperPrint == null || jasperPrint.getPages().isEmpty()) {
                    context.addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_WARN,
                                    "Warning",
                                    "Report generated empty pages."));
                    return;
                }

                // =================================================
                // EXPORT PDF & CONVERT TO BASE64
                // =================================================

                byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

                if (pdfBytes == null || pdfBytes.length == 0) {
                    context.addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Error",
                                    "Generated PDF is empty."));
                    return;
                }

                // Convert bytes to Base64 for HTML object tag preview
                this.pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                this.reportGenerated = true;

                System.out.println("PDF generated. Size = " + pdfBytes.length + " bytes");

                // =================================================
                // SUCCESS MESSAGE
                // =================================================

                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_INFO,
                                "Success",
                                "Jasper generated successfully with filter: " + testFilter));
            }

        } catch (Exception e) {
            e.printStackTrace();
            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "Failed to generate Jasper report: " + e.getMessage()));
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            System.out.println("Execution time = " + executionTime + " ms");
            System.out.println("-------------------------------------------------");
        }
    }
}
