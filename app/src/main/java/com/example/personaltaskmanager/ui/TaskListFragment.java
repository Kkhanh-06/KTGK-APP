package com.example.personaltaskmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tab 2 — Danh sách công việc: tìm kiếm theo tên, lọc theo trạng thái
 * (Tất cả / Chưa làm / Đã hoàn thành) và lọc theo mức độ ưu tiên.
 * Đổi trạng thái ngay trên danh sách bằng CheckBox.
 */
public class TaskListFragment extends Fragment {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_TODO = 1;
    private static final int FILTER_DONE = 2;

    private TaskFileDatabase db;
    private TaskAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();

    private EditText edtSearch;
    private TextView tvDateHeader;
    private TextView tvTaskCount;
    private TextView tvEmpty;
    private int statusFilter = FILTER_ALL;
    private int priorityFilter = -1; // -1: tất cả, 0/1/2: Thấp/Trung bình/Cao

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        edtSearch = view.findViewById(R.id.edtSearch);
        tvDateHeader = view.findViewById(R.id.tvDateHeader);
        tvTaskCount = view.findViewById(R.id.tvTaskCount);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        RecyclerView rvTasks = view.findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(Task task) {
                Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
                intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                startActivity(intent);
            }

            @Override
            public void onTaskStatusToggle(Task task) {
                db.toggleStatus(task.getId());
                loadData();
            }
        });
        rvTasks.setAdapter(adapter);

        // Tìm kiếm theo tên.
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Lọc theo trạng thái.
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupStatus);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTodo) {
                statusFilter = FILTER_TODO;
            } else if (checkedId == R.id.chipDone) {
                statusFilter = FILTER_DONE;
            } else {
                statusFilter = FILTER_ALL;
            }
            applyFilter();
        });

        // Lọc theo mức độ ưu tiên.
        Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.priority_filter,
                android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(spinnerAdapter);
        spinnerPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                // 0: Tất cả, 1: Cao, 2: Trung bình, 3: Thấp
                switch (position) {
                    case 1:
                        priorityFilter = Task.PRIORITY_HIGH;
                        break;
                    case 2:
                        priorityFilter = Task.PRIORITY_MEDIUM;
                        break;
                    case 3:
                        priorityFilter = Task.PRIORITY_LOW;
                        break;
                    default:
                        priorityFilter = -1;
                        break;
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        tvDateHeader.setText(FormatUtils.formatDayHeader(System.currentTimeMillis())
                .toUpperCase(Locale.forLanguageTag("vi-VN")));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allTasks = db.loadTasks();
        applyFilter();
    }

    private void applyFilter() {
        String query = edtSearch.getText().toString().trim()
                .toLowerCase(Locale.getDefault());
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            if (!query.isEmpty()
                    && !task.getTitle().toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            if (statusFilter == FILTER_TODO && task.isDone()) {
                continue;
            }
            if (statusFilter == FILTER_DONE && !task.isDone()) {
                continue;
            }
            if (priorityFilter != -1 && task.getPriority() != priorityFilter) {
                continue;
            }
            filtered.add(task);
        }
        adapter.setTasks(filtered);
        tvTaskCount.setText(getString(R.string.task_count, filtered.size()));
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
