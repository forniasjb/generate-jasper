package com.sandbox.ph.generatejasper.dtdts.daoImpl;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.sandbox.ph.generatejasper.dtdts.dto.DTDTSDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@ApplicationScoped
public class DTDTSDaoImpl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(DTDTSDaoImpl.class.getName());
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("db2PU");
    private static final String DATE_FORMAT_PATTERN = "yyyyMMdd";

    /**
     * Retrieves DTDTS summary report data based on criteria.
     */
    public List<DTDTSDto> findDTDTSDataByCriteria(Date fromDate, Date toDate, String orga) {
        Integer fDateNum = convertDateToInteger(fromDate);
        Integer tDateNum = convertDateToInteger(toDate);

        logInputFilters(fromDate, toDate, fDateNum, tDateNum, orga);

        String sql = buildQuery();

        List<Object[]> rawResults;
        try (EntityManager em = emf.createEntityManager()) {
            @SuppressWarnings("unchecked")
            List<Object[]> queryResult = em.createNativeQuery(sql)
                    .setParameter("fromDate", fDateNum)
                    .setParameter("toDate", tDateNum)
                    .setParameter("orgaCode", orga != null ? orga.trim() : "")
                    .getResultList();
            rawResults = queryResult;
        }

        LOGGER.info(() -> String.format("DTDTS Query executed successfully. Total raw rows fetched: %d",
                (rawResults != null ? rawResults.size() : 0)));

        return rawResults.stream()
                .map(this::mapRowToDto)
                .collect(Collectors.toList());
    }

    private Integer convertDateToInteger(Date date) {
        if (date == null) return 20250101;
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return Integer.parseInt(sdf.format(date));
    }

    private void logInputFilters(Date fromDate, Date toDate, Integer fDateNum, Integer tDateNum, String orga) {
        LOGGER.info("=== DTDTS SUMMARY REPORT FILTER DEBUG ===");
        LOGGER.info(() -> String.format("From Date (Date): %s | Converted Integer: %d", fromDate, fDateNum));
        LOGGER.info(() -> String.format("To Date (Date): %s | Converted Integer: %d", toDate, tDateNum));
        LOGGER.info(() -> String.format("Organization Unit Code (orga): %s", orga));
    }

    private String buildQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  a.orgaunit AS CODE, ");
        sql.append("  f.orgadesc AS DESC, ");
        sql.append("  CAST(0.00 AS DECIMAL(15,2)) AS LCOPN, ");
        sql.append("  '' AS LCOPN_SIGN, ");
        sql.append("  COALESCE(SUM(CASE WHEN TRIM(a.sign) = '+' THEN a.lcamt ELSE 0 END), 0.00) AS LCDR, ");
        sql.append("  COALESCE(SUM(CASE WHEN TRIM(a.sign) = '-' THEN a.lcamt ELSE 0 END), 0.00) AS LCCR, ");
        sql.append("  CAST(0.00 AS DECIMAL(15,2)) AS LCCLS, ");
        sql.append("  '' AS LCCLS_SIGN ");
        sql.append("FROM CFASLIB.FA0210 a ");
        sql.append("LEFT OUTER JOIN CFASLIB.FA0740 f ON a.orgaunit = f.orgaunit ");
        sql.append("WHERE a.trdat >= :fromDate AND a.trdat <= :toDate ");
        sql.append("  AND (:orgaCode = '' OR a.orgaunit = :orgaCode) ");
        sql.append("  AND (a.tridtype BETWEEN '1' AND '9' OR a.tridtype IS NULL) ");
        sql.append("GROUP BY a.orgaunit, f.orgadesc ");
        sql.append("ORDER BY a.orgaunit");
        return sql.toString();
    }

    private DTDTSDto mapRowToDto(Object[] row) {
        DTDTSDto dto = new DTDTSDto();
        dto.setCode(getStringValue(row[0]));
        dto.setDesc(getStringValue(row[1]));
        dto.setLcOpn(row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO);
        dto.setLcOpnSign(getStringValue(row[3]));
        dto.setLcDr(row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO);
        dto.setLcCr(row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO);
        dto.setLcCls(row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO);
        dto.setLcClsSign(row[7] != null ? String.valueOf(row[7]).trim() : "");

        return dto;
    }
    private String getStringValue(Object obj) {
        if (obj == null) {
            return "";
        }
        return String.valueOf(obj).trim();
    }

    public JasperPrint generateDtdtsReport(JasperReport jasperReport,
                                           Map<String, Object> parameters,
                                           Date fromDate,
                                           Date toDate,
                                           String orga) throws JRException {

        List<DTDTSDto> dtoList = findDTDTSDataByCriteria(fromDate, toDate, orga);
        LOGGER.info(() -> String.format("Passing %d DTDTS records to JasperFillManager.",
                (dtoList != null ? dtoList.size() : 0)));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dtoList);

        Map<String, Object> safeParameters = (parameters != null) ? parameters : new HashMap<>();
        return JasperFillManager.fillReport(jasperReport, safeParameters, dataSource);
    }

    public JasperPrint generateDtdtsReport(InputStream reportTemplateStream, Map<String, Object> parameters,
                                           Date fromDate, Date toDate, String orga) throws JRException {

        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplateStream);
        return generateDtdtsReport(jasperReport, parameters, fromDate, toDate, orga);
    }
}