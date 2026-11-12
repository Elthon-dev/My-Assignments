package com.nordic.myassignments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.nordic.myassignments.adapter.AssignmentFlipcardAdapter;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.model.SubTask;
import com.nordic.myassignments.storage.AssignmentRepository;
import com.nordic.myassignments.ui.AddEditAssignmentDialog;
import com.nordic.myassignments.ui.ZenFocusDialog;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AssignmentFlipcardAdapter.OnAssignmentActionListener {

    private enum FilterMode {
        ALL, DUE_SOON, ACTIVE, HIGH_PRIORITY, COMPLETED
    }

    private AssignmentRepository repository;
    private AssignmentFlipcardAdapter adapter;
    private List<Assignment> displayedAssignments = new ArrayList<>();
    private FilterMode currentFilter = FilterMode.ALL;

    private ViewPager2 viewPagerCards;
    private LinearLayout layoutEmptyDeck;
    private LinearLayout btnAddNewAssignment;
    private TextView btnEmptyCreateCard;
    private TextView tvCardDeckCounter;
    private ImageView btnPrevCard;
    private ImageView btnNextCard;

    private TextView chipFilterAll;
    private TextView chipFilterDueSoon;
    private TextView chipFilterActive;
    private TextView chipFilterPriority;
    private TextView chipFilterCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = AssignmentRepository.getInstance(this);

        initViews();
        setupViewPager();
        setupFilters();
        setupActions();

        loadAssignments();
    }

    private void initViews() {
        viewPagerCards = findViewById(R.id.viewPagerCards);
        layoutEmptyDeck = findViewById(R.id.layoutEmptyDeck);
        btnAddNewAssignment = findViewById(R.id.btnAddNewAssignment);
        btnEmptyCreateCard = findViewById(R.id.btnEmptyCreateCard);
        tvCardDeckCounter = findViewById(R.id.tvCardDeckCounter);
        btnPrevCard = findViewById(R.id.btnPrevCard);
        btnNextCard = findViewById(R.id.btnNextCard);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterDueSoon = findViewById(R.id.chipFilterDueSoon);
        chipFilterActive = findViewById(R.id.chipFilterActive);
        chipFilterPriority = findViewById(R.id.chipFilterPriority);
        chipFilterCompleted = findViewById(R.id.chipFilterCompleted);
    }

    private void setupViewPager() {
        adapter = new AssignmentFlipcardAdapter(this, this);
        viewPagerCards.setAdapter(adapter);
        viewPagerCards.setOffscreenPageLimit(3);

        // Tactile Nordic flipcard page transformer with depth stack effect
        float density = getResources().getDisplayMetrics().density;
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer((int) (10 * density)));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.90f + r * 0.10f);
            page.setAlpha(0.4f + r * 0.6f);
        });
        viewPagerCards.setPageTransformer(transformer);

        viewPagerCards.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position);
            }
        });
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
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_filter_unselected);
            chip.setTextColor(Color.parseColor("#2F3E46"));
        }
    }

    private void setupActions() {
        btnAddNewAssignment.setOnClickListener(v -> openAddEditDialog(null));
        btnEmptyCreateCard.setOnClickListener(v -> openAddEditDialog(null));

        btnPrevCard.setOnClickListener(v -> {
            int current = viewPagerCards.getCurrentItem();
            if (current > 0) {
                viewPagerCards.setCurrentItem(current - 1, true);
            }
        });

        btnNextCard.setOnClickListener(v -> {
            int current = viewPagerCards.getCurrentItem();
            if (current < displayedAssignments.size() - 1) {
                viewPagerCards.setCurrentItem(current + 1, true);
            }
        });
    }

    private void loadAssignments() {
        List<Assignment> all = repository.getAll();
        displayedAssignments.clear();

        for (Assignment a : all) {
            switch (currentFilter) {
                case ALL:
                    displayedAssignments.add(a);
                    break;
                case DUE_SOON:
                    if (a.isDueSoon()) displayedAssignments.add(a);
                    break;
                case ACTIVE:
                    if (!a.isCompleted()) displayedAssignments.add(a);
                    break;
                case HIGH_PRIORITY:
                    if ("HIGH".equalsIgnoreCase(a.getPriority())) displayedAssignments.add(a);
                    break;
                case COMPLETED:
                    if (a.isCompleted()) displayedAssignments.add(a);
                    break;
            }
        }

        adapter.setAssignments(displayedAssignments);

        if (displayedAssignments.isEmpty()) {
            layoutEmptyDeck.setVisibility(View.VISIBLE);
            viewPagerCards.setVisibility(View.GONE);
            findViewById(R.id.layoutBottomDeckControls).setVisibility(View.GONE);
        } else {
            layoutEmptyDeck.setVisibility(View.GONE);
            viewPagerCards.setVisibility(View.VISIBLE);
            findViewById(R.id.layoutBottomDeckControls).setVisibility(View.VISIBLE);
            updateCounter(viewPagerCards.getCurrentItem());
        }
    }

    private void updateCounter(int currentPosition) {
        int total = displayedAssignments.size();
        if (total == 0) {
            tvCardDeckCounter.setText("0 Cards");
            btnPrevCard.setEnabled(false);
            btnNextCard.setEnabled(false);
            return;
        }

        int index = Math.min(currentPosition + 1, total);
        tvCardDeckCounter.setText("Card " + index + " of " + total);
        btnPrevCard.setEnabled(currentPosition > 0);
        btnNextCard.setEnabled(currentPosition < total - 1);
        btnPrevCard.setAlpha(currentPosition > 0 ? 1.0f : 0.35f);
        btnNextCard.setAlpha(currentPosition < total - 1 ? 1.0f : 0.35f);
    }

    private void openAddEditDialog(Assignment assignment) {
        AddEditAssignmentDialog dialog = new AddEditAssignmentDialog(this, assignment, (saved, isNew) -> {
            loadAssignments();
            if (isNew) {
                viewPagerCards.setCurrentItem(0, true);
            }
        });
        dialog.show();
    }

    // ================= ADAPTER CALLBACKS =================

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
                .setMessage("Are you sure you want to remove '" + assignment.getTitle() + "' from your pad?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.delete(assignment.getId());
                    loadAssignments();
                    Toast.makeText(this, "Card removed", Toast.LENGTH_SHORT).show();
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
            sb.append("--- CHECKLIST ---\n");
            for (SubTask st : assignment.getChecklist()) {
                sb.append(st.isCompleted() ? "[✓] " : "[ ] ").append(st.getTitle()).append("\n");
            }
        }

        String shareText = sb.toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Assignment Notes", shareText);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied assignment summary & notes to clipboard!", Toast.LENGTH_SHORT).show();
        }

        // Also offer system share sheet
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, assignment.getTitle());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, "Share Assignment Notes");
            startActivity(shareIntent);
        } catch (Exception ignored) {}
    }

    @Override
    public void onToggleCompleted(Assignment assignment) {
        repository.toggleCompleted(assignment.getId());
        loadAssignments();
    }

    @Override
    public void onSubTaskToggle(Assignment assignment, SubTask subTask) {
        repository.toggleSubTask(assignment.getId(), subTask.getId());
        loadAssignments();
    }

    @Override
    public void onAddSubTask(Assignment assignment, String title) {
        repository.addSubTask(assignment.getId(), title);
        loadAssignments();
    }

    @Override
    public void onDeleteSubTask(Assignment assignment, SubTask subTask) {
        repository.removeSubTask(assignment.getId(), subTask.getId());
        loadAssignments();
    }

    @Override
    public void onNotesUpdated(Assignment assignment, String notes) {
        repository.updateNotes(assignment.getId(), notes);
    }
}
