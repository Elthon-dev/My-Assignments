package com.nordic.myassignments.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Assignment implements Serializable {
    private String id;
    private String title;
    private String subject;
    private String colorHex;
    private long dueTimestamp;
    private String priority; // HIGH, MEDIUM, LOW
    private String status;   // NOT_STARTED, IN_PROGRESS, COMPLETED
    private double estimatedHours;
    private int gradeWeight;
    private String notes;
    private List<SubTask> checklist;
    private int focusMinutesSpent;

    public Assignment() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.subject = "General";
        this.colorHex = "#52796F"; // Nordic sage
        this.dueTimestamp = System.currentTimeMillis() + 86400000L * 2; // in 2 days
        this.priority = "MEDIUM";
        this.status = "NOT_STARTED";
        this.estimatedHours = 2.0;
        this.gradeWeight = 10;
        this.notes = "";
        this.checklist = new ArrayList<>();
        this.focusMinutesSpent = 0;
    }

    public Assignment(String title, String subject, String colorHex, long dueTimestamp,
                      String priority, String status, double estimatedHours, int gradeWeight,
                      String notes, List<SubTask> checklist, int focusMinutesSpent) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.subject = subject;
        this.colorHex = colorHex;
        this.dueTimestamp = dueTimestamp;
        this.priority = priority;
        this.status = status;
        this.estimatedHours = estimatedHours;
        this.gradeWeight = gradeWeight;
        this.notes = notes != null ? notes : "";
        this.checklist = checklist != null ? checklist : new ArrayList<>();
        this.focusMinutesSpent = focusMinutesSpent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getColorHex() {
        return colorHex != null ? colorHex : "#52796F";
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public long getDueTimestamp() {
        return dueTimestamp;
    }

    public void setDueTimestamp(long dueTimestamp) {
        this.dueTimestamp = dueTimestamp;
    }

    public String getPriority() {
        return priority != null ? priority : "MEDIUM";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status != null ? status : "NOT_STARTED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public int getGradeWeight() {
        return gradeWeight;
    }

    public void setGradeWeight(int gradeWeight) {
        this.gradeWeight = gradeWeight;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SubTask> getChecklist() {
        if (checklist == null) {
            checklist = new ArrayList<>();
        }
        return checklist;
    }

    public void setChecklist(List<SubTask> checklist) {
        this.checklist = checklist;
    }

    public int getFocusMinutesSpent() {
        return focusMinutesSpent;
    }

    public void setFocusMinutesSpent(int focusMinutesSpent) {
        this.focusMinutesSpent = focusMinutesSpent;
    }

    public void addFocusMinutes(int minutes) {
        this.focusMinutesSpent += minutes;
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public void setCompleted(boolean completed) {
        this.status = completed ? "COMPLETED" : (getCompletedTasksCount() > 0 ? "IN_PROGRESS" : "NOT_STARTED");
    }

    public int getCompletedTasksCount() {
        if (checklist == null || checklist.isEmpty()) return 0;
        int count = 0;
        for (SubTask task : checklist) {
            if (task.isCompleted()) count++;
        }
        return count;
    }

    public int getTotalTasksCount() {
        return checklist != null ? checklist.size() : 0;
    }

    public int getProgressPercentage() {
        if (isCompleted()) return 100;
        if (checklist == null || checklist.isEmpty()) {
            return "IN_PROGRESS".equalsIgnoreCase(status) ? 50 : 0;
        }
        int completed = getCompletedTasksCount();
        return (int) Math.round(((double) completed / checklist.size()) * 100.0);
    }

    public String getFormattedDueDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(dueTimestamp));
    }

    public String getRelativeDueString() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar due = Calendar.getInstance();
        due.setTimeInMillis(dueTimestamp);
        due.set(Calendar.HOUR_OF_DAY, 0);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        long diffMillis = due.getTimeInMillis() - today.getTimeInMillis();
        long diffDays = diffMillis / (24 * 60 * 60 * 1000L);

        if (diffDays < 0) {
            long overdue = Math.abs(diffDays);
            return overdue == 1 ? "Overdue by 1 day" : "Overdue by " + overdue + " days";
        } else if (diffDays == 0) {
            return "Due Today";
        } else if (diffDays == 1) {
            return "Due Tomorrow";
        } else if (diffDays < 7) {
            return "Due in " + diffDays + " days";
        } else {
            return getFormattedDueDate();
        }
    }

    public boolean isDueSoon() {
        if (isCompleted()) return false;
        long diffMillis = dueTimestamp - System.currentTimeMillis();
        long diffDays = diffMillis / (24 * 60 * 60 * 1000L);
        return diffDays <= 3;
    }
}
