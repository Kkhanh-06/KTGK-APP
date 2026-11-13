package com.example.personaltaskmanager.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personaltaskmanager.R;
import com.example.personaltaskmanager.adapter.TaskAdapter;
import com.example.personaltaskmanager.database.TaskFileDatabase;
import com.example.personaltaskmanager.model.Task;
import com.example.personaltaskmanager.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tab 1 — Tổng quan: lời chào, thẻ tiến độ hôm nay, 3 ô thống kê
 * và danh sách "Ưu tiên hôm nay" (tối đa 3 việc).
 */
public class HomeFragment extends Fragment {

    private TaskFileDatabase db;
    private TaskAdapter adapter;
    private TextView tvGreeting;
    private TextView tvProgressPercent;
    private TextView tvProgressText;
    private ProgressBar progressBar;
    private TextView tvTotalTasks;
    private TextView tvDoneTasks;
    private TextView tvTodoTasks;
    private TextView tvEmptyToday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        progressBar = view.findViewById(R.id.progressBar);
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks);
        tvDoneTasks = view.findViewById(R.id.tvDoneTasks);
        tvTodoTasks = view.findViewById(R.id.tvTodoTasks);
        tvEmptyToday = view.findViewById(R.id.tvEmptyToday);

        RecyclerView rvPriority = view.findViewById(R.id.rvPriorityToday);
        rvPriority.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPriority.setNestedScrollingEnabled(false);
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(Task task) {
                openDetail(task);
            }

            @Override
            public void onTaskStatusToggle(Task task) {
                db.toggleStatus(task.getId());
                loadData();
            }
        });
        rvPriority.setAdapter(adapter);

        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTasksTab();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("profile", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "Khánh");
        tvGreeting.setText(getString(R.string.greeting, name));

        List<Task> tasks = db.loadTasks();
        int total = tasks.size();
        int done = 0;
        for (Task t : tasks) {
            if (t.isDone()) {
                done++;
            }
        }
        int todo = total - done;
        int percent = total == 0 ? 0 : done * 100 / total;

        tvProgressPercent.setText(percent + "%");
        tvProgressText.setText(getString(R.string.progress_text, done, total));
        progressBar.setProgress(percent);
        tvTotalTasks.setText(String.valueOf(total));
        tvDoneTasks.setText(String.valueOf(done));
        tvTodoTasks.setText(String.valueOf(todo));

        // Ưu tiên hôm nay: việc chưa làm, hết hạn hôm nay, ưu tiên cao trước.
        List<Task> today = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.isDone() && FormatUtils.isToday(t.getDueMillis())) {
                today.add(t);
            }
        }
        if (today.isEmpty()) {
            // Không có việc hết hạn hôm nay thì lấy việc chưa làm gần hạn nhất.
            for (Task t : tasks) {
                if (!t.isDone()) {
                    today.add(t);
                }
            }
        }
        Collections.sort(today, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                if (a.getPriority() != b.getPriority()) {
                    return b.getPriority() - a.getPriority();
                }
                return Long.compare(a.getDueMillis(), b.getDueMillis());
            }
        });
        if (today.size() > 3) {
            today = today.subList(0, 3);
        }
        adapter.setTasks(today);
        tvEmptyToday.setVisibility(today.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetail(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }
}
