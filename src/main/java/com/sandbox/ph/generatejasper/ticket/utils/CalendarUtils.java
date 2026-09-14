package com.sandbox.ph.generatejasper.ticket.utils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.event.SelectEvent;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("calendarUtils")
@ViewScoped
public class CalendarUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate txnStatFromDate;
    private LocalDate txnStatToDate;

    private List<LocalDate> invalidDates;
    private List<LocalDate> validDates;
    private LocalDate minDate;
    private LocalDate maxDate;

    @PostConstruct
    public void init() {
        invalidDates = new ArrayList<>();
        invalidDates.add(LocalDate.now());
        for (int i = 0; i < 5; i++) {
            invalidDates.add(invalidDates.get(i).plusDays(1));
        }

        validDates = new ArrayList<>();
        validDates.add(LocalDate.now());
        for (int i = 0; i < 5; i++) {
            validDates.add(validDates.get(i).plusDays(1));
        }

        minDate = LocalDate.now().minusYears(1);
        maxDate = LocalDate.now().plusYears(1);
    }

    public LocalDate getTxnStatFromDate() {
        return txnStatFromDate;
    }

    public void setTxnStatFromDate(LocalDate txnStatFromDate) {
        this.txnStatFromDate = txnStatFromDate;
    }

    public LocalDate getTxnStatToDate() {
        return txnStatToDate;
    }

    public void setTxnStatToDate(LocalDate txnStatToDate) {
        this.txnStatToDate = txnStatToDate;
    }

    public List<LocalDate> getInvalidDates() {
        return invalidDates;
    }

    public void setInvalidDates(List<LocalDate> invalidDates) {
        this.invalidDates = invalidDates;
    }

    public List<LocalDate> getValidDates() {
        return validDates;
    }

    public void setValidDates(List<LocalDate> validDates) {
        this.validDates = validDates;
    }

    public LocalDate getMinDate() {
        return minDate;
    }

    public void setMinDate(LocalDate minDate) {
        this.minDate = minDate;
    }

    public LocalDate getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(LocalDate maxDate) {
        this.maxDate = maxDate;
    }

    public void onDateSelect(SelectEvent<LocalDate> event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", event.getObject().format(formatter)));
    }
}
