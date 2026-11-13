package com.nordic.myassignments.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nordic.myassignments.R;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.storage.AssignmentRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditAssignmentDialog extends Dialog {

    public interface OnAssignmentSavedListener {
        void onSaved(Assignment assignment, boolean isNew);
    }

    private final Assignment existingAssignment;
    private final OnAssignmentSavedListener listener;
    private final AssignmentRepository repository;

    private TextView tvDialogTitle;
    private EditText etDialogAssignmentTitle;
    private EditText etDialogSubject;
    private LinearLayout btnPickDueDate;
    private TextView tvPickedDueDate;
    private TextView chipPriorityHigh;
    private TextView chipPriorityMed;
    private TextView chipPriorityLow;
    private EditText etDialogEstimatedHours;
    private EditText etDialogGradeWeight;
    private EditText etDialogNotes;
    private LinearLayout layoutColorPicker;
    private TextView btnCancelDialog;
    private TextView btnSaveDialogAssignment;

    private long selectedDueTimestamp;
    private String selectedPriority = "MEDIUM";
    private String selectedColorHex = "#52796F"; // Nordic sage default

    private static final String[] MONOCHROME_PALETTE = {
            "#FFFFFF", // Pure White
            "#E5E5EA", // Platinum Silver
            "#A1A1AA", // Zinc Gray
            "#71717A", // Slate Gray
            "#52525B", // Neutral Gray
            "#3F3F46", // Charcoal
            "#27272A"  // Deep Obsidian
    };

    public AddEditAssignmentDialog(@NonNull Context context, Assignment existing, OnAssignmentSavedListener listener) {
        super(context);
        this.existingAssignment = existing;
        this.listener = listener;
        this.repository = AssignmentRepository.getInstance(context);

        if (existing != null) {
            this.selectedDueTimestamp = existing.getDueTimestamp();
            this.selectedPriority = existing.getPriority();
            this.selectedColorHex = existing.getColorHex();
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 2);
            this.selectedDueTimestamp = cal.getTimeInMillis();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_edit_assignment);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        initViews();
        setupColorSwatches();
        setupPriorityButtons();
        setupDatePicker();

        if (existingAssignment != null) {
            populateExistingData();
        } else {
            updateDueDateText();
        }
    }

    private void initViews() {
        tvDialogTitle = findViewById(R.id.tvDialogTitle);
        etDialogAssignmentTitle = findViewById(R.id.etDialogAssignmentTitle);
        etDialogSubject = findViewById(R.id.etDialogSubject);
        btnPickDueDate = findViewById(R.id.btnPickDueDate);
        tvPickedDueDate = findViewById(R.id.tvPickedDueDate);
        chipPriorityHigh = findViewById(R.id.chipPriorityHigh);
        chipPriorityMed = findViewById(R.id.chipPriorityMed);
        chipPriorityLow = findViewById(R.id.chipPriorityLow);
        etDialogEstimatedHours = findViewById(R.id.etDialogEstimatedHours);
        etDialogGradeWeight = findViewById(R.id.etDialogGradeWeight);
        etDialogNotes = findViewById(R.id.etDialogNotes);
        layoutColorPicker = findViewById(R.id.layoutColorPicker);
        btnCancelDialog = findViewById(R.id.btnCancelDialog);
        btnSaveDialogAssignment = findViewById(R.id.btnSaveDialogAssignment);

        btnCancelDialog.setOnClickListener(v -> dismiss());
        btnSaveDialogAssignment.setOnClickListener(v -> saveAssignment());
    }

    private void populateExistingData() {
        tvDialogTitle.setText("Edit Assignment Card");
        etDialogAssignmentTitle.setText(existingAssignment.getTitle());
        etDialogSubject.setText(existingAssignment.getSubject());
        etDialogEstimatedHours.setText(String.valueOf(existingAssignment.getEstimatedHours()));
        etDialogGradeWeight.setText(String.valueOf(existingAssignment.getGradeWeight()));
        etDialogNotes.setText(existingAssignment.getNotes());
        updateDueDateText();
        setPriority(existingAssignment.getPriority());
    }

    private void setupDatePicker() {
        btnPickDueDate.setOnClickListener(v -> {
            Calendar current = Calendar.getInstance();
            current.setTimeInMillis(selectedDueTimestamp);

            DatePickerDialog datePicker = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth, 23, 59, 0);
                        selectedDueTimestamp = chosen.getTimeInMillis();
                        updateDueDateText();
                    },
                    current.get(Calendar.YEAR),
                    current.get(Calendar.MONTH),
                    current.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });
    }

    private void updateDueDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        tvPickedDueDate.setText(sdf.format(new Date(selectedDueTimestamp)));
    }

    private void setupPriorityButtons() {
        chipPriorityHigh.setOnClickListener(v -> setPriority("HIGH"));
        chipPriorityMed.setOnClickListener(v -> setPriority("MEDIUM"));
        chipPriorityLow.setOnClickListener(v -> setPriority("LOW"));
        setPriority(selectedPriority);
    }

    private void setPriority(String priority) {
        selectedPriority = priority != null ? priority.toUpperCase() : "MEDIUM";

        chipPriorityHigh.setBackgroundResource("HIGH".equals(selectedPriority) ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipPriorityHigh.setTextColor("HIGH".equals(selectedPriority) ? Color.BLACK : Color.parseColor("#8E8E93"));

        chipPriorityMed.setBackgroundResource("MEDIUM".equals(selectedPriority) ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipPriorityMed.setTextColor("MEDIUM".equals(selectedPriority) ? Color.BLACK : Color.parseColor("#8E8E93"));

        chipPriorityLow.setBackgroundResource("LOW".equals(selectedPriority) ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipPriorityLow.setTextColor("LOW".equals(selectedPriority) ? Color.BLACK : Color.parseColor("#8E8E93"));
    }

    private void setupColorSwatches() {
        layoutColorPicker.removeAllViews();
        int sizePx = (int) (32 * getContext().getResources().getDisplayMetrics().density);
        int marginPx = (int) (6 * getContext().getResources().getDisplayMetrics().density);

        for (final String colorHex : MONOCHROME_PALETTE) {
            FrameLayout frame = new FrameLayout(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(marginPx, marginPx, marginPx, marginPx);
            frame.setLayoutParams(lp);

            View circle = new View(getContext());
            circle.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            circle.setBackgroundResource(R.drawable.bg_chip_pill);
            try {
                circle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));
            } catch (Exception ignored) {}

            frame.addView(circle);

            // Selection indicator
            if (colorHex.equalsIgnoreCase(selectedColorHex)) {
                frame.setPadding(3, 3, 3, 3);
            }

            frame.setOnClickListener(v -> {
                selectedColorHex = colorHex;
                setupColorSwatches();
            });

            layoutColorPicker.addView(frame);
        }
    }

    private void saveAssignment() {
        String title = etDialogAssignmentTitle.getText().toString().trim();
        String subject = etDialogSubject.getText().toString().trim();

        if (title.isEmpty()) {
            etDialogAssignmentTitle.setError("Please enter assignment title");
            etDialogAssignmentTitle.requestFocus();
            return;
        }

        if (subject.isEmpty()) {
            subject = "General";
        }

        double estHours = 2.0;
        try {
            estHours = Double.parseDouble(etDialogEstimatedHours.getText().toString().trim());
        } catch (Exception ignored) {}

        int weight = 15;
        try {
            weight = Integer.parseInt(etDialogGradeWeight.getText().toString().trim());
        } catch (Exception ignored) {}

        String notes = etDialogNotes.getText().toString().trim();

        boolean isNew = (existingAssignment == null);
        Assignment assignment;
        if (isNew) {
            assignment = new Assignment();
        } else {
            assignment = existingAssignment;
        }

        assignment.setTitle(title);
        assignment.setSubject(subject);
        assignment.setDueTimestamp(selectedDueTimestamp);
        assignment.setPriority(selectedPriority);
        assignment.setColorHex(selectedColorHex);
        assignment.setEstimatedHours(estHours);
        assignment.setGradeWeight(weight);
        if (!notes.isEmpty()) {
            assignment.setNotes(notes);
        }

        if (isNew) {
            repository.add(assignment);
        } else {
            repository.update(assignment);
        }

        if (listener != null) {
            listener.onSaved(assignment, isNew);
        }

        Toast.makeText(getContext(), isNew ? "Assignment card created" : "Assignment updated", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
