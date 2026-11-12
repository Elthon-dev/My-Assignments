package com.nordic.myassignments.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nordic.myassignments.R;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.model.SubTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignmentFlipcardAdapter extends RecyclerView.Adapter<AssignmentFlipcardAdapter.CardViewHolder> {

    public interface OnAssignmentActionListener {
        void onFocusClick(Assignment assignment);
        void onEditClick(Assignment assignment);
        void onDeleteClick(Assignment assignment);
        void onShareClick(Assignment assignment);
        void onToggleCompleted(Assignment assignment);
        void onSubTaskToggle(Assignment assignment, SubTask subTask);
        void onAddSubTask(Assignment assignment, String title);
        void onDeleteSubTask(Assignment assignment, SubTask subTask);
        void onNotesUpdated(Assignment assignment, String notes);
    }

    private final Context context;
    private List<Assignment> assignments = new ArrayList<>();
    private final OnAssignmentActionListener listener;
    private final Set<String> flippedCardIds = new HashSet<>();

    public AssignmentFlipcardAdapter(Context context, OnAssignmentActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setAssignments(List<Assignment> list) {
        this.assignments = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public Assignment getItem(int position) {
        if (position >= 0 && position < assignments.size()) {
            return assignments.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Assignment assignment = assignments.get(position);
        holder.bind(assignment);
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        final CardView cardContainer;
        final LinearLayout layoutCardFront;
        final LinearLayout layoutCardBack;

        // Front views
        final TextView tvSubjectTag;
        final TextView tvPriorityBadge;
        final TextView tvStatusBadge;
        final ImageView btnCardOptions;
        final TextView tvAssignmentTitle;
        final TextView tvDueDateText;
        final TextView tvEstimatedTime;
        final TextView tvGradeWeight;
        final TextView tvFocusTimeSpent;
        final TextView tvTaskProgressRatio;
        final ProgressBar pbTaskProgress;
        final LinearLayout layoutNotesPreview;
        final TextView tvNotesPreview;
        final LinearLayout btnFlipToBack;
        final LinearLayout btnZenFocusMode;

        // Back views
        final LinearLayout btnFlipToFront;
        final TextView tvBackSubjectTitle;
        final ImageView btnCopyNotes;
        final ImageView btnSaveNotes;
        final EditText etAssignmentNotes;
        final EditText etNewSubTask;
        final ImageView btnAddSubTask;
        final LinearLayout layoutSubTasksContainer;
        final TextView btnQuickFocusFromNotes;

        private TextWatcher activeNotesWatcher;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            layoutCardFront = itemView.findViewById(R.id.layoutCardFront);
            layoutCardBack = itemView.findViewById(R.id.layoutCardBack);

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

            // Setup camera perspective for 3D card flips
            float distance = 9000 * context.getResources().getDisplayMetrics().density;
            layoutCardFront.setCameraDistance(distance);
            layoutCardBack.setCameraDistance(distance);
        }

        void bind(final Assignment assignment) {
            boolean isFlipped = flippedCardIds.contains(assignment.getId());
            if (isFlipped) {
                layoutCardFront.setVisibility(View.GONE);
                layoutCardBack.setVisibility(View.VISIBLE);
                layoutCardBack.setRotationY(0f);
            } else {
                layoutCardFront.setVisibility(View.VISIBLE);
                layoutCardBack.setVisibility(View.GONE);
                layoutCardFront.setRotationY(0f);
            }

            // Theme tint
            int accentColor;
            try {
                accentColor = Color.parseColor(assignment.getColorHex());
            } catch (Exception e) {
                accentColor = Color.parseColor("#52796F");
            }

            tvSubjectTag.setText(assignment.getSubject());
            tvSubjectTag.setBackgroundTintList(ColorStateList.valueOf(accentColor));

            // Priority
            String priority = assignment.getPriority().toUpperCase();
            tvPriorityBadge.setText(priority);
            if ("HIGH".equals(priority)) {
                tvPriorityBadge.setTextColor(Color.parseColor("#C86452")); // Terracotta
                tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FDF0ED")));
            } else if ("LOW".equals(priority)) {
                tvPriorityBadge.setTextColor(Color.parseColor("#52796F")); // Sage
                tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDF3F1")));
            } else {
                tvPriorityBadge.setTextColor(Color.parseColor("#D49B5B")); // Amber
                tvPriorityBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FDF6EC")));
            }

            // Status Badge
            if (assignment.isCompleted()) {
                tvStatusBadge.setText("✓ COMPLETED");
                tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                tvAssignmentTitle.setPaintFlags(tvAssignmentTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvStatusBadge.setText("IN_PROGRESS".equalsIgnoreCase(assignment.getStatus()) ? "⚡ IN PROGRESS" : "NOT STARTED");
                tvStatusBadge.setTextColor(Color.parseColor("#2F3E46"));
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDF2F0")));
                tvAssignmentTitle.setPaintFlags(tvAssignmentTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            tvStatusBadge.setOnClickListener(v -> {
                if (listener != null) listener.onToggleCompleted(assignment);
            });

            // Title & Due date
            tvAssignmentTitle.setText(assignment.getTitle());
            tvDueDateText.setText(assignment.getRelativeDueString());
            if (assignment.isDueSoon() && !assignment.isCompleted()) {
                tvDueDateText.setTextColor(Color.parseColor("#C86452"));
            } else {
                tvDueDateText.setTextColor(Color.parseColor("#2F3E46"));
            }

            tvEstimatedTime.setText(assignment.getEstimatedHours() + " hrs");
            tvGradeWeight.setText(assignment.getGradeWeight() + "% of Grade");

            // Focus Minutes
            if (assignment.getFocusMinutesSpent() > 0) {
                tvFocusTimeSpent.setText(assignment.getFocusMinutesSpent() + " mins logged in Focus Mode");
            } else {
                tvFocusTimeSpent.setText("Ready for first focus session");
            }

            // Progress Bar & Checklist Stats
            int totalTasks = assignment.getTotalTasksCount();
            int completedTasks = assignment.getCompletedTasksCount();
            int progress = assignment.getProgressPercentage();
            pbTaskProgress.setProgress(progress);
            pbTaskProgress.setProgressTintList(ColorStateList.valueOf(accentColor));
            tvTaskProgressRatio.setText(completedTasks + " / " + totalTasks + " (" + progress + "%)");

            // Notes Preview
            if (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty()) {
                tvNotesPreview.setText(assignment.getNotes().trim());
            } else {
                tvNotesPreview.setText("Tap to flip & jot down lecture notes, formula sheets, or key references...");
            }

            // Overflow Options Menu
            btnCardOptions.setOnClickListener(v -> showPopupMenu(v, assignment));

            // Card Flip Interactions
            btnFlipToBack.setOnClickListener(v -> flipCard(true, assignment));
            layoutNotesPreview.setOnClickListener(v -> flipCard(true, assignment));
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

            // Handle live notes editing with text watcher
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

            // Save Notes Button
            btnSaveNotes.setOnClickListener(v -> {
                String notes = etAssignmentNotes.getText().toString();
                assignment.setNotes(notes);
                if (listener != null) {
                    listener.onNotesUpdated(assignment, notes);
                }
                Toast.makeText(context, "Notes saved to " + assignment.getSubject(), Toast.LENGTH_SHORT).show();
            });

            // Copy / Share Notes Button
            btnCopyNotes.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(assignment);
            });

            // Add new checklist sub-task
            btnAddSubTask.setOnClickListener(v -> {
                String title = etNewSubTask.getText().toString().trim();
                if (!title.isEmpty()) {
                    etNewSubTask.setText("");
                    if (listener != null) {
                        listener.onAddSubTask(assignment, title);
                    }
                }
            });

            // Populate Subtasks dynamically in Back View
            populateSubTasks(assignment);
        }

        private void populateSubTasks(final Assignment assignment) {
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
                    tvTitle.setTextColor(Color.parseColor("#94A3B8"));
                } else {
                    tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    tvTitle.setTextColor(Color.parseColor("#2F3E46"));
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

        private void flipCard(final boolean toBack, final Assignment assignment) {
            final View front = layoutCardFront;
            final View back = layoutCardBack;

            if (toBack) {
                flippedCardIds.add(assignment.getId());
                front.animate()
                        .rotationY(90f)
                        .setDuration(220)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                front.setVisibility(View.GONE);
                                back.setVisibility(View.VISIBLE);
                                back.setRotationY(-90f);
                                back.animate()
                                        .rotationY(0f)
                                        .setDuration(220)
                                        .setListener(null)
                                        .start();
                            }
                        }).start();
            } else {
                flippedCardIds.remove(assignment.getId());
                back.animate()
                        .rotationY(-90f)
                        .setDuration(220)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                back.setVisibility(View.GONE);
                                front.setVisibility(View.VISIBLE);
                                front.setRotationY(90f);
                                front.animate()
                                        .rotationY(0f)
                                        .setDuration(220)
                                        .setListener(null)
                                        .start();
                            }
                        }).start();
            }
        }

        private void showPopupMenu(View view, final Assignment assignment) {
            PopupMenu popup = new PopupMenu(context, view);
            popup.getMenu().add(0, 1, 0, assignment.isCompleted() ? "Mark as In Progress" : "Mark as Completed");
            popup.getMenu().add(0, 2, 1, "Edit Assignment");
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
    }
}
