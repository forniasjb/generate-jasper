package com.sandbox.ph.generatejasper.ticket.daoImpl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.sandbox.ph.generatejasper.ticket.dto.SingleFilterTicketDto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@ApplicationScoped
public class SingleFilterDaoImpl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SingleFilterDaoImpl.class.getName());
    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("db2PU");

    /**
     * Retrieves report data based on the provided testFilter (Ticket filter).
     *
     * @param testFilter
     * @return
     */
    public List<SingleFilterTicketDto> findDataByTestFilter(String testFilter) {
        LOGGER.info("Executing findDataByTestFilter with filter: " + testFilter);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  CAST(a.ORGAUNIT AS VARCHAR(20)) AS RC_CODE, ");
        sql.append("  COALESCE(f.F0740_ORGAUNIT_DESC, 'N/A') AS RC_DESC, ");
        sql.append("  SUBSTR(DIGITS(DECIMAL(a.TRDAT, 8, 0)), 5, 2) || '/' || ");
        sql.append("  SUBSTR(DIGITS(DECIMAL(a.TRDAT, 8, 0)), 7, 2) || '/' || ");
        sql.append("  SUBSTR(DIGITS(DECIMAL(a.TRDAT, 8, 0)), 1, 4) AS TRANS_DATE, ");
        sql.append("  CAST(a.TRTYPE AS VARCHAR(20)) AS TRANS_TYPE, ");
        sql.append("  CAST(a.TICKET AS VARCHAR(20)) AS TICKET_NO, ");
        sql.append("  CAST(COALESCE(b1.PART1, '') || COALESCE(b2.PART2, '') AS VARCHAR(250)) AS PARTICULARS, ");
        sql.append("  a.ANALCODE AS ANALYSIS_CODE, ");
        sql.append("  COALESCE(f.F0740_ORGAUNIT_DESC, '') AS INSTITUTION_NAME, ");
        sql.append("  CASE WHEN TRIM(a.SIGN) = '+' THEN a.LCAMT ELSE 0 END AS DEBIT, ");
        sql.append("  CASE WHEN TRIM(a.SIGN) = '-' THEN a.LCAMT ELSE 0 END AS CREDIT ");
        sql.append("FROM CFASLIB.FA0210 a ");
        sql.append("LEFT JOIN CFASLIB.FA0211LF1 b1 ");
        sql.append(
                "  ON a.TRDAT = b1.TRDAT AND a.ORGAUNIT = b1.ORGAUNIT AND a.TICKET = b1.TICKET AND a.POSTDATE = b1.POSTDATE AND a.POSTTIME = b1.POSTTIME ");
        sql.append("LEFT JOIN CFASLIB.FA0211LF2 b2 ");
        sql.append(
                "  ON a.TRDAT = b2.TRDAT AND a.ORGAUNIT = b2.ORGAUNIT AND a.TICKET = b2.TICKET AND a.POSTDATE = b2.POSTDATE AND a.POSTTIME = b2.POSTTIME ");
        sql.append("LEFT JOIN CFASLIB.FA0740 f ");
        sql.append("  ON a.ORGAUNIT = f.F0740_ORGAUNIT_CODE ");
        sql.append("WHERE a.TRDAT BETWEEN 20250101 AND 20251231 ");
        sql.append("  AND a.TICKET BETWEEN 0 AND 999999 ");
        sql.append(
                "  AND (:testFilter IS NULL OR :testFilter = '' OR CAST(a.TICKET AS VARCHAR(20)) LIKE :likeFilter) ");
        sql.append("ORDER BY a.ORGAUNIT, a.TRDAT, a.TICKET, a.POSTDATE, a.POSTTIME, a.SIGN, a.TICKETSEQ ");
        sql.append("FETCH FIRST 10 ROWS ONLY");

        List<SingleFilterTicketDto> dtoList = new ArrayList<>();

        try (EntityManager em = emf.createEntityManager()) {

            String likePattern = (testFilter != null && !testFilter.isBlank()) ? "%" + testFilter.trim() + "%" : "%";

            List<Object[]> results = em.createNativeQuery(sql.toString())
                    .setParameter("testFilter", testFilter)
                    .setParameter("likeFilter", likePattern)
                    .getResultList();

            if (results != null && !results.isEmpty()) {
                // 2. I-setup SimpleDateFormatter to MM/dd/yyyy galing sa SQL
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

                for (Object[] row : results) {
                    SingleFilterTicketDto dto = new SingleFilterTicketDto();

                    dto.setRcCode((String) row[0]); // 0: RC_CODE
                    dto.setRcDesc((String) row[1]); // 1: RC_DESC

                    // 2. I-parse TRANS_DATE to String patungong java.util.Date
                    String dateStr = (String) row[2];
                    if (dateStr != null && !dateStr.isBlank()) {
                        try {
                            Date parsedDate = sdf.parse(dateStr);
                            dto.setTransDate(parsedDate);
                        } catch (Exception e) {
                            dto.setTransDate(null);
                        }
                    } else {
                        dto.setTransDate(null);
                    }

                    dto.setTransType((String) row[3]); // 3: TRANS_TYPE
                    dto.setTicketNo((String) row[4]); // 4: TICKET_NO
                    dto.setParticulars((String) row[5]); // 5: PARTICULARS
                    dto.setAnalysisCode((String) row[6]); // 6: ANALYSIS_CODE
                    dto.setInstitutionName((String) row[7]); // 7: INSTITUTION_NAME
                    dto.setDebit((BigDecimal) row[8]); // 8: DEBIT
                    dto.setCredit((BigDecimal) row[9]); // 9: CREDIT

                    dtoList.add(dto);
                }
            }

            LOGGER.info("Query executed successfully. Total rows mapped: " + dtoList.size());
        } catch (Exception e) {
            LOGGER.severe("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }

        return dtoList;
    }
}
