package com.sandbox.ph.generatejasper.ticket.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandbox.ph.generatejasper.ticket.daoImpl.DtdtsTicketDaoImpl;
import com.sandbox.ph.generatejasper.ticket.dto.TicketDto;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Named("generateBeanDTDTS")
@ViewScoped
public class GenerateBeanDTDTS implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GenerateBeanDTDTS.class);
    private static final String MSG_REQUIRED = "Required";
    private static final String MSG_WARNING = "Warning";
    private static final String MSG_ERROR = "Error";

    // =========================================================
    // REPORT RESOURCE PATHS
    // =========================================================

    private static final String MAIN_REPORT = "/reports/Activity Report.jrxml";
    private static final String SUB_REPORT = "/reports/sub2.jrxml";
    private static final String SUB_REPORT_3 = "/reports/sub3.jrxml";

    // =========================================================
    // DAO
    // =========================================================

    private transient DtdtsTicketDaoImpl ticketDao = new DtdtsTicketDaoImpl();

    // =========================================================
    // DTDTS FILTERS
    // =========================================================

    private Date fromDate;
    private Date toDate;

    private String ticketNoFrom;
    private String ticketNoTo;

    private String orgaCode;

    // =========================================================
    // REPORT OUTPUT
    // =========================================================

    private byte[] pdfBytes;
    private String pdfBase64;
    private boolean reportGenerated = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GenerateBeanDTDTS() {
        log.info("GenerateBeanDTDTS initialized");
    }

    // =========================================================
    // GENERATE DTDTS PDF
    // =========================================================

    public void generatePdf() {

        long startTime = System.currentTimeMillis();

        log.info("-------------------------------------------------");
        log.info("START generatePdf() - DTDTS");
        log.info("-------------------------------------------------");

        FacesContext context = FacesContext.getCurrentInstance();

        if (context == null) {
            log.error("FacesContext is NULL.");
            return;
        }

        // =====================================================
        // RESET PREVIOUS REPORT
        // =====================================================

        reportGenerated = false;
        pdfBytes = null;
        pdfBase64 = null;

        // =====================================================
        // VALIDATE FILTERS
        // =====================================================

        if (!isValidFilters(context)) {
            return;
        }

        // =====================================================
        // LOAD REPORT RESOURCES
        // =====================================================

        try (
                InputStream mainStream = GenerateBeanDTDTS.class.getResourceAsStream(
                        MAIN_REPORT);

                InputStream subStream = GenerateBeanDTDTS.class.getResourceAsStream(
                        SUB_REPORT);

                InputStream sub3Stream = GenerateBeanDTDTS.class.getResourceAsStream(
                        SUB_REPORT_3)) {

            // =================================================
            // CHECK MAIN REPORT
            // =================================================

            if (mainStream == null) {

                throw new IllegalStateException(
                        "Activity Report.jrxml was not found in /reports/");
            }

            log.info(
                    "Found main report: {}",
                    MAIN_REPORT);

            // =================================================
            // COMPILE MAIN REPORT
            // =================================================

            log.info(
                    "Compiling Activity Report.jrxml...");

            JasperReport dtdtsReport = JasperCompileManager.compileReport(
                    mainStream);

            log.info(
                    "Activity Report.jrxml compiled successfully.");

            // =================================================
            // PARAMETERS
            // =================================================

            Map<String, Object> parameters = new HashMap<>();

            parameters.put(
                    "ftrndt",
                    fromDate);

            parameters.put(
                    "ttrndt",
                    toDate);

            parameters.put(
                    "frticket",
                    ticketNoFrom.trim());

            parameters.put(
                    "toticket",
                    ticketNoTo.trim());

            parameters.put(
                    "orga",
                    orgaCode.trim());

            log.info("DTDTS parameters prepared.");
            log.info("From Date  : {}", fromDate);
            log.info("To Date    : {}", toDate);
            log.info("Ticket From: {}", ticketNoFrom);
            log.info("Ticket To  : {}", ticketNoTo);
            log.info("Organization: {}", orgaCode);

            // =================================================
            // COMPILE SUB REPORT
            // =================================================

            if (subStream == null) {

                throw new IllegalStateException(
                        "sub2.jrxml was not found in /reports/");
            }

            log.info(
                    "Compiling sub2.jrxml...");

            JasperReport subReport = JasperCompileManager.compileReport(
                    subStream);

            parameters.put(
                    "sub",
                    subReport);

            log.info(
                    "sub2.jrxml compiled successfully.");

            // =================================================
            // COMPILE SUB REPORT 3
            // =================================================

            if (sub3Stream == null) {

                throw new IllegalStateException(
                        "sub3.jrxml was not found in /reports/");
            }

            log.info(
                    "Compiling sub3.jrxml...");

            JasperReport sub3Report = JasperCompileManager.compileReport(
                    sub3Stream);

            parameters.put(
                    "sub3",
                    sub3Report);

            log.info(
                    "sub3.jrxml compiled successfully.");

            // =================================================
            // GET DATA FROM DAO
            // =================================================

            log.info(
                    "Calling DTDTS DAO...");

            List<TicketDto> rows = ticketDao.findReportDataByDateAndTicketRange(
                    fromDate, toDate, ticketNoFrom.trim(), ticketNoTo.trim(), orgaCode.trim());

            log.info("DTDTS records found: {}", rows != null ? rows.size() : 0);

            if (rows != null) {
                for (TicketDto dto : rows) {
                    log.info("Ticket No: {}, Trans Type: {}", dto.getTicketNo(), dto.getTransType());
                }
            }

            // =================================================
            // NO DATA
            // =================================================

            if (rows == null || rows.isEmpty()) {

                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                MSG_WARNING,
                                "No data found for the selected DTDTS filters."));

                return;
            }

            // =================================================
            // CREATE DATA SOURCE
            // =================================================

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);

            log.info(
                    "JRBeanCollectionDataSource created.");

            // =================================================
            // FILL REPORT
            // =================================================

            log.info(
                    "Filling Activity Report...");

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    dtdtsReport,
                    parameters,
                    dataSource);

            log.info(
                    "Report filled successfully.");

            // =================================================
            // CHECK JASPER PAGES
            // =================================================

            if (jasperPrint == null) {

                throw new IllegalStateException(
                        "JasperPrint is null.");
            }

            if (jasperPrint.getPages().isEmpty()) {

                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                MSG_WARNING,
                                "No data found for the selected DTDTS filters."));

                return;
            }

            log.info(
                    "Generated report pages: {}",
                    jasperPrint.getPages().size());

            // =================================================
            // EXPORT PDF
            // =================================================

            log.info(
                    "Exporting report to PDF...");

            pdfBytes = JasperExportManager.exportReportToPdf(
                    jasperPrint);

            // =================================================
            // CHECK PDF
            // =================================================

            if (pdfBytes == null ||
                    pdfBytes.length == 0) {

                reportGenerated = false;

                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                MSG_ERROR,
                                "Generated DTDTS PDF is empty."));

                return;
            }

            log.info(
                    "PDF generated. Size={} bytes",
                    pdfBytes.length);

            // =================================================
            // BASE64
            // =================================================

            pdfBase64 = Base64.getEncoder()
                    .encodeToString(pdfBytes);

            // =================================================
            // SUCCESS
            // =================================================

            reportGenerated = true;

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "DTDTS report generated successfully."));

            log.info(
                    "DTDTS PDF generated successfully.");

        } catch (Exception e) {

            // =================================================
            // ERROR HANDLING
            // =================================================

            log.error(
                    "Failed to generate DTDTS report",
                    e);

            pdfBytes = null;
            pdfBase64 = null;
            reportGenerated = false;

            String message = e.getMessage();

            if (message == null ||
                    message.isBlank()) {

                message = e.getClass()
                        .getSimpleName();
            }

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_ERROR,
                            "Failed to generate DTDTS report: "
                                    + message));

        } finally {

            // =================================================
            // EXECUTION TIME
            // =================================================

            long executionTime = System.currentTimeMillis()
                    - startTime;

            log.info(
                    "DTDTS execution time = {} ms",
                    executionTime);

            log.info(
                    "END generatePdf() - DTDTS");

            log.info(
                    "-------------------------------------------------");
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean isValidFilters(
            FacesContext context) {

        // =====================================================
        // DATE REQUIRED
        // =====================================================

        if (fromDate == null ||
                toDate == null) {

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_REQUIRED,
                            "From Date and To Date are required for DTDTS."));

            return false;
        }

        // =====================================================
        // DATE RANGE
        // =====================================================

        if (fromDate.after(toDate)) {

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Invalid Date Range",
                            "From Date cannot be later than To Date."));

            return false;
        }

        // =====================================================
        // TICKET REQUIRED
        // =====================================================

        if (ticketNoFrom == null ||
                ticketNoFrom.isBlank() ||
                ticketNoTo == null ||
                ticketNoTo.isBlank()) {

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_REQUIRED,
                            "Ticket No. From and Ticket No. To are required for DTDTS."));

            return false;
        }

        // =====================================================
        // TICKET NUMBER VALIDATION
        // =====================================================

        try {

            int from = Integer.parseInt(
                    ticketNoFrom.trim());

            int to = Integer.parseInt(
                    ticketNoTo.trim());

            if (from > to) {

                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "Invalid Ticket Range",
                                "Ticket No. From cannot be greater than Ticket No. To."));

                return false;
            }

        } catch (NumberFormatException e) {

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Invalid Ticket Number",
                            "Ticket numbers must contain digits only."));

            return false;
        }

        // =====================================================
        // ORGANIZATION REQUIRED
        // =====================================================

        if (orgaCode == null ||
                orgaCode.isBlank()) {

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_REQUIRED,
                            "Organization Unit Code is required for DTDTS."));

            return false;
        }
        return true;
    }

    // =========================================================
    // CLEAR FILTERS
    // =========================================================

    public void clearFilters() {

        log.info("Clearing DTDTS filters...");
        // fromDate = null;
        // toDate = null;
        // ticketNoFrom = null;
        // ticketNoTo = null;
        // orgaCode = null;
        pdfBytes = null;
        pdfBase64 = null;
        reportGenerated = false;
    }

    // =========================================================
    // DOWNLOAD PDF
    // =========================================================

    public StreamedContent getFile() {

        if (pdfBytes == null ||
                pdfBytes.length == 0) {

            return null;
        }

        return DefaultStreamedContent
                .builder()
                .name("ActivityReports.pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(
                        pdfBytes))
                .build();
    }

    // =========================================================
    // GETTERS / SETTERS
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

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public void setPdfBytes(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
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

    public void setReportGenerated(
            boolean reportGenerated) {
        this.reportGenerated = reportGenerated;
    }

    public DtdtsTicketDaoImpl getTicketDao() {
        return ticketDao;
    }

    public void setTicketDao(DtdtsTicketDaoImpl ticketDao) {
        this.ticketDao = ticketDao;
    }

}
