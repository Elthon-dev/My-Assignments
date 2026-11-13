package com.nordic.myassignments.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;

import com.nordic.myassignments.R;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.model.SubTask;

public class AssignmentCardViewHolder {

    public interface OnCardActionListener {
        void onFocusClick(Assignment assignment);
        void onEditClick(Assignment assignment);
        void onDeleteClick(Assignment assignment);
        void onShareClick(Assignment assignment);
        void onToggleCompleted(Assignment assignment);
        void onSubTaskToggle(Assignment assignment, SubTask subTask);
        void onAddSubTask(Assignment assignment, String title);
        void onDeleteSubTask(Assignment assignment, SubTask subTask);
        void onNotesUpdated(Assignment assignment, String notes);
        void onCardDoubleTapped(Assignment assignment);
    }

    private final Context context;
    public final View rootView;

    public final CardView cardContainer;
    public final LinearLayout layoutCardFront;
    public final LinearLayout layoutCardBack;
    public final FrameLayout layoutCompletionOverlay;
    public final ImageView ivCompletionCheck;

    // Front views
    public final TextView tvSubjectTag;
    public final TextView tvPriorityBadge;
    public final TextView tvStatusBadge;
    public final ImageView btnCardOptions;
    public final TextView tvAssignmentTitle;
    public final TextView tvDueDateText;
    public final TextView tvEstimatedTime;
    public final TextView tvGradeWeight;
    public final TextView tvFocusTimeSpent;
    public final TextView tvTaskProgressRatio;
    public final ProgressBar pbTaskProgress;
    public final LinearLayout layoutNotesPreview;
    public final TextView tvNotesPreview;
    public final LinearLayout btnFlipToBack;
    public final LinearLayout btnZenFocusMode;

    // Back views
    public final LinearLayout btnFlipToFront;
    public final TextView tvBackSubjectTitle;
    public final ImageView btnCopyNotes;
    public final ImageView btnSaveNotes;
    public final EditText etAssignmentNotes;
    public final EditText etNewSubTask;
    public final ImageView btnAddSubTask;
    public final LinearLayout layoutSubTasksContainer;
    public final TextView btnQuickFocusFromNotes;

    private boolean isFlipped = false;
    private TextWatcher activeNotesWatcher;
    private Assignment boundAssignment;

    public AssignmentCardViewHolder(View itemView) {
        this.rootView = itemView;
        this.context = itemView.getContext();

        cardContainer = itemView.findViewById(R.id.cardContainer);
        layoutCardFront = itemView.findViewById(R.id.layoutCardFront);
        layoutCardBack = itemView.findViewById(R.id.layoutCardBack);
        layoutCompletionOverlay = itemView.findViewById(R.id.layoutCompletionOverlay);
        ivCompletionCheck = itemView.findViewById(R.id.ivCompletionCheck);

        // Front
        tvSubjectTag = itemView.findViewById(R.id.tvSubjectTag);
        tvPriorityBadge = itemView.findViewById(R.id.tvPriorityBadge);
        tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        btnCardOptions = itemView.findViewById(R.id.btnCardOptions);
        tvAssignmentTitle = itemView.findViewById(R.id.tvAssignmentTitle);
        tvDueDateText = itemView.findViewById(R.id.tvDueDateText);
        tvEstimatedTime = itemView.findViewById(R.id.tvEstimatedTime);
        tvGradeWeight = itemView.findViewById(R.id.tvGradeWeight);
        tvFocusTimeSpent = itemView.findViewById(R.id.tvFocusTimeSpent);
        tvTaskProgressRatio = itemView.findViewById(R.id.tvTaskProgressRatio);
        pbTaskProgress = itemView.findViewById(R.id.pbTaskProgress);
        layoutNotesPreview = itemView.findViewById(R.id.layoutNotesPreview);
        tvNotesPreview = itemView.findViewById(R.id.tvNotesPreview);
        btnFlipToBack = itemView.findViewById(R.id.btnFlipToBack);
        btnZenFocusMode = itemView.findViewById(R.id.btnZenFocusMode);

        // Back
        btnFlipToFront = itemView.findViewById(R.id.btnFlipToFront);
        tvBackSubjectTitle = itemView.findViewById(R.id.tvBackSubjectTitle);
        btnCopyNotes = itemView.findViewById(R.id.btnCopyNotes);
        btnSaveNotes = itemView.findViewById(R.id.btnSaveNotes);
        etAssignmentNotes = itemView.findViewById(R.id.etAssignmentNotes);
        etNewSubTask = itemView.findViewById(R.id.etNewSubTask);
        btnAddSubTask = itemView.findViewById(R.id.btnAddSubTask);
        layoutSubTasksContainer = itemView.findViewById(R.id.layoutSubTasksContainer);
        btnQuickFocusFromNotes = itemView.findViewById(R.id.btnQuickFocusFromNotes);

        // Setup 3D camera distance
        float distance = 10000 * context.getResources().getDisplayMetrics().density;
        layoutCardFront.setCameraDistance(distance);
        layoutCardBack.setCameraDistance(distance);
    }

    public void bind(final Assignment assignment, final boolean isInteractiveTop, final OnCardActionListener listener) {
        this.boundAssignment = assignment;
        if (assignment == null) {
            rootView.setVisibility(View.GONE);
            return;
        }
        rootView.setVisibility(View.VISIBLE);

        // Ensure completion overlay is hidden initially
        if (layoutCompletionOverlay != null) {
            layoutCompletionOverlay.setVisibility(View.GONE);
            layoutCompletionOverlay.setAlpha(0f);
        }

        // Keep front visible unless already flipped
        if (isFlipped && isInteractiveTop) {
            layoutCardFront.setVisibility(View.GONE);
            layoutCardBack.setVisibility(View.VISIBLE);
            layoutCardBack.setRotationY(0f);
        } else {
            isFlipped = false;
            layoutCardFront.setVisibility(View.VISIBLE);
            layoutCardBack.setVisibility(View.GONE);
            layoutCardFront.setRotationY(0f);
        }

        // Subject pill styling
        tvSubjectTag.setText(assignment.getSubject());
        tvSubjectTag.setBackgroundResource(R.drawable.bg_chip_pill);
        tvSubjectTag.setTextColor(Color.WHITE);

        // Priority Badge styling (Matte Monochrome with subtle tonal difference)
        String priority = assignment.getPriority() != null ? assignment.getPriority().toUpperCase() : "MEDIUM";
        tvPriorityBadge.setText(priority);
        tvPriorityBadge.setBackgroundResource(R.drawable.bg_chip_pill);
        if ("HIGH".equals(priority)) {
            tvPriorityBadge.setTextColor(Color.parseColor("#FFFFFF"));
            tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E2020")));
        } else if ("LOW".equals(priority)) {
            tvPriorityBadge.setTextColor(Color.parseColor("#A1A1AA"));
            tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1C1C1E")));
        } else {
            tvPriorityBadge.setTextColor(Color.parseColor("#E5E5EA"));
            tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#242426")));
        }

        // Status Badge styling
        if (assignment.isCompleted()) {
            tvStatusBadge.setText("✓ COMPLETED");
            tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1C2A1E")));
            tvAssignmentTitle.setPaintFlags(tvAssignmentTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            boolean inProgress = "IN_PROGRESS".equalsIgnoreCase(assignment.getStatus());
            tvStatusBadge.setText(inProgress ? "⚡ IN PROGRESS" : "NOT STARTED");
            tvStatusBadge.setTextColor(Color.parseColor("#E5E5EA"));
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F1F1F")));
            tvAssignmentTitle.setPaintFlags(tvAssignmentTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        tvStatusBadge.setOnClickListener(v -> {
            if (listener != null) listener.onToggleCompleted(assignment);
        });

        // Title & Due date
        tvAssignmentTitle.setText(assignment.getTitle());
        tvDueDateText.setText(assignment.getRelativeDueString());
        if (assignment.isDueSoon() && !assignment.isCompleted()) {
            tvDueDateText.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            tvDueDateText.setTextColor(Color.parseColor("#8E8E93"));
        }

        tvEstimatedTime.setText(assignment.getEstimatedHours() + " hrs");
        tvGradeWeight.setText(assignment.getGradeWeight() + "% Grade");

        // Focus Minutes
        if (assignment.getFocusMinutesSpent() > 0) {
            tvFocusTimeSpent.setText(assignment.getFocusMinutesSpent() + " mins logged in Focus");
        } else {
            tvFocusTimeSpent.setText("Ready for focus sprint");
        }

        // Checklist Progress
        int totalTasks = assignment.getTotalTasksCount();
        int completedTasks = assignment.getCompletedTasksCount();
        int progress = assignment.getProgressPercentage();
        pbTaskProgress.setProgress(progress);
        tvTaskProgressRatio.setText(completedTasks + " / " + totalTasks + " (" + progress + "%)");

        // Notes Preview
        if (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty()) {
            tvNotesPreview.setText(assignment.getNotes().trim());
        } else {
            tvNotesPreview.setText("Tap to flip card & jot down notes, milestones, or key pointers...");
        }

        if (!isInteractiveTop) {
            // Background cards in stack don't need active touch listeners
            btnCardOptions.setOnClickListener(null);
            btnFlipToBack.setOnClickListener(null);
            layoutNotesPreview.setOnClickListener(null);
            btnZenFocusMode.setOnClickListener(null);
            return;
        }

        // Overflow Options Menu
        btnCardOptions.setOnClickListener(v -> showPopupMenu(v, assignment, listener));

        // Flip to Notes Back Pad
        btnFlipToBack.setOnClickListener(v -> flipCard(true, assignment));
        layoutNotesPreview.setOnClickListener(v -> flipCard(true, assignment));

        // Flip back to Front Overview
        btnFlipToFront.setOnClickListener(v -> flipCard(false, assignment));

        // Zen Focus Mode Buttons
        btnZenFocusMode.setOnClickListener(v -> {
            if (listener != null) listener.onFocusClick(assignment);
        });
        btnQuickFocusFromNotes.setOnClickListener(v -> {
            if (listener != null) listener.onFocusClick(assignment);
        });

        // ================= BACK NOTE PAD SETUP =================
        tvBackSubjectTitle.setText(assignment.getSubject() + " • " + assignment.getTitle());

        // Live notes editing
        if (activeNotesWatcher != null) {
            etAssignmentNotes.removeTextChangedListener(activeNotesWatcher);
        }
        etAssignmentNotes.setText(assignment.getNotes());
        activeNotesWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                assignment.setNotes(s.toString());
                if (listener != null) {
                    listener.onNotesUpdated(assignment, s.toString());
                }
            }
        };
        etAssignmentNotes.addTextChangedListener(activeNotesWatcher);

        // CHECK / SAVE BUTTON: User requirement:
        // "When the user presses the check/save button:
        // - Save the updated assignment information.
        // - Animate the card flipping back to its original display interface.
        // - Completely remove the editing interface from view.
        // - Display the updated assignment information on the original card.
        // - Ensure the card does not remain stuck in editing mode.
        // Saving changes must never accidentally mark an assignment as completed."
        btnSaveNotes.setOnClickListener(v -> {
            hideKeyboard(etAssignmentNotes);
            String notes = etAssignmentNotes.getText().toString();
            assignment.setNotes(notes);
            if (listener != null) {
                listener.onNotesUpdated(assignment, notes);
            }
            vibrate(30);
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            // Flip back to front original interface!
            flipCard(false, assignment);
        });

        // Copy / Share Notes Button
        btnCopyNotes.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(assignment);
        });

        // Add subtask
        btnAddSubTask.setOnClickListener(v -> {
            String title = etNewSubTask.getText().toString().trim();
            if (!title.isEmpty()) {
                etNewSubTask.setText("");
                hideKeyboard(etNewSubTask);
                if (listener != null) {
                    listener.onAddSubTask(assignment, title);
                }
            }
        });

        populateSubTasks(assignment, listener);
    }

    private void populateSubTasks(final Assignment assignment, final OnCardActionListener listener) {
        layoutSubTasksContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        for (final SubTask subTask : assignment.getChecklist()) {
            View item = inflater.inflate(R.layout.item_subtask_checkable, layoutSubTasksContainer, false);
            CheckBox cb = item.findViewById(R.id.cbSubTask);
            final TextView tvTitle = item.findViewById(R.id.tvSubTaskTitle);
            ImageView btnDelete = item.findViewById(R.id.btnDeleteSubTask);

            cb.setChecked(subTask.isCompleted());
            tvTitle.setText(subTask.getTitle());
            if (subTask.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(Color.parseColor("#636366"));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(Color.parseColor("#E5E5EA"));
            }

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSubTaskToggle(assignment, subTask);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteSubTask(assignment, subTask);
                }
            });

            layoutSubTasksContainer.addView(item);
        }
    }

    public void flipCard(final boolean toBack, final Assignment assignment) {
        final View front = layoutCardFront;
        final View back = layoutCardBack;

        if (toBack) {
            isFlipped = true;
            front.animate()
                    .rotationY(90f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            front.setVisibility(View.GONE);
                            back.setVisibility(View.VISIBLE);
                            back.setRotationY(-90f);
                            back.animate()
                                    .rotationY(0f)
                                    .setDuration(200)
                                    .setListener(null)
                                    .start();
                        }
                    }).start();
        } else {
            isFlipped = false;
            back.animate()
                    .rotationY(-90f)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            back.setVisibility(View.GONE);
                            front.setVisibility(View.VISIBLE);
                            front.setRotationY(90f);
                            front.animate()
                                    .rotationY(0f)
                                    .setDuration(200)
                                    .setListener(null)
                                    .start();
                            // Update notes preview on front card
                            if (assignment != null && tvNotesPreview != null) {
                                String n = assignment.getNotes();
                                if (n != null && !n.trim().isEmpty()) {
                                    tvNotesPreview.setText(n.trim());
                                }
                            }
                        }
                    }).start();
        }
    }

    public boolean isFlipped() {
        return isFlipped;
    }

    public void showCompletionAnimation(final Runnable onFinished) {
        if (layoutCompletionOverlay == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        vibrate(50);
        layoutCompletionOverlay.setVisibility(View.VISIBLE);
        layoutCompletionOverlay.setAlpha(0f);
        ivCompletionCheck.setScaleX(0.4f);
        ivCompletionCheck.setScaleY(0.4f);

        layoutCompletionOverlay.animate()
                .alpha(1f)
                .setDuration(180)
                .start();

        ivCompletionCheck.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(220)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ivCompletionCheck.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (onFinished != null) {
                                            onFinished.run();
                                        }
                                    }
                                }).start();
                    }
                }).start();
    }

    private void showPopupMenu(View view, final Assignment assignment, final OnCardActionListener listener) {
        PopupMenu popup = new PopupMenu(context, view);
        if (assignment.isCompleted()) {
            popup.getMenu().add(0, 1, 0, "Restore to In Progress");
        } else {
            popup.getMenu().add(0, 1, 0, "Mark as Completed");
        }
        popup.getMenu().add(0, 2, 1, "Edit Details");
        popup.getMenu().add(0, 3, 2, "Share / Copy Summary");
        popup.getMenu().add(0, 4, 3, "Delete Card");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    if (listener != null) listener.onToggleCompleted(assignment);
                    return true;
                } else if (item.getItemId() == 2) {
                    if (listener != null) listener.onEditClick(assignment);
                    return true;
                } else if (item.getItemId() == 3) {
                    if (listener != null) listener.onShareClick(assignment);
                    return true;
                } else if (item.getItemId() == 4) {
                    if (listener != null) listener.onDeleteClick(assignment);
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void vibrate(long ms) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(ms);
                }
            }
        } catch (Exception ignored) {}
    }
}
