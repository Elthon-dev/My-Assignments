package com.nordic.myassignments.storage;

import android.content.Context;
import com.nordic.myassignments.model.Assignment;
import com.nordic.myassignments.model.SubTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AssignmentRepository {
    private static final String FILE_NAME = "my_assignments_deck.json";
    private static AssignmentRepository instance;
    private final Context context;
    private List<Assignment> assignments;

    private AssignmentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.assignments = loadFromFile();
        // Ensure there are no dummy/starter assignments at start of opening the app
        boolean changed = false;
        for (int i = this.assignments.size() - 1; i >= 0; i--) {
            String title = this.assignments.get(i).getTitle();
            if (isStarterTitle(title)) {
                this.assignments.remove(i);
                changed = true;
            }
        }
        if (changed) {
            saveToFile();
        }
    }

    private boolean isStarterTitle(String title) {
        if (title == null) return false;
        return title.contains("Nordic Ethics & Well-being Essay")
                || title.contains("Calculus Problem Set #4")
                || title.contains("Fjord Hydrology Lab")
                || title.contains("Distributed Consensus")
                || title.contains("Tomas Tranströmer");
    }

    public synchronized void markCompleted(String id) {
        Assignment a = getById(id);
        if (a != null) {
            a.setStatus("COMPLETED");
            saveToFile();
        }
    }

    public static synchronized AssignmentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AssignmentRepository(context);
        }
        return instance;
    }

    public synchronized List<Assignment> getAll() {
        return new ArrayList<>(assignments);
    }

    public synchronized Assignment getById(String id) {
        for (Assignment a : assignments) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    public synchronized void add(Assignment assignment) {
        assignments.add(0, assignment);
        saveToFile();
    }

    public synchronized void update(Assignment updated) {
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getId().equals(updated.getId())) {
                assignments.set(i, updated);
                saveToFile();
                return;
            }
        }
    }

    public synchronized void delete(String id) {
        for (int i = 0; i < assignments.size(); i++) {
            if (assignments.get(i).getId().equals(id)) {
                assignments.remove(i);
                saveToFile();
                return;
            }
        }
    }

    public synchronized void updateNotes(String id, String notes) {
        Assignment a = getById(id);
        if (a != null) {
            a.setNotes(notes);
            saveToFile();
        }
    }

    public synchronized void toggleSubTask(String assignmentId, String subTaskId) {
        Assignment a = getById(assignmentId);
        if (a != null) {
            for (SubTask st : a.getChecklist()) {
                if (st.getId().equals(subTaskId)) {
                    st.setCompleted(!st.isCompleted());
                    break;
                }
            }
            int total = a.getTotalTasksCount();
            int done = a.getCompletedTasksCount();
            if (total > 0 && done == total) {
                a.setStatus("COMPLETED");
            } else if (done > 0) {
                a.setStatus("IN_PROGRESS");
            } else if ("COMPLETED".equals(a.getStatus())) {
                a.setStatus("IN_PROGRESS");
            }
            saveToFile();
        }
    }

    public synchronized void addSubTask(String assignmentId, String title) {
        Assignment a = getById(assignmentId);
        if (a != null) {
            a.getChecklist().add(new SubTask(title, false));
            saveToFile();
        }
    }

    public synchronized void removeSubTask(String assignmentId, String subTaskId) {
        Assignment a = getById(assignmentId);
        if (a != null) {
            for (int i = 0; i < a.getChecklist().size(); i++) {
                if (a.getChecklist().get(i).getId().equals(subTaskId)) {
                    a.getChecklist().remove(i);
                    saveToFile();
                    return;
                }
            }
        }
    }

    public synchronized void addFocusTime(String assignmentId, int minutes) {
        Assignment a = getById(assignmentId);
        if (a != null) {
            a.addFocusMinutes(minutes);
            if (!a.isCompleted() && "NOT_STARTED".equals(a.getStatus())) {
                a.setStatus("IN_PROGRESS");
            }
            saveToFile();
        }
    }

    public synchronized void toggleCompleted(String assignmentId) {
        Assignment a = getById(assignmentId);
        if (a != null) {
            if (a.isCompleted()) {
                a.setStatus(a.getCompletedTasksCount() > 0 ? "IN_PROGRESS" : "NOT_STARTED");
            } else {
                a.setStatus("COMPLETED");
            }
            saveToFile();
        }
    }

    private synchronized void saveToFile() {
        try {
            JSONArray arr = new JSONArray();
            for (Assignment a : assignments) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.getId());
                obj.put("title", a.getTitle());
                obj.put("subject", a.getSubject());
                obj.put("colorHex", a.getColorHex());
                obj.put("dueTimestamp", a.getDueTimestamp());
                obj.put("priority", a.getPriority());
                obj.put("status", a.getStatus());
                obj.put("estimatedHours", a.getEstimatedHours());
                obj.put("gradeWeight", a.getGradeWeight());
                obj.put("notes", a.getNotes());
                obj.put("focusMinutesSpent", a.getFocusMinutesSpent());

                JSONArray tasksArr = new JSONArray();
                for (SubTask st : a.getChecklist()) {
                    JSONObject taskObj = new JSONObject();
                    taskObj.put("id", st.getId());
                    taskObj.put("title", st.getTitle());
                    taskObj.put("completed", st.isCompleted());
                    tasksArr.put(taskObj);
                }
                obj.put("checklist", tasksArr);
                arr.put(obj);
            }

            File file = new File(context.getFilesDir(), FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                writer.write(arr.toString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Assignment> loadFromFile() {
        List<Assignment> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return list;
        }
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Assignment a = new Assignment();
                a.setId(obj.getString("id"));
                a.setTitle(obj.getString("title"));
                a.setSubject(obj.optString("subject", "General"));
                a.setColorHex(obj.optString("colorHex", "#52796F"));
                a.setDueTimestamp(obj.optLong("dueTimestamp", System.currentTimeMillis()));
                a.setPriority(obj.optString("priority", "MEDIUM"));
                a.setStatus(obj.optString("status", "NOT_STARTED"));
                a.setEstimatedHours(obj.optDouble("estimatedHours", 2.0));
                a.setGradeWeight(obj.optInt("gradeWeight", 10));
                a.setNotes(obj.optString("notes", ""));
                a.setFocusMinutesSpent(obj.optInt("focusMinutesSpent", 0));

                List<SubTask> checklist = new ArrayList<>();
                JSONArray tasksArr = obj.optJSONArray("checklist");
                if (tasksArr != null) {
                    for (int j = 0; j < tasksArr.length(); j++) {
                        JSONObject taskObj = tasksArr.getJSONObject(j);
                        checklist.add(new SubTask(
                                taskObj.getString("id"),
                                taskObj.getString("title"),
                                taskObj.optBoolean("completed", false)
                        ));
                    }
                }
                a.setChecklist(checklist);
                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<Assignment> createStarterAssignments() {
        List<Assignment> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // 1. Philosophy
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, 2);
        List<SubTask> tasks1 = new ArrayList<>();
        tasks1.add(new SubTask("Analyze Kierkegaard's 'Either/Or' ethical sphere", true));
        tasks1.add(new SubTask("Synthesize concepts of 'Lagom' and balance in Nordic civic life", true));
        tasks1.add(new SubTask("Draft 1,500 word comparative thesis", false));
        tasks1.add(new SubTask("Format Chicago bibliography & citations", false));

        Assignment a1 = new Assignment(
                "Nordic Ethics & Well-being Essay",
                "Philosophy",
                "#354F52",
                cal.getTimeInMillis(),
                "HIGH",
                "IN_PROGRESS",
                3.5,
                25,
                "• Thesis argument: Equilibrium in Scandinavian thought focuses on communal harmony rather than isolated individualism.\n• Key source: Danish Royal Library digital archives.\n• Quote: 'Life can only be understood backwards; but it must be lived forwards.' — Kierkegaard\n• Need to verify peer-review notes from Prof. Lindholm by Friday morning.",
                tasks1,
                50
        );
        list.add(a1);

        // 2. Mathematics
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        List<SubTask> tasks2 = new ArrayList<>();
        tasks2.add(new SubTask("Problem 1-3: Conservative vector fields & curl", true));
        tasks2.add(new SubTask("Problem 4-6: Stokes' Theorem surface integrals", false));
        tasks2.add(new SubTask("Problem 7: Divergence Theorem 3D flux cylinder", false));

        Assignment a2 = new Assignment(
                "Calculus Problem Set #4: Vector Fields",
                "Mathematics",
                "#2F3E46",
                cal.getTimeInMillis(),
                "HIGH",
                "IN_PROGRESS",
                2.5,
                15,
                "• Remember: Surface orientation matters! Outward normal vector n = (r_u x r_v) / |r_u x r_v|.\n• Check boundary curve direction via right-hand rule.\n• Office hours: Tuesday 3:00 PM in Hall 402.\n• Review formula sheet for cylindrical coordinates transformation: x=r*cos(θ), y=r*sin(θ), z=z, Jacobian = r.",
                tasks2,
                25
        );
        list.add(a2);

        // 3. Ecology
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, 5);
        List<SubTask> tasks3 = new ArrayList<>();
        tasks3.add(new SubTask("Calibrate turbidity sensor readouts from fjord samples", true));
        tasks3.add(new SubTask("Plot temperature gradient vs depth in Python", true));
        tasks3.add(new SubTask("Write Discussion section on glacial melt rates", false));

        Assignment a3 = new Assignment(
                "Fjord Hydrology Lab & Field Data",
                "Ecology",
                "#52796F",
                cal.getTimeInMillis(),
                "MEDIUM",
                "IN_PROGRESS",
                4.0,
                20,
                "• Field measurements taken at Sognefjord monitoring station.\n• Average summer salinity: 32.4 PSU at 50m depth.\n• Discussion focus: Impact of seasonal freshwater influx on benthic macroinvertebrates.\n• Colleague Anders shared raw CSV dataset on cloud drive.",
                tasks3,
                45
        );
        list.add(a3);

        // 4. Computer Science
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, 8);
        List<SubTask> tasks4 = new ArrayList<>();
        tasks4.add(new SubTask("Draft architecture diagram for peer nodes", true));
        tasks4.add(new SubTask("Implement consensus protocol state machine", false));
        tasks4.add(new SubTask("Write automated stress test with 500 concurrent workers", false));

        Assignment a4 = new Assignment(
                "Distributed Consensus Protocol Engine",
                "Computer Science",
                "#4A6B82",
                cal.getTimeInMillis(),
                "MEDIUM",
                "NOT_STARTED",
                5.0,
                30,
                "• Project spec: Build Raft or Paxos leader election in Go/Java.\n• Edge cases to handle: Network partitions, split vote resolution, stale log repair.\n• Test harness provided by course repo on GitHub.\n• Milestone 1 due next week for 10 bonus points.",
                tasks4,
                0
        );
        list.add(a4);

        // 5. Literature
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        List<SubTask> tasks5 = new ArrayList<>();
        tasks5.add(new SubTask("Read Tranströmer's 'The Half-Finished Heaven'", true));
        tasks5.add(new SubTask("Annotate metaphor motifs of nature and interiority", true));
        tasks5.add(new SubTask("Submit 500-word critique to student forum", true));

        Assignment a5 = new Assignment(
                "Tomas Tranströmer Poetics Analysis",
                "Literature",
                "#C86452",
                cal.getTimeInMillis(),
                "LOW",
                "COMPLETED",
                1.5,
                10,
                "• Analysis complete and posted to the student portal.\n• Remarkable imagery of awakening and silent forest clearings.\n• Received full marks from seminar coordinator Dr. Eklund.",
                tasks5,
                30
        );
        list.add(a5);

        return list;
    }
}
