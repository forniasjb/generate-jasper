package com.sandbox.ph.generatejasper.ticket.controller;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.primefaces.PrimeFaces;
import org.primefaces.model.StreamedContent;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("reportTypeSelection")
@ViewScoped
public class TicketBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportType;
    private Map<String, String> reportTypes;

    // DTDTS filters
    private String ticketNoFrom;
    private String ticketNoTo;
    private String accountNoFrom;
    private String accountNoTo;

    // DDSOA filters
    private String ticketNumber;
    private String testFilter;

    // StreamedContent for displaying PDF in UI
    private StreamedContent reportStream;

    private String pdfBase64;
    private boolean testReportGenerated;

    @PostConstruct
    public void init() {
        reportTypes = new LinkedHashMap<>();
        // Key = Label shown in dropdown, Value = Submitted value
        reportTypes.put("Demand Deposit Statement of Account (DDSOA)", "DDSOA");
        reportTypes.put("Demand and Time Deposit Transaction Summary (DTDTS)", "DTDTS");
        reportTypes.put("Generate Test Jasper", "TEST_JASPER");
    }

    public void handleProceed() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (reportType == null || reportType.trim().isEmpty()) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Please select a report type."));
            return;
        }

        if ("DTDTS".equals(reportType)) {
            PrimeFaces.current().executeScript("PF('reportFilterDialog').show();");
        } else if ("DDSOA".equals(reportType)) {
            PrimeFaces.current().executeScript("PF('ddsoaFilterDialog').show();");
        } else if ("TEST_JASPER".equals(reportType)) {
            PrimeFaces.current().executeScript("PF('testFilterDialog').show();");
        }
    }

    // Getters and Setters
    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Map<String, String> getReportTypes() {
        return reportTypes;
    }

    public void setReportTypes(Map<String, String> reportTypes) {
        this.reportTypes = reportTypes;
    }

    public String getTicketNoFrom() {
        return ticketNoFrom;
    }

    public void setTicketNoFrom(String ticketNoFrom) {
        this.ticketNoFrom = ticketNoFrom;
    }

    public String getTicketNoTo() {
        return ticketNoTo;
    }

    public void setTicketNoTo(String ticketNoTo) {
        this.ticketNoTo = ticketNoTo;
    }

    public String getAccountNoFrom() {
        return accountNoFrom;
    }

    public void setAccountNoFrom(String accountNoFrom) {
        this.accountNoFrom = accountNoFrom;
    }

    public String getAccountNoTo() {
        return accountNoTo;
    }

    public void setAccountNoTo(String accountNoTo) {
        this.accountNoTo = accountNoTo;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getTestFilter() {
        return testFilter;
    }

    public void setTestFilter(String testFilter) {
        this.testFilter = testFilter;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public boolean isTestReportGenerated() {
        return testReportGenerated;
    }

    public void setTestReportGenerated(boolean testReportGenerated) {
        this.testReportGenerated = testReportGenerated;
    }

    public StreamedContent getReportStream() {
        return reportStream;
    }

    public void setReportStream(StreamedContent reportStream) {
        this.reportStream = reportStream;
    }

}
