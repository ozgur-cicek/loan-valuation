package com.ozgoru.loanvaluation.provider.model;

import java.util.Date;
import java.util.UUID;

public class LoanDetail {

    public LoanDetail() {
    }

    private UUID id;
    private Date date;
    private int installment;
    private long amount;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getInstallment() {
        return installment;
    }

    public void setInstallment(int installment) {
        this.installment = installment;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

}
