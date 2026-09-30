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
import java.util.Objects;
import java.util.function.Function;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sandbox.ph.generatejasper.dtdts.daoImpl.DTDTSDaoImpl;
import com.sandbox.ph.generatejasper.dtdts.dto.DTDTSDto;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Named("dtdtsBean")
@ViewScoped
public class DTDTSBean implements Serializable {

    // ═══════════════════════════════════════════════════════════
    // ⚙️ CONSTANTS & CONFIGURATION
    // ═══════════════════════════════════════════════════════════
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DTDTSBean.class);

    private static final String MSG_REQUIRED = "Required";
    private static final String MSG_WARNING = "Warning";
    private static final String MSG_ERROR = "Error";
    private static final String LOG_SEPARATOR = "-------------------------------------------------";

    private static final String DTDTS_REPORT = "/reports/DTDTS.jrxml";
    private static final String REPORT_FILE_NAME = "DTDTS_Report.pdf";

    // ═══════════════════════════════════════════════════════════
    // 🗄️ CACHE (Performance Optimization)
    // ═══════════════════════════════════════════════════════════
    // ⚡ Cached to prevent compiling the JRXML on every request
    private static JasperReport compiledDtdtsReport;

    // ═══════════════════════════════════════════════════════════
    // 💉 DEPENDENCY INJECTION
    // ═══════════════════════════════════════════════════════════
    @Inject
    private DTDTSDaoImpl dtdtsDao;

    // ═══════════════════════════════════════════════════════════
    // 🔍 SEARCH FILTERS
    // ═══════════════════════════════════════════════════════════
    private Date fromDate;
    private Date toDate;
    private String orgaCode;

    // ═══════════════════════════════════════════════════════════
    // 📄 REPORT OUTPUTS
    // ═══════════════════════════════════════════════════════════
    private byte[] pdfBytes;
    private String pdfBase64;
    private boolean reportGenerated = false;

    // ═══════════════════════════════════════════════════════════
    // 🚀 INITIALIZATION
    // ═══════════════════════════════════════════════════════════
    public DTDTSBean() {
        log.info("DTDTSBean initialized");
    }

    @PostConstruct
    public void init() {
        // 🛠️ Compile the report once when the bean is first created
        if (compiledDtdtsReport == null) {
            compileReportLocally();
        }
    }

    private synchronized void compileReportLocally() {
        if (compiledDtdtsReport == null) {
            try (InputStream mainStream = DTDTSBean.class.getResourceAsStream(DTDTS_REPORT)) {
                if (mainStream == null) {
                    throw new IllegalStateException("DTDTS.jrxml was not found in " + DTDTS_REPORT);
                }
                log.info("Compiling DTDTS.jrxml initially...");
                compiledDtdtsReport = JasperCompileManager.compileReport(mainStream);
                log.info("✅ DTDTS.jrxml compiled and cached successfully.");
            } catch (Exception e) {
                log.error("❌ Failed to compile Jasper report on startup", e);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 🖨️ PDF GENERATION CORE LOGIC
    // ═══════════════════════════════════════════════════════════
    public void generatePdf() {
        long startTime = System.currentTimeMillis();

        log.info(LOG_SEPARATOR);
        log.info("▶️ START generatePdf() - DTDTDS Summary");
        log.info(LOG_SEPARATOR);

        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            log.error("❌ FacesContext is NULL.");
            return;
        }

        // 🧹 Reset previous state
        reportGenerated = false;
        pdfBytes = null;
        pdfBase64 = null;

        // 🛡️ Validation
        if (!isValidFilters(context)) {
            return;
        }

        if (compiledDtdtsReport == null) {
            addErrorMessage(context, "Report template is not available. Check server logs.");
            return;
        }

        try {
            // 📥 Fetch Data
            log.info("Calling DTDTS DAO...");
            List<DTDTSDto> rows = dtdtsDao.findDTDTSDataByCriteria(fromDate, toDate, orgaCode);
            log.info("📊 DTDTS records found: {}", rows != null ? rows.size() : 0);

            if (rows == null || rows.isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, MSG_WARNING,
                        "No data found for the selected DTDTS filters."));
                return;
            }

            // 🧮 Compute Totals (Null-Safe)
            BigDecimal totalDr = sumBigDecimals(rows, DTDTSDto::getLcDr);
            BigDecimal totalCr = sumBigDecimals(rows, DTDTSDto::getLcCr);
            BigDecimal totalOpn = sumBigDecimals(rows, DTDTSDto::getLcOpn);
            BigDecimal totalCls = sumBigDecimals(rows, DTDTSDto::getLcCls);

            // 📝 Populate Report Parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("prgid", "FA51003R");
            parameters.put("user", "jay");
            parameters.put("comp", "BSP");
            parameters.put("filename", "DTDTS SUMMARY REPORT");
            parameters.put("date", new SimpleDateFormat("yyyy/MM/dd").format(new Date()));

            parameters.put("lcOpnTotal", totalOpn);
            parameters.put("lcOpnTotalSign", "");
            parameters.put("lcDrTotal", totalDr);
            parameters.put("lcCrTotal", totalCr);
            parameters.put("lcClsTotal", totalCls);
            parameters.put("lcClsTotalSign", "");

            // 🏗️ Build Data Source & Fill
            log.info("JRBeanCollectionDataSource created. Filling report...");
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);
            JasperPrint jasperPrint = JasperFillManager.fillReport(compiledDtdtsReport, parameters, dataSource);

            if (jasperPrint == null || jasperPrint.getPages().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, MSG_WARNING,
                        "No data generated in DTDTS report pages."));
                return;
            }

            // 📦 Export to PDF
            pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            if (pdfBytes == null || pdfBytes.length == 0) {
                addErrorMessage(context, "Generated DTDTS PDF is empty.");
                return;
            }

            pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            reportGenerated = true;

            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "✅ DTDTS report generated successfully."));
            log.info("📄 DTDTS PDF generated successfully. Size={} bytes", pdfBytes.length);

        } catch (Exception e) {
            log.error("❌ Failed to generate DTDTS report", e);
            pdfBytes = null;
            pdfBase64 = null;
            reportGenerated = false;

            String message = (e.getMessage() == null || e.getMessage().isBlank()) ? e.getClass().getSimpleName()
                    : e.getMessage();
            addErrorMessage(context, "Failed to generate DTDTS report: " + message);
        } finally {
            log.info("⏱️ DTDTS execution time = {} ms", System.currentTimeMillis() - startTime);
            log.info("⏹️ END generatePdf() - DTDTS Summary\n" + LOG_SEPARATOR);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 🛠️ HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * 🧮 Safely sums BigDecimals, filtering out nulls to prevent
     * NullPointerException.
     */
    private BigDecimal sumBigDecimals(List<DTDTSDto> rows, Function<DTDTSDto, BigDecimal> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 📢 Standardizes error messaging for JSF context.
     */
    private void addErrorMessage(FacesContext context, String detail) {
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, MSG_ERROR, detail));
    }

    /**
     * 🛡️ Validates user inputs before report generation.
     */
    private boolean isValidFilters(FacesContext context) {
        if (fromDate == null || toDate == null) {
            addErrorMessage(context, "From Date and To Date are required for DTDTS.");
            return false;
        }
        if (fromDate.after(toDate)) {
            addErrorMessage(context, "From Date cannot be later than To Date.");
            return false;
        }
        if (orgaCode == null || orgaCode.isBlank()) {
            addErrorMessage(context, "Organization Unit Code is required for DTDTS.");
            return false;
        }
        return true;
    }

    /**
     * 🧹 Resets all filters and clears the generated report from memory.
     */
    public void clearFilters() {
        log.info("🧹 Clearing DTDTS filters...");
        fromDate = null;
        toDate = null;
        orgaCode = null;
        pdfBytes = null;
        pdfBase64 = null;
        reportGenerated = false;
    }

    /**
     * 📥 Provides the StreamedContent required by PrimeFaces <p:fileDownload>.
     */
    public StreamedContent getFile() {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }
        return DefaultStreamedContent.builder()
                .name(REPORT_FILE_NAME)
                .contentType("application/pdf")
                .stream(() -> new ByteArrayInputStream(pdfBytes))
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // 🔄 GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════

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
