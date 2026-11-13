package com.nordic.myassignments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.model.SubTask;
import com.nordic.myassignments.storage.AssignmentRepository;
import com.nordic.myassignments.ui.AddEditAssignmentDialog;
import com.nordic.myassignments.ui.AssignmentCardViewHolder;
import com.nordic.myassignments.ui.ZenFocusDialog;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AssignmentCardViewHolder.OnCardActionListener {

    private enum FilterMode {
        ALL, DUE_SOON, ACTIVE, HIGH_PRIORITY, COMPLETED
    }

    private AssignmentRepository repository;
    private final List<Assignment> displayedAssignments = new ArrayList<>();
    private FilterMode currentFilter = FilterMode.ALL;

    // Undo support
    private Assignment lastCompletedAssignment = null;

    // Layout views
    private FrameLayout layoutCardStackContainer;
    private FrameLayout containerCardTop;
    private FrameLayout containerCardMiddle;
    private FrameLayout containerCardBottom;

    private AssignmentCardViewHolder holderTop;
    private AssignmentCardViewHolder holderMiddle;
    private AssignmentCardViewHolder holderBottom;

    private LinearLayout layoutEmptyDeck;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private TextView btnEmptyCreateCard;
    private LinearLayout btnAddNewAssignment;
    private LinearLayout layoutBottomDeckControls;
    private TextView tvCardDeckCounter;
    private ImageView btnPrevCard;
    private ImageView btnNextCard;

    private TextView chipFilterAll;
    private TextView chipFilterDueSoon;
    private TextView chipFilterActive;
    private TextView chipFilterPriority;
    private TextView chipFilterCompleted;

    // Gesture & Stack physics
    private int screenWidth;
    private int screenHeight;
    private float density;
    private int touchSlop;
    private GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    private float downX;
    private float downY;
    private boolean isDraggingCard = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = AssignmentRepository.getInstance(this);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        density = dm.density;
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        initViews();
        setupGestureDetection();
        setupFilters();
        setupActions();

        loadAssignments();
    }

    private void initViews() {
        layoutCardStackContainer = findViewById(R.id.layoutCardStackContainer);
        containerCardTop = findViewById(R.id.containerCardTop);
        containerCardMiddle = findViewById(R.id.containerCardMiddle);
        containerCardBottom = findViewById(R.id.containerCardBottom);

        layoutEmptyDeck = findViewById(R.id.layoutEmptyDeck);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnEmptyCreateCard = findViewById(R.id.btnEmptyCreateCard);

        btnAddNewAssignment = findViewById(R.id.btnAddNewAssignment);
        layoutBottomDeckControls = findViewById(R.id.layoutBottomDeckControls);
        tvCardDeckCounter = findViewById(R.id.tvCardDeckCounter);
        btnPrevCard = findViewById(R.id.btnPrevCard);
        btnNextCard = findViewById(R.id.btnNextCard);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterDueSoon = findViewById(R.id.chipFilterDueSoon);
        chipFilterActive = findViewById(R.id.chipFilterActive);
        chipFilterPriority = findViewById(R.id.chipFilterPriority);
        chipFilterCompleted = findViewById(R.id.chipFilterCompleted);

        // Inflate the 3 card layer views
        LayoutInflater inflater = LayoutInflater.from(this);
        View viewBottom = inflater.inflate(R.layout.item_assignment_card, containerCardBottom, false);
        containerCardBottom.addView(viewBottom);
        holderBottom = new AssignmentCardViewHolder(viewBottom);

        View viewMiddle = inflater.inflate(R.layout.item_assignment_card, containerCardMiddle, false);
        containerCardMiddle.addView(viewMiddle);
        holderMiddle = new AssignmentCardViewHolder(viewMiddle);

        View viewTop = inflater.inflate(R.layout.item_assignment_card, containerCardTop, false);
        containerCardTop.addView(viewTop);
        holderTop = new AssignmentCardViewHolder(viewTop);
    }

    private void setupGestureDetection() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (displayedAssignments.isEmpty()) return false;
                // Double tap marks as completed & auto-removes from active stack
                triggerCompleteCurrentCard();
                return true;
            }
        });

        // Top card touch listener: handles drag left/right to cycle stack
        containerCardTop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (displayedAssignments.isEmpty()) return false;

                // Let gesture detector inspect for double-tap
                gestureDetector.onTouchEvent(event);

                // If user flipped card to back, allow them to interact with notes & checklist freely
                if (holderTop.isFlipped()) {
                    return false;
                }

                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                velocityTracker.addMovement(event);

                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        isDraggingCard = false;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;

                        if (!isDraggingCard) {
                            if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                                isDraggingCard = true;
                                containerCardTop.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                        }

                        if (isDraggingCard) {
                            containerCardTop.setTranslationX(dx);
                            containerCardTop.setRotation(dx * 0.032f);

                            // Progressively scale middle and bottom cards
                            float progress = Math.min(Math.abs(dx) / (screenWidth * 0.45f), 1f);

                            float scaleMid = 0.94f + progress * 0.06f;
                            float transYMid = (14f - progress * 14f) * density;
                            float alphaMid = 0.75f + progress * 0.25f;
                            containerCardMiddle.setScaleX(scaleMid);
                            containerCardMiddle.setScaleY(scaleMid);
                            containerCardMiddle.setTranslationY(transYMid);
                            containerCardMiddle.setAlpha(alphaMid);

                            float scaleBot = 0.88f + progress * 0.06f;
                            float transYBot = (28f - progress * 14f) * density;
                            float alphaBot = 0.45f + progress * 0.30f;
                            containerCardBottom.setScaleX(scaleBot);
                            containerCardBottom.setScaleY(scaleBot);
                            containerCardBottom.setTranslationY(transYBot);
                            containerCardBottom.setAlpha(alphaBot);

                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isDraggingCard) {
                            isDraggingCard = false;
                            velocityTracker.computeCurrentVelocity(1000);
                            float velocityX = velocityTracker.getXVelocity();
                            float finalDx = event.getRawX() - downX;

                            boolean isFling = Math.abs(velocityX) > 900;
                            boolean isPastThreshold = Math.abs(finalDx) > screenWidth * 0.28f;

                            if ((isPastThreshold || isFling) && displayedAssignments.size() > 1) {
                                // User swiped left OR right: Fly top card away and cycle to bottom of stack!
                                float exitTargetX = (finalDx >= 0 ? 1 : -1) * (screenWidth + 250);
                                animateCardSwipeCycle(exitTargetX);
                            } else {
                                // Settle back to center
                                containerCardTop.animate()
                                        .translationX(0f)
                                        .rotation(0f)
                                        .setDuration(180)
                                        .setInterpolator(new DecelerateInterpolator())
                                        .start();

                                containerCardMiddle.animate()
                                        .scaleX(0.94f).scaleY(0.94f)
                                        .translationY(14 * density).alpha(0.75f)
                                        .setDuration(180)
                                        .start();

                                containerCardBottom.animate()
                                        .scaleX(0.88f).scaleY(0.88f)
                                        .translationY(28 * density).alpha(0.45f)
                                        .setDuration(180)
                                        .start();
                            }

                            if (velocityTracker != null) {
                                velocityTracker.recycle();
                                velocityTracker = null;
                            }
                            return true;
                        }

                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void animateCardSwipeCycle(float exitX) {
        containerCardTop.animate()
                .translationX(exitX)
                .rotation(exitX * 0.032f)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (displayedAssignments.size() > 1) {
                        Assignment swiped = displayedAssignments.remove(0);
                        displayedAssignments.add(swiped); // Put to bottom of the stack!
                    }
                    vibrate(20);
                    resetCardTransforms();
                    renderStack();
                }).start();

        containerCardMiddle.animate()
                .scaleX(1.0f).scaleY(1.0f)
                .translationY(0f).alpha(1.0f)
                .setDuration(220)
                .start();

        containerCardBottom.animate()
                .scaleX(0.94f).scaleY(0.94f)
                .translationY(14 * density).alpha(0.75f)
                .setDuration(220)
                .start();
    }

    private void resetCardTransforms() {
        containerCardTop.setTranslationX(0f);
        containerCardTop.setTranslationY(0f);
        containerCardTop.setRotation(0f);
        containerCardTop.setScaleX(1.0f);
        containerCardTop.setScaleY(1.0f);
        containerCardTop.setAlpha(1.0f);

        containerCardMiddle.setScaleX(0.94f);
        containerCardMiddle.setScaleY(0.94f);
        containerCardMiddle.setTranslationY(14 * density);
        containerCardMiddle.setTranslationX(0f);
        containerCardMiddle.setRotation(0f);
        containerCardMiddle.setAlpha(0.75f);

        containerCardBottom.setScaleX(0.88f);
        containerCardBottom.setScaleY(0.88f);
        containerCardBottom.setTranslationY(28 * density);
        containerCardBottom.setTranslationX(0f);
        containerCardBottom.setRotation(0f);
        containerCardBottom.setAlpha(0.45f);
    }

    private void setupFilters() {
        chipFilterAll.setOnClickListener(v -> applyFilter(FilterMode.ALL));
        chipFilterDueSoon.setOnClickListener(v -> applyFilter(FilterMode.DUE_SOON));
        chipFilterActive.setOnClickListener(v -> applyFilter(FilterMode.ACTIVE));
        chipFilterPriority.setOnClickListener(v -> applyFilter(FilterMode.HIGH_PRIORITY));
        chipFilterCompleted.setOnClickListener(v -> applyFilter(FilterMode.COMPLETED));
    }

    private void applyFilter(FilterMode filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        loadAssignments();
    }

    private void updateFilterChipStyles() {
        setChipActive(chipFilterAll, currentFilter == FilterMode.ALL);
        setChipActive(chipFilterDueSoon, currentFilter == FilterMode.DUE_SOON);
        setChipActive(chipFilterActive, currentFilter == FilterMode.ACTIVE);
        setChipActive(chipFilterPriority, currentFilter == FilterMode.HIGH_PRIORITY);
        setChipActive(chipFilterCompleted, currentFilter == FilterMode.COMPLETED);
    }

    private void setChipActive(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_chip_filter_selected);
            chip.setTextColor(Color.BLACK);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_filter_unselected);
            chip.setTextColor(Color.parseColor("#8E8E93"));
        }
    }

    private void setupActions() {
        btnAddNewAssignment.setOnClickListener(v -> openAddEditDialog(null));
        btnEmptyCreateCard.setOnClickListener(v -> openAddEditDialog(null));

        // Programmatic Next (Cycle top to bottom)
        btnNextCard.setOnClickListener(v -> {
            if (displayedAssignments.size() > 1) {
                animateCardSwipeCycle(screenWidth + 250);
            }
        });

        // Programmatic Prev (Pull bottom card to top with entrance)
        btnPrevCard.setOnClickListener(v -> {
            if (displayedAssignments.size() > 1) {
                Assignment bottom = displayedAssignments.remove(displayedAssignments.size() - 1);
                displayedAssignments.add(0, bottom); // Pull to top!
                vibrate(20);

                // Slide in from left
                containerCardTop.setTranslationX(-(screenWidth + 200));
                containerCardTop.setRotation(-12f);
                containerCardTop.setAlpha(0f);
                renderStack();

                containerCardTop.animate()
                        .translationX(0f)
                        .rotation(0f)
                        .alpha(1f)
                        .setDuration(220)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });
    }

    private void loadAssignments() {
        List<Assignment> all = repository.getAll();
        displayedAssignments.clear();

        for (Assignment a : all) {
            switch (currentFilter) {
                case ALL:
                    // Active cards are shown in the primary stack; completed cards are stored in COMPLETED filter
                    if (!a.isCompleted()) displayedAssignments.add(a);
                    break;
                case DUE_SOON:
                    if (!a.isCompleted() && a.isDueSoon()) displayedAssignments.add(a);
                    break;
                case ACTIVE:
                    if (!a.isCompleted()) displayedAssignments.add(a);
                    break;
                case HIGH_PRIORITY:
                    if (!a.isCompleted() && "HIGH".equalsIgnoreCase(a.getPriority())) displayedAssignments.add(a);
                    break;
                case COMPLETED:
                    if (a.isCompleted()) displayedAssignments.add(a);
                    break;
            }
        }

        renderStack();
    }

    private void renderStack() {
        resetCardTransforms();
        int total = displayedAssignments.size();

        if (total == 0) {
            layoutCardStackContainer.setVisibility(View.GONE);
            layoutEmptyDeck.setVisibility(View.VISIBLE);
            layoutBottomDeckControls.setVisibility(View.GONE);

            if (currentFilter == FilterMode.COMPLETED) {
                tvEmptyTitle.setText("No Completed Assignments");
                tvEmptySubtitle.setText("Double-tap any active card in your stack to mark it completed.");
                btnEmptyCreateCard.setVisibility(View.GONE);
            } else {
                tvEmptyTitle.setText("Your Stack is Empty");
                tvEmptySubtitle.setText("No assignment cards found. Start with a clean slate and create your first matte card.");
                btnEmptyCreateCard.setVisibility(View.VISIBLE);
            }
            updateCounter();
            return;
        }

        layoutEmptyDeck.setVisibility(View.GONE);
        layoutCardStackContainer.setVisibility(View.VISIBLE);
        layoutBottomDeckControls.setVisibility(View.VISIBLE);

        if (total == 1) {
            containerCardBottom.setVisibility(View.GONE);
            containerCardMiddle.setVisibility(View.GONE);
            containerCardTop.setVisibility(View.VISIBLE);
            holderTop.bind(displayedAssignments.get(0), true, this);
        } else if (total == 2) {
            containerCardBottom.setVisibility(View.GONE);
            containerCardMiddle.setVisibility(View.VISIBLE);
            containerCardTop.setVisibility(View.VISIBLE);
            holderTop.bind(displayedAssignments.get(0), true, this);
            holderMiddle.bind(displayedAssignments.get(1), false, null);
        } else {
            containerCardBottom.setVisibility(View.VISIBLE);
            containerCardMiddle.setVisibility(View.VISIBLE);
            containerCardTop.setVisibility(View.VISIBLE);
            holderTop.bind(displayedAssignments.get(0), true, this);
            holderMiddle.bind(displayedAssignments.get(1), false, null);
            holderBottom.bind(displayedAssignments.get(2), false, null);
        }

        updateCounter();
    }

    private void updateCounter() {
        int total = displayedAssignments.size();
        if (total == 0) {
            tvCardDeckCounter.setText("0 Cards");
            btnPrevCard.setEnabled(false);
            btnNextCard.setEnabled(false);
            btnPrevCard.setAlpha(0.25f);
            btnNextCard.setAlpha(0.25f);
            return;
        }

        tvCardDeckCounter.setText("Card 1 of " + total);
        boolean hasMultiple = total > 1;
        btnPrevCard.setEnabled(hasMultiple);
        btnNextCard.setEnabled(hasMultiple);
        btnPrevCard.setAlpha(hasMultiple ? 1.0f : 0.35f);
        btnNextCard.setAlpha(hasMultiple ? 1.0f : 0.35f);
    }

    // ================= DOUBLE-TAP COMPLETION & AUTO-REMOVAL =================

    private void triggerCompleteCurrentCard() {
        if (displayedAssignments.isEmpty()) return;
        final Assignment target = displayedAssignments.get(0);

        // 1. Play checkmark burst animation overlay
        holderTop.showCompletionAnimation(() -> {
            // 2. Animate card floating upward and fading away
            containerCardTop.animate()
                    .translationY(-screenHeight * 0.45f)
                    .alpha(0f)
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(240)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        // 3. Mark completed in persistent storage
                        repository.markCompleted(target.getId());
                        lastCompletedAssignment = target;

                        // 4. Auto-remove from current active stack!
                        if (!displayedAssignments.isEmpty()) {
                            displayedAssignments.remove(0);
                        }

                        // 5. Re-render stack smoothly
                        renderStack();

                        // 6. Provide subtle undo snackbar
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                "✓ Marked '" + target.getTitle() + "' completed", Snackbar.LENGTH_LONG);
                        snackbar.setAction("UNDO", v -> {
                            target.setStatus("IN_PROGRESS");
                            repository.update(target);
                            displayedAssignments.add(0, target);
                            renderStack();
                            vibrate(25);
                        });
                        snackbar.setActionTextColor(Color.WHITE);
                        try {
                            snackbar.getView().setBackgroundColor(Color.parseColor("#1C1C1E"));
                        } catch (Exception ignored) {}
                        snackbar.show();
                    }).start();

            // Simultaneously animate middle card smoothly up to top position
            containerCardMiddle.animate()
                    .scaleX(1.0f).scaleY(1.0f)
                    .translationY(0f).alpha(1.0f)
                    .setDuration(240)
                    .start();
        });
    }

    private void openAddEditDialog(Assignment assignment) {
        AddEditAssignmentDialog dialog = new AddEditAssignmentDialog(this, assignment, (saved, isNew) -> {
            if (isNew) {
                // New card is placed right on top of the stack!
                displayedAssignments.add(0, saved);
                renderStack();
            } else {
                loadAssignments();
            }
        });
        dialog.show();
    }

    // ================= CARD ACTION LISTENER CALLBACKS =================

    @Override
    public void onFocusClick(Assignment assignment) {
        ZenFocusDialog focusDialog = new ZenFocusDialog(this, assignment, (updatedAssignment, minutesLogged) -> {
            loadAssignments();
        });
        focusDialog.show();
    }

    @Override
    public void onEditClick(Assignment assignment) {
        openAddEditDialog(assignment);
    }

    @Override
    public void onDeleteClick(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Assignment Card")
                .setMessage("Remove '" + assignment.getTitle() + "' from your stack?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.delete(assignment.getId());
                    loadAssignments();
                    vibrate(30);
                    Toast.makeText(this, "Card deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onShareClick(Assignment assignment) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 ").append(assignment.getTitle()).append(" (").append(assignment.getSubject()).append(")\n");
        sb.append("• Deadline: ").append(assignment.getRelativeDueString()).append(" (").append(assignment.getFormattedDueDate()).append(")\n");
        sb.append("• Priority: ").append(assignment.getPriority()).append(" • Status: ").append(assignment.getStatus()).append("\n");
        sb.append("• Progress: ").append(assignment.getCompletedTasksCount()).append("/").append(assignment.getTotalTasksCount()).append(" milestones\n\n");

        if (assignment.getNotes() != null && !assignment.getNotes().trim().isEmpty()) {
            sb.append("--- NOTES & SCRATCHPAD ---\n");
            sb.append(assignment.getNotes().trim()).append("\n\n");
        }

        if (assignment.getChecklist() != null && !assignment.getChecklist().isEmpty()) {
            sb.append("--- MILESTONES ---\n");
            for (SubTask st : assignment.getChecklist()) {
                sb.append(st.isCompleted() ? "[✓] " : "[ ] ").append(st.getTitle()).append("\n");
            }
        }

        String shareText = sb.toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Assignment Notes", shareText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied summary & notes to clipboard", Toast.LENGTH_SHORT).show();
        }

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, assignment.getTitle());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, "Share Assignment");
            startActivity(shareIntent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onToggleCompleted(Assignment assignment) {
        if (assignment.isCompleted()) {
            // Restore to in progress
            assignment.setStatus("IN_PROGRESS");
            repository.update(assignment);
            loadAssignments();
            Toast.makeText(this, "Restored to In Progress", Toast.LENGTH_SHORT).show();
        } else {
            // Complete & auto remove
            triggerCompleteCurrentCard();
        }
    }

    @Override
    public void onSubTaskToggle(Assignment assignment, SubTask subTask) {
        repository.toggleSubTask(assignment.getId(), subTask.getId());
        // Re-check progress and re-bind without reloading full stack
        if (assignment.getTotalTasksCount() > 0 && assignment.getCompletedTasksCount() == assignment.getTotalTasksCount()) {
            // All tasks complete! Trigger completion
            triggerCompleteCurrentCard();
        } else {
            holderTop.bind(assignment, true, this);
        }
    }

    @Override
    public void onAddSubTask(Assignment assignment, String title) {
        repository.addSubTask(assignment.getId(), title);
        holderTop.bind(assignment, true, this);
    }

    @Override
    public void onDeleteSubTask(Assignment assignment, SubTask subTask) {
        repository.removeSubTask(assignment.getId(), subTask.getId());
        holderTop.bind(assignment, true, this);
    }

    @Override
    public void onNotesUpdated(Assignment assignment, String notes) {
        repository.updateNotes(assignment.getId(), notes);
    }

    @Override
    public void onCardDoubleTapped(Assignment assignment) {
        triggerCompleteCurrentCard();
    }

    private void vibrate(long ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
