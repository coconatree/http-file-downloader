package org.example.model;

import java.sql.Date;

public class UsageNotification {
    private String usedBy;
    private Date usedAt;

    public UsageNotification(String usedBy, Date usedAt) {
        setUsedBy(usedBy);
        setUsedBy(usedAt);
    }

    public String getUsedBy() { return this.usedBy; }
    public Date getUsedAt() { return this.usedAt; }

    public void setUsedBy(String usedBy) {
        if (usedBy.isEmpty()) {
            throw new IllegalArgumentException(
                    "String can't be empty"
            );
        }
        this.usedBy = usedBy;
    }

    public void setUsedBy(Date date) {
        if (date.before(new Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException(
                    "Date of the usage notification can't be before the current date"
            );
        }
        this.usedAt = date;
    }
}
