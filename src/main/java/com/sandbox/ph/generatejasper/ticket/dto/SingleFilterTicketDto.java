package com.sandbox.ph.generatejasper.ticket.dto;

import java.math.BigDecimal;
import java.util.Date;

public class SingleFilterTicketDto {

    private String rcCode;
    private String rcDesc;
    private Date transDate;
    private String transType;
    private String ticketNo;
    private String particulars;
    private String analysisCode;
    private String institutionName;
    private BigDecimal debit;
    private BigDecimal credit;

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

    public Date getTransDate() {
        return transDate;
    }

    public void setTransDate(Date transDate) {
        this.transDate = transDate;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getParticulars() {
        return particulars;
    }

    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }

    public String getAnalysisCode() {
        return analysisCode;
    }

    public void setAnalysisCode(String analysisCode) {
        this.analysisCode = analysisCode;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
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

    public String getRC_CODE() {
        return rcCode;
    }

    public String getRC_DESC() {
        return rcDesc;
    }

    public Date getTRANS_DATE() {
        return transDate;
    }

    public String getTRANS_TYPE() {
        return transType;
    }

    public String getTICKET_NO() {
        return ticketNo;
    }

    public String getPARTICULARS() {
        return particulars;
    }

    public String getANALYSIS_CODE() {
        return analysisCode;
    }

    public String getINSTITUTION_NAME() {
        return institutionName;
    }

    public BigDecimal getDEBIT() {
        return debit;
    }

    public BigDecimal getCREDIT() {
        return credit;
    }
}
