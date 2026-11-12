package com.nordic.myassignments.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nordic.myassignments.R;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.storage.AssignmentRepository;

import java.util.Locale;

public class ZenFocusDialog extends Dialog {

    public interface OnFocusSessionCompletedListener {
        void onSessionFinished(Assignment updatedAssignment, int minutesLogged);
    }

    private final Assignment assignment;
    private final OnFocusSessionCompletedListener listener;
    private final AssignmentRepository repository;

    private TextView tvFocusSubject;
    private TextView tvFocusAssignmentTitle;
    private TextView tvFocusQuote;
    private TextView tvTimerTime;
    private TextView tvTimerStatus;
    private FrameLayout btnToggleTimer;
    private ImageView ivPlayPauseIcon;
    private ImageView btnResetTimer;
    private ImageView btnCloseFocus;
    private TextView chipDuration15;
    private TextView chipDuration25;
    private TextView chipDuration45;
    private EditText etFocusScratchpad;
    private TextView btnFinishAndLogSession;

    private CountDownTimer countDownTimer;
    private long totalDurationMillis = 25 * 60 * 1000L; // default 25 mins
    private long millisRemaining = totalDurationMillis;
    private boolean isTimerRunning = false;

    private static final String[] NORDIC_QUOTES = {
            "“Quiet minds build deep understanding.”",
            "“Step by step the path is walked; snowflake by snowflake the mountain is covered.”",
            "“Focus on the single task before you; let all else dissolve.”",
            "“Simplicity is not the absence of clutter, but the presence of purpose.”",
            "“In the stillness of Nordic winter, deep thoughts take root.”"
    };

    public ZenFocusDialog(@NonNull Context context, Assignment assignment, OnFocusSessionCompletedListener listener) {
        super(context);
        this.assignment = assignment;
        this.listener = listener;
        this.repository = AssignmentRepository.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_zen_focus);

        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        initViews();
        setupListeners();
        updateTimerDisplay();
    }

    private void initViews() {
        tvFocusSubject = findViewById(R.id.tvFocusSubject);
        tvFocusAssignmentTitle = findViewById(R.id.tvFocusAssignmentTitle);
        tvFocusQuote = findViewById(R.id.tvFocusQuote);
        tvTimerTime = findViewById(R.id.tvTimerTime);
        tvTimerStatus = findViewById(R.id.tvTimerStatus);
        btnToggleTimer = findViewById(R.id.btnToggleTimer);
        ivPlayPauseIcon = findViewById(R.id.ivPlayPauseIcon);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        btnCloseFocus = findViewById(R.id.btnCloseFocus);
        chipDuration15 = findViewById(R.id.chipDuration15);
        chipDuration25 = findViewById(R.id.chipDuration25);
        chipDuration45 = findViewById(R.id.chipDuration45);
        etFocusScratchpad = findViewById(R.id.etFocusScratchpad);
        btnFinishAndLogSession = findViewById(R.id.btnFinishAndLogSession);

        tvFocusSubject.setText(assignment.getSubject());
        try {
            int accent = Color.parseColor(assignment.getColorHex());
            tvFocusSubject.setBackgroundTintList(ColorStateList.valueOf(accent));
        } catch (Exception ignored) {}

        tvFocusAssignmentTitle.setText(assignment.getTitle());

        // Random Nordic calm quote
        int quoteIndex = (int) (Math.random() * NORDIC_QUOTES.length);
        tvFocusQuote.setText(NORDIC_QUOTES[quoteIndex]);
    }

    private void setupListeners() {
        btnCloseFocus.setOnClickListener(v -> dismiss());

        btnToggleTimer.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnResetTimer.setOnClickListener(v -> resetTimer());

        chipDuration15.setOnClickListener(v -> setDuration(15));
        chipDuration25.setOnClickListener(v -> setDuration(25));
        chipDuration45.setOnClickListener(v -> setDuration(45));

        btnFinishAndLogSession.setOnClickListener(v -> finishAndLog());
    }

    private void setDuration(int minutes) {
        if (isTimerRunning) {
            pauseTimer();
        }
        totalDurationMillis = minutes * 60 * 1000L;
        millisRemaining = totalDurationMillis;
        updateTimerDisplay();

        // Update chip styles
        chipDuration15.setBackgroundResource(minutes == 15 ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipDuration15.setTextColor(minutes == 15 ? Color.WHITE : Color.parseColor("#2F3E46"));

        chipDuration25.setBackgroundResource(minutes == 25 ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipDuration25.setTextColor(minutes == 25 ? Color.WHITE : Color.parseColor("#2F3E46"));

        chipDuration45.setBackgroundResource(minutes == 45 ? R.drawable.bg_chip_filter_selected : R.drawable.bg_chip_filter_unselected);
        chipDuration45.setTextColor(minutes == 45 ? Color.WHITE : Color.parseColor("#2F3E46"));
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(millisRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisRemaining = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                millisRemaining = 0;
                updateTimerDisplay();
                ivPlayPauseIcon.setImageResource(R.drawable.ic_play);
                tvTimerStatus.setText("FOCUS SESSION COMPLETED");

                try {
                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        vibrator.vibrate(500);
                    }
                } catch (Exception ignored) {}

                Toast.makeText(getContext(), "🌿 Peaceful focus session complete!", Toast.LENGTH_LONG).show();
            }
        }.start();

        isTimerRunning = true;
        ivPlayPauseIcon.setImageResource(R.drawable.ic_pause);
        tvTimerStatus.setText("DEEP STUDY IN PROGRESS");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        ivPlayPauseIcon.setImageResource(R.drawable.ic_play);
        tvTimerStatus.setText("PAUSED");
    }

    private void resetTimer() {
        pauseTimer();
        millisRemaining = totalDurationMillis;
        updateTimerDisplay();
        tvTimerStatus.setText("READY TO FOCUS");
    }

    private void updateTimerDisplay() {
        long secondsTotal = millisRemaining / 1000;
        long minutes = secondsTotal / 60;
        long seconds = secondsTotal % 60;
        tvTimerTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void finishAndLog() {
        pauseTimer();
        long elapsedMillis = totalDurationMillis - millisRemaining;
        int elapsedMinutes = (int) Math.max(1, Math.round(elapsedMillis / 60000.0));

        // Save to repository
        repository.addFocusTime(assignment.getId(), elapsedMinutes);

        // Append Eureka scratchpad notes if student typed any
        String eureka = etFocusScratchpad.getText().toString().trim();
        if (!eureka.isEmpty()) {
            String updatedNotes = assignment.getNotes();
            if (updatedNotes == null || updatedNotes.trim().isEmpty()) {
                updatedNotes = "• Focus Session Insight (" + elapsedMinutes + "m): " + eureka;
            } else {
                updatedNotes = updatedNotes + "\n• Focus Session Insight (" + elapsedMinutes + "m): " + eureka;
            }
            repository.updateNotes(assignment.getId(), updatedNotes);
            assignment.setNotes(updatedNotes);
        }

        Assignment updated = repository.getById(assignment.getId());
        if (listener != null && updated != null) {
            listener.onSessionFinished(updated, elapsedMinutes);
        }

        Toast.makeText(getContext(), "Logged " + elapsedMinutes + "m of focus study to " + assignment.getSubject(), Toast.LENGTH_SHORT).show();
        dismiss();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
