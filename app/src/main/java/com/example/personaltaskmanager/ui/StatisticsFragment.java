package com.example.personaltaskmanager.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.personaltaskmanager.R;
import com.example.personaltaskmanager.database.TaskFileDatabase;
import com.example.personaltaskmanager.model.Task;
import com.example.personaltaskmanager.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Tab 3 — Thống kê: tổng số công việc, đã/chưa hoàn thành,
 * biểu đồ hoạt động 7 ngày và nhận xét. Có 3 kỳ: Tuần này / Tháng này / Tất cả.
 */
public class StatisticsFragment extends Fragment {

    private static final int PERIOD_WEEK = 0;
    private static final int PERIOD_MONTH = 1;
    private static final int PERIOD_ALL = 2;

    private TaskFileDatabase db;
    private int period = PERIOD_WEEK;

    private Button btnWeek;
    private Button btnMonth;
    private Button btnAll;
    private TextView tvTotalBig;
    private TextView tvTotalDelta;
    private TextView tvDoneBig;
    private TextView tvTodoBig;
    private LinearLayout llChart;
    private TextView tvComment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        btnWeek = view.findViewById(R.id.btnWeek);
        btnMonth = view.findViewById(R.id.btnMonth);
        btnAll = view.findViewById(R.id.btnAll);
        tvTotalBig = view.findViewById(R.id.tvTotalBig);
        tvTotalDelta = view.findViewById(R.id.tvTotalDelta);
        tvDoneBig = view.findViewById(R.id.tvDoneBig);
        tvTodoBig = view.findViewById(R.id.tvTodoBig);
        llChart = view.findViewById(R.id.llChart);
        tvComment = view.findViewById(R.id.tvComment);

        View.OnClickListener periodClick = v -> {
            if (v.getId() == R.id.btnWeek) {
                period = PERIOD_WEEK;
            } else if (v.getId() == R.id.btnMonth) {
                period = PERIOD_MONTH;
            } else {
                period = PERIOD_ALL;
            }
            updatePeriodButtons();
            loadData();
        };
        btnWeek.setOnClickListener(periodClick);
        btnMonth.setOnClickListener(periodClick);
        btnAll.setOnClickListener(periodClick);
        updatePeriodButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void updatePeriodButtons() {
        setPeriodButton(btnWeek, period == PERIOD_WEEK);
        setPeriodButton(btnMonth, period == PERIOD_MONTH);
        setPeriodButton(btnAll, period == PERIOD_ALL);
    }

    private void setPeriodButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.bg_button_primary : R.drawable.bg_button_neutral);
        button.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.white : R.color.text_secondary));
    }

    private void loadData() {
        List<Task> all = db.loadTasks();
        List<Task> inPeriod = new ArrayList<>();
        long now = System.currentTimeMillis();
        long periodStart = 0;
        if (period == PERIOD_WEEK) {
            periodStart = startOfWeek(now);
        } else if (period == PERIOD_MONTH) {
            periodStart = startOfMonth(now);
        }
        for (Task t : all) {
            if (period == PERIOD_ALL || t.getDueMillis() >= periodStart) {
                inPeriod.add(t);
            }
        }

        int total = inPeriod.size();
        int done = 0;
        for (Task t : inPeriod) {
            if (t.isDone()) {
                done++;
            }
        }
        int todo = total - done;

        tvTotalBig.setText(String.valueOf(total));
        tvDoneBig.setText(String.valueOf(done));
        tvTodoBig.setText(String.valueOf(todo));

        // So sánh với kỳ trước (chỉ tính cho kỳ Tuần này).
        if (period == PERIOD_WEEK) {
            int createdThisWeek = 0;
            int createdLastWeek = 0;
            long lastWeekStart = periodStart - 7L * 24 * 60 * 60 * 1000;
            for (Task t : all) {
                if (t.getCreatedAt() >= periodStart) {
                    createdThisWeek++;
                } else if (t.getCreatedAt() >= lastWeekStart) {
                    createdLastWeek++;
                }
            }
            int delta = createdThisWeek - createdLastWeek;
            tvTotalDelta.setText(delta >= 0
                    ? getString(R.string.delta_up, delta)
                    : getString(R.string.delta_down, -delta));
        } else {
            tvTotalDelta.setText(getString(R.string.in_period, total));
        }

        buildChart(all);
        buildComment(total, done);
    }

    /** Biểu đồ 7 ngày gần nhất: số công việc hết hạn mỗi ngày. */
    private void buildChart(List<Task> all) {
        llChart.removeAllViews();
        long todayStart = FormatUtils.startOfDay(System.currentTimeMillis());
        int[] counts = new int[7];
        int max = 0;
        for (int i = 0; i < 7; i++) {
            long dayStart = todayStart - (6 - i) * 24L * 60 * 60 * 1000;
            long dayEnd = dayStart + 24L * 60 * 60 * 1000;
            int count = 0;
            for (Task t : all) {
                if (t.getDueMillis() >= dayStart && t.getDueMillis() < dayEnd) {
                    count++;
                }
            }
            counts[i] = count;
            max = Math.max(max, count);
        }

        int maxBarDp = 110;
        int minBarDp = 10;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 7; i++) {
            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            column.setLayoutParams(colParams);

            int barHeightDp = max == 0 ? minBarDp
                    : Math.max(minBarDp, counts[i] * maxBarDp / max);
            View bar = new View(requireContext());
            bar.setBackgroundResource(i == 6
                    ? R.drawable.bg_bar_active : R.drawable.bg_bar);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    (int) (22 * density), (int) (barHeightDp * density));
            bar.setLayoutParams(barParams);
            column.addView(bar);

            TextView label = new TextView(requireContext());
            label.setText(FormatUtils.weekdayShort(
                    todayStart - (6 - i) * 24L * 60 * 60 * 1000));
            label.setTextSize(12);
            label.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.text_secondary));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = (int) (6 * density);
            label.setLayoutParams(labelParams);
            column.addView(label);

            llChart.addView(column);
        }
    }

    private void buildComment(int total, int done) {
        if (total == 0) {
            tvComment.setText(R.string.comment_empty);
            return;
        }
        int percent = done * 100 / total;
        if (percent >= 80) {
            tvComment.setText(getString(R.string.comment_great, percent));
        } else if (percent >= 50) {
            tvComment.setText(getString(R.string.comment_good, percent));
        } else {
            tvComment.setText(getString(R.string.comment_keep_going, percent));
        }
    }

    /** Đầu tuần hiện tại (thứ Hai 00:00). */
    private long startOfWeek(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int daysSinceMonday = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY;
        c.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);
        return FormatUtils.startOfDay(c.getTimeInMillis());
    }

    /** Đầu tháng hiện tại (ngày 1, 00:00). */
    private long startOfMonth(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return FormatUtils.startOfDay(c.getTimeInMillis());
    }
}
