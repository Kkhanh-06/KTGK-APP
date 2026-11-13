package com.example.personaltaskmanager.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.personaltaskmanager.R;
import com.example.personaltaskmanager.database.TaskFileDatabase;
import com.example.personaltaskmanager.model.Task;
import com.example.personaltaskmanager.utils.FormatUtils;

import java.util.Calendar;

/**
 * Màn hình Thêm công việc / Chỉnh sửa công việc.
 * - Thêm mới: mở từ nút + (không có EXTRA_TASK_ID).
 * - Chỉnh sửa: mở từ màn hình chi tiết, truyền id qua Intent.
 * Kiểm tra tên công việc không được bỏ trống (setError).
 */
public class AddEditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private EditText edtTitle;
    private EditText edtDescription;
    private TextView tvDate;
    private TextView tvTime;
    private Button btnPriorityLow;
    private Button btnPriorityMedium;
    private Button btnPriorityHigh;
    private Button btnStatusTodo;
    private Button btnStatusDone;
    private Button btnSave;

    private TaskFileDatabase db;
    private Task editingTask; // null khi thêm mới
    private final Calendar dueCalendar = Calendar.getInstance();
    private int selectedPriority = Task.PRIORITY_HIGH;
    private int selectedStatus = Task.STATUS_TODO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_dark);
        toolbar.setNavigationOnClickListener(v -> finish());

        db = new TaskFileDatabase(this);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        btnPriorityLow = findViewById(R.id.btnPriorityLow);
        btnPriorityMedium = findViewById(R.id.btnPriorityMedium);
        btnPriorityHigh = findViewById(R.id.btnPriorityHigh);
        btnStatusTodo = findViewById(R.id.btnStatusTodo);
        btnStatusDone = findViewById(R.id.btnStatusDone);
        btnSave = findViewById(R.id.btnSave);

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            editingTask = db.getTaskById(taskId);
        }

        if (editingTask != null) {
            // Chế độ chỉnh sửa: điền sẵn dữ liệu cũ.
            setTitle(R.string.edit_task_title);
            edtTitle.setText(editingTask.getTitle());
            edtDescription.setText(editingTask.getDescription());
            dueCalendar.setTimeInMillis(editingTask.getDueMillis());
            selectedPriority = editingTask.getPriority();
            selectedStatus = editingTask.getStatus();
        } else {
            // Chế độ thêm mới: mặc định hôm nay 16:00, ưu tiên Cao, chưa thực hiện.
            setTitle(R.string.add_task_title);
            dueCalendar.set(Calendar.HOUR_OF_DAY, 16);
            dueCalendar.set(Calendar.MINUTE, 0);
            dueCalendar.set(Calendar.SECOND, 0);
            dueCalendar.set(Calendar.MILLISECOND, 0);
        }

        updateDateTimeViews();
        updatePriorityButtons();
        updateStatusButtons();

        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());

        btnPriorityLow.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_LOW;
            updatePriorityButtons();
        });
        btnPriorityMedium.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_MEDIUM;
            updatePriorityButtons();
        });
        btnPriorityHigh.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_HIGH;
            updatePriorityButtons();
        });
        btnStatusTodo.setOnClickListener(v -> {
            selectedStatus = Task.STATUS_TODO;
            updateStatusButtons();
        });
        btnStatusDone.setOnClickListener(v -> {
            selectedStatus = Task.STATUS_DONE;
            updateStatusButtons();
        });

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dueCalendar.set(Calendar.YEAR, year);
                    dueCalendar.set(Calendar.MONTH, month);
                    dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeViews();
                },
                dueCalendar.get(Calendar.YEAR),
                dueCalendar.get(Calendar.MONTH),
                dueCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    dueCalendar.set(Calendar.MINUTE, minute);
                    updateDateTimeViews();
                },
                dueCalendar.get(Calendar.HOUR_OF_DAY),
                dueCalendar.get(Calendar.MINUTE),
                true).show();
    }

    private void updateDateTimeViews() {
        tvDate.setText(FormatUtils.formatDate(dueCalendar.getTimeInMillis()));
        tvTime.setText(FormatUtils.formatTime(dueCalendar.getTimeInMillis()));
    }

    private void updatePriorityButtons() {
        setToggleButton(btnPriorityLow, selectedPriority == Task.PRIORITY_LOW);
        setToggleButton(btnPriorityMedium, selectedPriority == Task.PRIORITY_MEDIUM);
        setToggleButton(btnPriorityHigh, selectedPriority == Task.PRIORITY_HIGH);
    }

    private void updateStatusButtons() {
        setToggleButton(btnStatusTodo, selectedStatus == Task.STATUS_TODO);
        setToggleButton(btnStatusDone, selectedStatus == Task.STATUS_DONE);
    }

    private void setToggleButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.bg_button_primary : R.drawable.bg_button_neutral);
        button.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.white : R.color.text_secondary));
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_empty_title));
            edtTitle.requestFocus();
            return;
        }
        String description = edtDescription.getText().toString().trim();

        if (editingTask != null) {
            editingTask.setTitle(title);
            editingTask.setDescription(description);
            editingTask.setDueMillis(dueCalendar.getTimeInMillis());
            editingTask.setPriority(selectedPriority);
            editingTask.setStatus(selectedStatus);
            db.updateTask(editingTask);
            Toast.makeText(this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show();
        } else {
            Task task = new Task(title, description,
                    dueCalendar.getTimeInMillis(), selectedPriority, selectedStatus);
            db.addTask(task);
            Toast.makeText(this, R.string.toast_task_saved, Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}
