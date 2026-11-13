package com.example.personaltaskmanager.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personaltaskmanager.R;
import com.example.personaltaskmanager.model.Task;
import com.example.personaltaskmanager.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách công việc lên RecyclerView.
 * Mỗi dòng: chấm màu mức độ ưu tiên, tên, ngày hết hạn, chip ưu tiên,
 * trạng thái và CheckBox để đổi trạng thái ngay trên danh sách.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskActionListener {
        void onTaskClick(Task task);

        void onTaskStatusToggle(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskActionListener listener;

    public TaskAdapter(OnTaskActionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks == null ? new ArrayList<>() : tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(task.getTitle());
        holder.tvDue.setText(FormatUtils.dueLabel(task.getDueMillis()));

        // Gạch ngang tên khi đã hoàn thành.
        holder.tvTitle.setPaintFlags(task.isDone()
                ? holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        // Chấm màu + chip mức độ ưu tiên.
        int dotRes;
        int chipRes;
        int chipTextRes;
        int chipColorRes;
        switch (task.getPriority()) {
            case Task.PRIORITY_HIGH:
                dotRes = R.drawable.bg_dot_high;
                chipRes = R.drawable.bg_chip_high;
                chipTextRes = R.string.priority_high;
                chipColorRes = R.color.priority_high;
                break;
            case Task.PRIORITY_LOW:
                dotRes = R.drawable.bg_dot_low;
                chipRes = R.drawable.bg_chip_low;
                chipTextRes = R.string.priority_low;
                chipColorRes = R.color.priority_low;
                break;
            default:
                dotRes = R.drawable.bg_dot_medium;
                chipRes = R.drawable.bg_chip_medium;
                chipTextRes = R.string.priority_medium;
                chipColorRes = R.color.priority_medium;
                break;
        }
        holder.viewDot.setBackgroundResource(dotRes);
        holder.tvPriority.setBackgroundResource(chipRes);
        holder.tvPriority.setText(chipTextRes);
        holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, chipColorRes));

        // Trạng thái.
        if (task.isDone()) {
            holder.tvStatus.setText(R.string.status_done);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success));
        } else {
            holder.tvStatus.setText(R.string.status_todo);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning));
        }

        // CheckBox đổi trạng thái ngay trên danh sách (tránh gọi listener khi tái sử dụng view).
        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(task.isDone());
        holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked != task.isDone()) {
                listener.onTaskStatusToggle(task);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewDot;
        TextView tvTitle;
        TextView tvDue;
        TextView tvPriority;
        TextView tvStatus;
        CheckBox cbDone;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewDot = itemView.findViewById(R.id.viewDot);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDue = itemView.findViewById(R.id.tvDue);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cbDone = itemView.findViewById(R.id.cbDone);
        }
    }
}
