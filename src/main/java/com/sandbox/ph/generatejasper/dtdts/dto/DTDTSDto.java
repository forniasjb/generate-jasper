package com.sandbox.ph.generatejasper.dtdts.dto; // O kung saan man nakalagay ang DTDTSDto mo

import java.math.BigDecimal;

public class DTDTSDto {
    private String code;
    private String desc;
    private BigDecimal lcOpn;
    private String lcOpnSign;
    private BigDecimal lcDr;
    private BigDecimal lcCr;
    private BigDecimal lcCls;
    private String lcClsSign;

    public DTDTSDto() {
    }

    // Original getters/setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCODE() {
        return code;
    }

    public String getDESC() {
        return desc;
    }

    public BigDecimal getLCOPN() {
        return lcOpn;
    }

    public String getLCOPN_SIGN() {
        return lcOpnSign;
    }

    public BigDecimal getLCDR() {
        return lcDr;
    }

    public BigDecimal getLCCR() {
        return lcCr;
    }

    public BigDecimal getLCCLS() {
        return lcCls;
    }

    public String getLCCLS_SIGN() {
        return lcClsSign;
    }


    public BigDecimal getLcOpn() {
        return lcOpn;
    }

    public void setLcOpn(BigDecimal lcOpn) {
        this.lcOpn = lcOpn;
    }

    public String getLcOpnSign() {
        return lcOpnSign;
    }

    public void setLcOpnSign(String lcOpnSign) {
        this.lcOpnSign = lcOpnSign;
    }

    public BigDecimal getLcDr() {
        return lcDr;
    }

    public void setLcDr(BigDecimal lcDr) {
        this.lcDr = lcDr;
    }

    public BigDecimal getLcCr() {
        return lcCr;
    }

    public void setLcCr(BigDecimal lcCr) {
        this.lcCr = lcCr;
    }

    public BigDecimal getLcCls() {
        return lcCls;
    }

    public void setLcCls(BigDecimal lcCls) {
        this.lcCls = lcCls;
    }

    public String getLcClsSign() {
        return lcClsSign;
    }

    public void setLcClsSign(String lcClsSign) {
        this.lcClsSign = lcClsSign;
    }
}