package com.sandbox.ph.generatejasper.ticket.daoImpl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.sandbox.ph.generatejasper.ticket.dto.TicketDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class TicketDaoImpl {
    private static final Logger LOGGER = Logger.getLogger(TicketDaoImpl.class.getName());
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("db2PU");
    private static final String DATE_FORMAT_PATTERN = "yyyyMMdd";

    public List<TicketDto> findReportDataByCriteria(Date fromDate, Date toDate, String ticketFrom, String ticketTo, String orga) {
        Integer fDateNum = convertDateToInteger(fromDate);
        Integer tDateNum = convertDateToInteger(toDate);
        Integer fTicket = parseTicket(ticketFrom);
        Integer tTicket = parseTicket(ticketTo);

        logInputFilters(fromDate, toDate, fDateNum, tDateNum, fTicket, tTicket, orga);

        String sql = buildQuery();

        List<Object[]> rawResults;
        try (EntityManager em = emf.createEntityManager()) {
            @SuppressWarnings("unchecked")
            List<Object[]> queryResult = em.createNativeQuery(sql)
                    .setParameter(1, fDateNum)
                    .setParameter(2, tDateNum)
                    .setParameter(3, fTicket)
                    .setParameter(4, tTicket)
                    .setParameter(5, orga)
                    .getResultList();
            rawResults = queryResult;
        }

        LOGGER.info(() -> String.format("Query executed successfully. Total raw rows fetched: %d", (rawResults != null ? rawResults.size() : 0)));

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return rawResults.stream()
                .map(row -> mapRowToDto(row, sdf))
                .collect(Collectors.toList());
    }

    private Integer convertDateToInteger(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return Integer.parseInt(sdf.format(date));
    }

    private Integer parseTicket(String ticketStr) {
        return ticketStr != null ? Integer.parseInt(ticketStr.trim()) : 0;
    }

    private void logInputFilters(Date fromDate, Date toDate, Integer fDateNum, Integer tDateNum, Integer fTicket, Integer tTicket, String orga) {
        LOGGER.info("=== REPORT FILTER DEBUG ===");
        LOGGER.info(() -> String.format("From Date (Date): %s | Converted Integer: %d", fromDate, fDateNum));
        LOGGER.info(() -> String.format("To Date (Date): %s | Converted Integer: %d", toDate, tDateNum));
        LOGGER.info(() -> String.format("Ticket From: %d", fTicket));
        LOGGER.info(() -> String.format("Ticket To: %d", tTicket));
        LOGGER.info(() -> String.format("Organization Unit Code (orga): %s", orga));
    }

    private String buildQuery() {
        return "SELECT " +
                "a.orgaunit AS RC_CODE, f.orgadesc AS RC_DESC, a.trtype AS TRANS_TYPE, " +
                "d.trdesc AS TRANS_TYPE_DESC, a.trid AS TRANS_ID, e.triddesc AS TRANS_ID_DESC, " +
                "a.ticket AS TICKET_NO, CAST(a.trdat AS VARCHAR(10)) AS TRANS_DATE, a.prousr AS PREPARED_BY, " +
                "'' AS REVIEWED_BY, '' AS APPROVED_BY, a.glcode AS GL_ACCOUNT_CODE, " +
                "b.gldesc AS GL_ACCOUNT_DESCRIPTION, a.analcode AS ANALYSIS_CODE, " +
                "y1.anadesc AS ANALYSIS_DESCRIPTION, " +
                "CASE WHEN TRIM(a.sign) = '+' THEN a.lcamt ELSE 0 END AS DEBIT, " +
                "CASE WHEN TRIM(a.sign) = '-' THEN a.lcamt ELSE 0 END AS CREDIT, " +
                "CAST(COALESCE(b1.part1, '') || COALESCE(b2.part2, '') AS VARCHAR(250)) AS PARTICULARS " +
                "FROM CFASLIB.FA0210 a " +
                "LEFT OUTER JOIN CFASLIB.FA0211LF1 b1 ON a.trdat = b1.trdat AND a.orgaunit = b1.orgaunit AND a.ticket = b1.ticket AND a.postdate = b1.postdate AND a.posttime = b1.posttime " +
                "LEFT OUTER JOIN CFASLIB.FA0211LF2 b2 ON a.trdat = b2.trdat AND a.orgaunit = b2.orgaunit AND a.ticket = b2.ticket AND a.postdate = b2.postdate AND a.posttime = b2.posttime " +
                "LEFT OUTER JOIN CFASLIB.FA0150 y1 ON a.analtype = y1.analtype AND a.analcode = y1.analcode " +
                "LEFT OUTER JOIN CFASLIB.FA0720 b ON a.glcode = b.glcode " +
                "LEFT OUTER JOIN CFASLIB.FA0710 d ON a.trtype = d.trtype " +
                "LEFT OUTER JOIN CFASLIB.FA0630 e ON a.trid = e.trid AND e.trline = 0 " +
                "LEFT OUTER JOIN CFASLIB.FA0740 f ON a.orgaunit = f.orgaunit " +
                "WHERE a.trdat >= ?1 AND a.trdat <= ?2 " +
                "AND a.ticket >= ?3 AND a.ticket <= ?4 " +
                "AND a.orgaunit = ?5 " +
                "AND (a.tridtype BETWEEN '1' AND '9' OR a.tridtype IS NULL) " +
                "ORDER BY a.postdate, a.posttime, a.trdat, a.ticket, a.sign, a.ticketseq";
    }

    private TicketDto mapRowToDto(Object[] row, SimpleDateFormat sdf) {
        TicketDto dto = new TicketDto();
        mapBasicFields(dto, row);
        mapDate(dto, row, sdf);
        mapFinancialsAndParticulars(dto, row);
        return dto;
    }

    private void mapBasicFields(TicketDto dto, Object[] row) {
        dto.setRcCode(row[0] != null ? row[0].toString().trim() : "");
        dto.setRcDesc(row[1] != null ? row[1].toString().trim() : "");
        dto.setTransType(row[2] != null ? row[2].toString().trim() : "");
        dto.setTransTypeDesc(row[3] != null ? row[3].toString().trim() : "");
        dto.setTransId(row[4] != null ? row[4].toString().trim() : "");
        dto.setTransIdDesc(row[5] != null ? row[5].toString().trim() : "");
        dto.setTicketNo(row[6] != null ? row[6].toString() : "");
        dto.setPreparedBy(row[8] != null ? row[8].toString().trim() : "");
        dto.setReviewedBy(row[9] != null ? row[9].toString().trim() : "");
        dto.setApprovedBy(row[10] != null ? row[10].toString().trim() : "");
        dto.setGlAccountCode(row[11] != null ? row[11].toString().trim() : "");
        dto.setGlAccountDescription(row[12] != null ? row[12].toString().trim() : "");
        dto.setAnalysisCode(row[13] != null ? row[13].toString().trim() : "");
        dto.setAnalysisDescription(row[14] != null ? row[14].toString().trim() : "");
    }

    private void mapDate(TicketDto dto, Object[] row, SimpleDateFormat sdf) {
        if (row[7] != null) {
            try {
                dto.setTransDate(sdf.parse(row[7].toString()));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Failed to parse transaction date: {0}", row[7]);
                dto.setTransDate(null);
            }
        }
    }

    private void mapFinancialsAndParticulars(TicketDto dto, Object[] row) {
        dto.setDebit(row[15] != null ? new BigDecimal(row[15].toString()) : BigDecimal.ZERO);
        dto.setCredit(row[16] != null ? new BigDecimal(row[16].toString()) : BigDecimal.ZERO);

        String particularsVal = row[17] != null ? row[17].toString().trim() : "";
        LOGGER.info(() -> String.format("DEBUG - Ticket: %s | Particulars: [%s]", row[6], particularsVal));
        dto.setParticulars(particularsVal);
    }

    public JasperPrint generateBankSummaryReport(JasperReport jasperReport,
                                                 Map<String, Object> parameters,
                                                 Date fromDate,
                                                 Date toDate,
                                                 String ticketFrom,
                                                 String ticketTo,
                                                 String orga) throws JRException {

        List<TicketDto> transactionList = findReportDataByCriteria(fromDate, toDate, ticketFrom, ticketTo, orga);
        LOGGER.info(() -> String.format("Passing %d records to JasperFillManager.", (transactionList != null ? transactionList.size() : 0)));
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(transactionList);

        Map<String, Object> safeParameters = (parameters != null) ? parameters : new HashMap<>();

        return JasperFillManager.fillReport(jasperReport, safeParameters, dataSource);
    }

    public JasperPrint generateBankSummaryReport(InputStream reportTemplateStream, Map<String, Object> parameters,
                                                 Date fromDate, Date toDate, String ticketFrom, String ticketTo, String orga) throws JRException {

        JasperReport jasperReport = JasperCompileManager.compileReport(reportTemplateStream);
        return generateBankSummaryReport(jasperReport, parameters, fromDate, toDate, ticketFrom, ticketTo, orga);
    }
}