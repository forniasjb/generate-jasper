package com.sandbox.ph.generatejasper.dtdts.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
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

import com.sandbox.ph.generatejasper.dtdts.daoImpl.DTDTSDaoImpl;
import com.sandbox.ph.generatejasper.dtdts.dto.DTDTSDto;

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

@Named("dtdtsBean")
@ViewScoped
public class DTDTSBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DTDTSBean.class);
    private static final String MSG_REQUIRED = "Required";
    private static final String MSG_WARNING = "Warning";
    private static final String MSG_ERROR = "Error";

    // =========================================================
    // REPORT RESOURCE PATHS
    // =========================================================
    private static final String LOG_SEPARATOR = "-------------------------------------------------";
    private static final String DTDTS_REPORT = "/reports/DTDTS.jrxml";

    // =========================================================
    // DAO
    // =========================================================

    private transient DTDTSDaoImpl dtdtsDao = new DTDTSDaoImpl();

    // =========================================================
    // DTDTS FILTERS
    // =========================================================

    private Date fromDate;
    private Date toDate;
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

    public DTDTSBean() {
        log.info("DTDTSBean initialized");
    }

    // =========================================================
    // GENERATE DTDTS PDF
    // =========================================================

    public void generatePdf() {

        long startTime = System.currentTimeMillis();

        log.info(LOG_SEPARATOR);
        log.info("START generatePdf() - DTDTDS Summary");
        log.info(LOG_SEPARATOR);

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

        try (InputStream mainStream = DTDTSBean.class.getResourceAsStream(DTDTS_REPORT)) {

            if (mainStream == null) {
                throw new IllegalStateException("DTDTS.jrxml was not found in " + DTDTS_REPORT);
            }

            log.info("Found DTDTS report: {}", DTDTS_REPORT);

            // =================================================
            // COMPILE REPORT
            // =================================================

            log.info("Compiling DTDTS.jrxml...");
            JasperReport dtdtsReport = JasperCompileManager.compileReport(mainStream);
            log.info("DTDTS.jrxml compiled successfully.");

            // =================================================
            // GET DATA FROM DAO
            // =================================================

            log.info("Calling DTDTS DAO...");

            List<DTDTSDto> rows = dtdtsDao.findDTDTSDataByCriteria(
                    fromDate, toDate, orgaCode);

            log.info("DTDTS records found: {}", rows != null ? rows.size() : 0);

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
            // COMPUTE TOTALS FOR PARAMETERS (Kung kailangan sa Summary)
            // =================================================
            BigDecimal totalDr = rows.stream().map(DTDTSDto::getLcDr).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCr = rows.stream().map(DTDTSDto::getLcCr).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOpn = rows.stream().map(DTDTSDto::getLcOpn).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCls = rows.stream().map(DTDTSDto::getLcCls).reduce(BigDecimal.ZERO, BigDecimal::add);

            // =================================================
            // PARAMETERS
            // =================================================

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("prgid", "FA51003R");
            parameters.put("user", "jay");
            parameters.put("comp", "BSP");
            parameters.put("filename", "DTDTS SUMMARY REPORT");
            parameters.put("date", new SimpleDateFormat("yyyy/MM/dd").format(new Date()));

            // I-pass ang computed totals sa JRXML parameters
            parameters.put("lcOpnTotal", totalOpn);
            parameters.put("lcOpnTotalSign", "");
            parameters.put("lcDrTotal", totalDr);
            parameters.put("lcCrTotal", totalCr);
            parameters.put("lcClsTotal", totalCls);
            parameters.put("lcClsTotalSign", "");

            log.info("DTDTS parameters prepared.");

            // =================================================
            // CREATE DATA SOURCE & FILL REPORT
            // =================================================

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);
            log.info("JRBeanCollectionDataSource created. Filling report...");

            JasperPrint jasperPrint = JasperFillManager.fillReport(
                    dtdtsReport,
                    parameters,
                    dataSource);

            if (jasperPrint == null || jasperPrint.getPages().isEmpty()) {
                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                MSG_WARNING,
                                "No data generated in DTDTS report pages."));
                return;
            }

            log.info("Generated report pages: {}", jasperPrint.getPages().size());

            // =================================================
            // EXPORT PDF
            // =================================================

            pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            if (pdfBytes == null || pdfBytes.length == 0) {
                reportGenerated = false;
                context.addMessage(
                        null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                MSG_ERROR,
                                "Generated DTDTS PDF is empty."));
                return;
            }

            pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            reportGenerated = true;

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "DTDTS report generated successfully."));

            log.info("DTDTS PDF generated successfully. Size={} bytes", pdfBytes.length);

        } catch (Exception e) {
            log.error("Failed to generate DTDTS report", e);
            pdfBytes = null;
            pdfBase64 = null;
            reportGenerated = false;

            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }

            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_ERROR,
                            "Failed to generate DTDTS report: " + message));
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("DTDTS execution time = {} ms", executionTime);
            log.info("END generatePdf() - DTDTS Summary");
            log.info(LOG_SEPARATOR);
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean isValidFilters(FacesContext context) {
        if (fromDate == null || toDate == null) {
            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            MSG_REQUIRED,
                            "From Date and To Date are required for DTDTS."));
            return false;
        }

        if (fromDate.after(toDate)) {
            context.addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Invalid Date Range",
                            "From Date cannot be later than To Date."));
            return false;
        }

        if (orgaCode == null || orgaCode.isBlank()) {
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
        fromDate = null;
        toDate = null;
        orgaCode = null;
        pdfBytes = null;
        pdfBase64 = null;
        reportGenerated = false;
    }

    // =========================================================
    // DOWNLOAD PDF
    // =========================================================

    public StreamedContent getFile() {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }

        return DefaultStreamedContent
                .builder()
                .name("DTDTS_Report.pdf")
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(pdfBytes))
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

    public void setReportGenerated(boolean reportGenerated) {
        this.reportGenerated = reportGenerated;
    }

    public DTDTSDaoImpl getDtdtsDao() {
        return dtdtsDao;
    }

    public void setDtdtsDao(DTDTSDaoImpl dtdtsDao) {
        this.dtdtsDao = dtdtsDao;
    }
}