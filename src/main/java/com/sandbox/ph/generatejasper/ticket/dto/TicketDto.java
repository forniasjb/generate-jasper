package com.sandbox.ph.generatejasper.ticket.dto;

import java.math.BigDecimal;
import java.util.Date;

public class TicketDto {

    private String rcCode;
    private String rcDesc;
    private String transType;
    private String transTypeDesc;
    private String transId;
    private String transIdDesc;
    private String ticketNo;
    private Date transDate;
    private String preparedBy;
    private String reviewedBy;
    private String approvedBy;
    private String glAccountCode;
    private String glAccountDescription;
    private String analysisCode;
    private String analysisDescription;
    private BigDecimal debit;
    private BigDecimal credit;
    private String particulars;
    private String institutionName;

    // =========================================================
    // Standard Getters and Setters
    // =========================================================

    public String getRcCode() {
        return rcCode;
    }

    public void setRcCode(String rcCode) {
        this.rcCode = rcCode;
    }

    public String getRcDesc() {
        return rcDesc;
    }

    public void setRcDesc(String rcDesc) {
        this.rcDesc = rcDesc;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getTransTypeDesc() {
        return transTypeDesc;
    }

    public void setTransTypeDesc(String transTypeDesc) {
        this.transTypeDesc = transTypeDesc;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public String getTransIdDesc() {
        return transIdDesc;
    }

    public void setTransIdDesc(String transIdDesc) {
        this.transIdDesc = transIdDesc;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public Date getTransDate() {
        return transDate;
    }

    public void setTransDate(Date transDate) {
        this.transDate = transDate;
    }

    public String getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(String preparedBy) {
        this.preparedBy = preparedBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getGlAccountCode() {
        return glAccountCode;
    }

    public void setGlAccountCode(String glAccountCode) {
        this.glAccountCode = glAccountCode;
    }

    public String getGlAccountDescription() {
        return glAccountDescription;
    }

    public void setGlAccountDescription(String glAccountDescription) {
        this.glAccountDescription = glAccountDescription;
    }

    public String getAnalysisCode() {
        return analysisCode;
    }

    public void setAnalysisCode(String analysisCode) {
        this.analysisCode = analysisCode;
    }

    public String getAnalysisDescription() {
        return analysisDescription;
    }

    public void setAnalysisDescription(String analysisDescription) {
        this.analysisDescription = analysisDescription;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getParticulars() {
        return particulars;
    }

    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    // =========================================================
    // Uppercase Getters (JasperReports .jrxml fields)
    // =========================================================

    public String getRC_CODE() {
        return rcCode;
    }

    public String getRC_DESC() {
        return rcDesc;
    }

    public String getTRANS_TYPE() {
        return transType;
    }

    public String getTRANS_TYPE_DESC() {
        return transTypeDesc;
    }

    public String getTRANS_ID() {
        return transId;
    }

    public String getTRANS_ID_DESC() {
        return transIdDesc;
    }

    public String getTICKET_NO() {
        return ticketNo;
    }

    public Date getTRANS_DATE() {
        return transDate;
    }

    public String getPREPARED_BY() {
        return preparedBy;
    }

    public String getREVIEWED_BY() {
        return reviewedBy;
    }

    public String getAPPROVED_BY() {
        return approvedBy;
    }

    public String getGL_ACCOUNT_CODE() {
        return glAccountCode;
    }

    public String getGL_ACCOUNT_DESCRIPTION() {
        return glAccountDescription;
    }

    public String getANALYSIS_CODE() {
        return analysisCode;
    }

    public String getANALYSIS_DESCRIPTION() {
        return analysisDescription;
    }

    public BigDecimal getDEBIT() {
        return debit;
    }

    public BigDecimal getCREDIT() {
        return credit;
    }

    public String getPARTICULARS() {
        return particulars;
    }

    public String getINSTITUTION_NAME() {
        return institutionName;
    }
}
