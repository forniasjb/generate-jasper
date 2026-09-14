package com.sandbox.ph.generatejasper.ticket.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandbox.ph.generatejasper.ticket.daoImpl.TicketDaoImpl;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

@Named("generateBean")
@ViewScoped
public class GenerateBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GenerateBean.class);
    private static final String LOG_SEPARATOR_EQUALS = "=================================================";
    private static final String LOG_SEPARATOR_DASH = "-------------------------------------------------";
    private static final String MSG_REQUIRED = "Required";
    private static final String MSG_WARNING = "Warning";
    private static final String MSG_ERROR = "Error";
    private static final String REPORT_PATH = "/reports/main.jasper";
    private transient TicketDaoImpl ticketDao = new TicketDaoImpl();

    // =========================================================
    // Form Filters
    // =========================================================

    private Date fromDate;
    private Date toDate;
    private String ticketNoFrom;
    private String ticketNoTo;
    private String orgaCode;

    // =========================================================
    // Report Properties
    // =========================================================

    private byte[] pdfBytes;
    private String pdfBase64;
    private boolean reportGenerated = false;

    // =========================================================
    // Constructor
    // =========================================================

    public GenerateBean() {
        log.info("GenerateBean initialized");
    }

    // =========================================================
    // Generate PDF
    // =========================================================

    public void generatePdf() {
        log.info(LOG_SEPARATOR_EQUALS);
        log.info("Generate PDF button clicked");
        log.info(LOG_SEPARATOR_EQUALS);

        generateReport();
    }

    public void generateDdsoaPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        reportGenerated = false;
        pdfBytes = null;
        pdfBase64 = null;

        if (!isValidDdsoaFilters(context)) {
            return;
        }

        try (InputStream mainStream = getClass().getResourceAsStream("/reports/main.jrxml");
                InputStream subStream = getClass().getResourceAsStream("/reports/sub.jrxml")) {
            if (mainStream == null || subStream == null) {
                throw new IllegalStateException("DDSOA report templates were not found in /reports.");
            }

            JasperReport mainReport = JasperCompileManager.compileReport(mainStream);
            JasperReport subReport = JasperCompileManager.compileReport(subStream);
            List<com.sandbox.ph.generatejasper.ticket.dto.TicketDto> rows = ticketDao
                    .findReportDataByDateAndTicketRange(fromDate, toDate, ticketNoFrom, ticketNoTo, orgaCode);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("frticket", ticketNoFrom.trim());
            parameters.put("toticket", ticketNoTo.trim());
            parameters.put("sub", subReport);
            parameters.put("ftrndt", fromDate);
            parameters.put("ttrndt", toDate);

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    mainReport, parameters, new JRBeanCollectionDataSource(rows));
            if (jasperPrint.getPages().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Warning", "No data found for the selected ticket range."));
                return;
            }

            pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);
            pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            reportGenerated = true;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Success", "DDSOA report generated successfully."));
        } catch (Exception e) {
            log.error("Failed to generate DDSOA report", e);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Failed to generate DDSOA report: " + e.getMessage()));
        }
    }

    private boolean isValidDdsoaFilters(FacesContext context) {
        if (fromDate == null || toDate == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Required", "From Date and To Date are required."));
            return false;
        }

        if (fromDate.after(toDate)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid Date Range", "From Date cannot be later than To Date."));
            return false;
        }

        if (ticketNoFrom == null || ticketNoFrom.isBlank()
                || ticketNoTo == null || ticketNoTo.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Required", "Ticket No. From and Ticket No. To are required."));
            return false;
        }

        if (orgaCode == null || orgaCode.isBlank()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Required", "Organization Unit Code is required."));
            return false;
        }

        try {
            if (Integer.parseInt(ticketNoFrom.trim()) > Integer.parseInt(ticketNoTo.trim())) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Invalid Range", "Ticket No. From cannot be greater than Ticket No. To."));
                return false;
            }
        } catch (NumberFormatException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid Ticket Number", "Ticket numbers must contain digits only."));
            return false;
        }
        return true;
    }

    // =========================================================
    // Generate Report
    // =========================================================

    public void generateReport() {
        long startTime = System.currentTimeMillis();

        log.info(LOG_SEPARATOR_DASH);
        log.info("START generateReport()");
        log.info(LOG_SEPARATOR_DASH);

        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            log.error("FacesContext is NULL. Cannot generate report.");
            return;
        }

        log.info("FacesContext obtained successfully");
        logInputParameters();

        if (!validateInputs(context)) {
            return;
        }

        try {
            Map<String, Object> parameters = prepareParameters();
            JasperReport jasperReport = loadJasperReport();

            JasperPrint jasperPrint = executeDao(jasperReport, parameters);
            if (!validateJasperPrint(context, jasperPrint)) {
                return;
            }

            processPdfExport(context, jasperPrint);

        } catch (Exception e) {
            handleReportException(context, e);
        } finally {
            logExecutionSummary(startTime);
        }
    }

    private void logInputParameters() {
        log.info("Report input parameters:");
        log.info("  fromDate     = {}", fromDate);
        log.info("  toDate       = {}", toDate);
        log.info("  ticketNoFrom = {}", ticketNoFrom);
        log.info("  ticketNoTo   = {}", ticketNoTo);
        log.info("  orgaCode     = {}", orgaCode);
    }

    private boolean validateInputs(FacesContext context) {
        log.info("Starting input validation...");

        if (fromDate == null) {
            addValidationError(context, "From Date is required.");
            return false;
        }
        if (toDate == null) {
            addValidationError(context, "To Date is required.");
            return false;
        }
        if (ticketNoFrom == null || ticketNoFrom.isBlank()) {
            addValidationError(context, "Ticket No. From is required.");
            return false;
        }
        if (ticketNoTo == null || ticketNoTo.isBlank()) {
            addValidationError(context, "Ticket No. To is required.");
            return false;
        }
        if (orgaCode == null || orgaCode.isBlank()) {
            addValidationError(context, "Organization Unit Code is required.");
            return false;
        }
        if (fromDate.after(toDate)) {
            log.warn("Validation failed: From Date is after To Date. fromDate = {}, toDate = {}", fromDate, toDate);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid Date Range",
                    "From Date cannot be later than To Date."));
            reportGenerated = false;
            return false;
        }

        log.info("Input validation PASSED");
        return true;
    }

    private void addValidationError(FacesContext context, String message) {
        log.warn("Validation failed: {}", message);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, MSG_REQUIRED, message));
        reportGenerated = false;
    }

    private Map<String, Object> prepareParameters() {
        log.info("Converting java.util.Date to DB2 YYYYMMDD...");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Integer ftrndtNum = Integer.parseInt(sdf.format(fromDate));
        Integer ttrndtNum = Integer.parseInt(sdf.format(toDate));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ftrndt", fromDate);
        parameters.put("ttrndt", toDate);
        parameters.put("ftrndt_num", ftrndtNum);
        parameters.put("ttrndt_num", ttrndtNum);
        parameters.put("frticket", ticketNoFrom);
        parameters.put("toticket", ticketNoTo);
        parameters.put("orga", orgaCode);

        try {
            InputStream subReportStream = getClass().getResourceAsStream("/reports/sub.jasper");
            if (subReportStream != null) {
                JasperReport subReportObj = (JasperReport) JRLoader.loadObject(subReportStream);
                parameters.put("sub", subReportObj);
                log.info("SubReport 'sub.jasper' loaded successfully as object.");
            } else {
                log.error("SubReport stream is NULL at /reports/sub.jasper");
            }
        } catch (Exception e) {
            log.error("Failed to load subReport 'sub.jasper'", e);
        }

        return parameters;
    }

    private JasperReport loadJasperReport() throws JRException {
        log.info("Loading Jasper report: {}", REPORT_PATH);

        InputStream reportStream = getClass().getResourceAsStream(REPORT_PATH);
        if (reportStream == null) {
            log.error("Jasper report NOT FOUND: {}", REPORT_PATH);
            throw new IllegalStateException("Report file 'main.jasper' not found in src/main/resources/reports/");
        }

        return (JasperReport) JRLoader.loadObject(reportStream);
    }

    private JasperPrint executeDao(JasperReport jasperReport, Map<String, Object> parameters) throws JRException {
        log.info("Calling TicketDaoImpl.generateBankSummaryReport()...");
        long daoStartTime = System.currentTimeMillis();

        JasperPrint jasperPrint = ticketDao.generateBankSummaryReport(
                jasperReport, parameters, fromDate, toDate, ticketNoFrom, ticketNoTo, orgaCode);

        log.info("DAO/Jasper execution time = {} ms", System.currentTimeMillis() - daoStartTime);
        return jasperPrint;
    }

    private boolean validateJasperPrint(FacesContext context, JasperPrint jasperPrint) {
        if (jasperPrint == null) {
            log.warn("JasperPrint is NULL");
            reportGenerated = false;
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, MSG_WARNING, "No report was generated."));
            return false;
        }

        int pageCount = jasperPrint.getPages().size();
        log.info("JasperPrint page count = {}", pageCount);

        if (pageCount == 0) {
            log.warn("JasperPrint contains ZERO pages");
            reportGenerated = false;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, MSG_WARNING,
                    "No data found for the selected filters."));
            return false;
        }

        return true;
    }

    private void processPdfExport(FacesContext context, JasperPrint jasperPrint) throws JRException {
        log.info("Exporting JasperPrint to PDF...");
        long pdfStartTime = System.currentTimeMillis();

        this.pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

        log.info("PDF export time = {} ms", System.currentTimeMillis() - pdfStartTime);

        if (this.pdfBytes != null && this.pdfBytes.length > 0) {
            log.info("PDF generated successfully. Size = {} bytes", this.pdfBytes.length);

            long base64StartTime = System.currentTimeMillis();
            this.pdfBase64 = Base64.getEncoder().encodeToString(this.pdfBytes);
            log.info("Base64 conversion time = {} ms", System.currentTimeMillis() - base64StartTime);

            this.reportGenerated = true;
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Report generated successfully."));
        } else {
            log.warn("PDF is NULL or EMPTY");
            this.pdfBytes = null;
            this.pdfBase64 = null;
            this.reportGenerated = false;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, MSG_WARNING,
                    "No data found for the selected filters."));
        }
    }

    private void handleReportException(FacesContext context, Exception e) {
        log.error(LOG_SEPARATOR_EQUALS);
        log.error("ERROR while generating Jasper report", e);
        log.error(LOG_SEPARATOR_EQUALS);

        this.pdfBytes = null;
        this.pdfBase64 = null;
        this.reportGenerated = false;

        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = e.getClass().getSimpleName();
        }

        context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, MSG_ERROR, "Failed to generate report: " + errorMessage));
    }

    private void logExecutionSummary(long startTime) {
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("generateReport() TOTAL execution time = {} ms", totalDuration);
        log.info("Final reportGenerated = {}", reportGenerated);
        log.info("Final PDF size = {} bytes", pdfBytes != null ? pdfBytes.length : 0);
        log.info("END generateReport()");
        log.info(LOG_SEPARATOR_DASH);
    }

    // =========================================================
    // Clear Filters
    // =========================================================

    public void clearFilters() {
        log.info("clearFilters() called");

        fromDate = null;
        toDate = null;
        ticketNoFrom = null;
        ticketNoTo = null;
        orgaCode = null;

        pdfBytes = null;
        pdfBase64 = null;
        reportGenerated = false;

        log.info("All filters cleared. Report state reset.");
    }

    // =========================================================
    // Streamed File
    // =========================================================

    public StreamedContent getFile() {
        log.debug("getFile() called");

        if (pdfBytes == null || pdfBytes.length == 0) {
            log.debug("getFile(): PDF bytes are NULL or EMPTY");
            return null;
        }

        log.debug("getFile(): returning PDF, size={} bytes", pdfBytes.length);

        return DefaultStreamedContent.builder()
                .name("Ticket.pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(pdfBytes))
                .build();
    }

    // =========================================================
    // Getters and Setters
    // =========================================================

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

    public String getOrgaCode() {
        return orgaCode;
    }

    public void setOrgaCode(String orgaCode) {
        this.orgaCode = orgaCode;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public boolean isReportGenerated() {
        return reportGenerated;
    }

    public void setReportGenerated(boolean reportGenerated) {
        this.reportGenerated = reportGenerated;
    }

    public TicketDaoImpl getTicketDao() {
        return ticketDao;
    }

    public void setTicketDao(TicketDaoImpl ticketDao) {
        this.ticketDao = ticketDao;
    }
}
