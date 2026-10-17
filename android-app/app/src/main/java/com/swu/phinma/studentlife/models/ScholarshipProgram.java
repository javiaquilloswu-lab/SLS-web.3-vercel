package com.swu.phinma.studentlife.models;

import java.io.Serializable;

public class ScholarshipProgram implements Serializable {
    private String id;
    private String code;
    private String title;
    private String description;
    private String academicYear;
    private String deadline;
    private String stipendAmount;
    private boolean isActive;
    private boolean isExpired;

    public ScholarshipProgram(String id, String code, String title, String description, String academicYear, String deadline, String stipendAmount, boolean isActive, boolean isExpired) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.academicYear = academicYear;
        this.deadline = deadline;
        this.stipendAmount = stipendAmount;
        this.isActive = isActive;
        this.isExpired = isExpired;
    }

    public String getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAcademicYear() { return academicYear; }
    public String getDeadline() { return deadline; }
    public String getStipendAmount() { return stipendAmount; }
    public boolean isActive() { return isActive; }
    public boolean isExpired() { return isExpired; }
}
