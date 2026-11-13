package com.example.personaltaskmanager.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.personaltaskmanager.R;
import com.example.personaltaskmanager.database.TaskFileDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tab 4 — Cá nhân & Cài đặt: hồ sơ người dùng, thông báo,
 * định dạng ngày giờ, sao lưu dữ liệu, giao diện, giới thiệu ứng dụng.
 */
public class ProfileFragment extends Fragment {

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private Switch switchNotification;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        switchNotification = view.findViewById(R.id.switchNotification);

        loadProfile();

        SharedPreferences settings = requireContext()
                .getSharedPreferences("settings", Context.MODE_PRIVATE);
        switchNotification.setChecked(settings.getBoolean("notifications", true));
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(requireContext(),
                    isChecked ? R.string.toast_notification_on : R.string.toast_notification_off,
                    Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.tvEditProfile).setOnClickListener(v -> showEditProfileDialog());
        view.findViewById(R.id.rowDateFormat).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.toast_date_format,
                        Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.rowBackup).setOnClickListener(v -> backupData());
        view.findViewById(R.id.rowTheme).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.toast_theme,
                        Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.rowAbout).setOnClickListener(v -> showAboutDialog());
    }

    private void loadProfile() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("profile", Context.MODE_PRIVATE);
        tvProfileName.setText(prefs.getString("name", "Khánh"));
        tvProfileEmail.setText(prefs.getString("email", "khanh@example.com"));
    }

    private void showEditProfileDialog() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("profile", Context.MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText edtName = new EditText(requireContext());
        edtName.setHint(R.string.profile_name_hint);
        edtName.setText(prefs.getString("name", "Khánh"));
        layout.addView(edtName);

        final EditText edtEmail = new EditText(requireContext());
        edtEmail.setHint(R.string.profile_email_hint);
        edtEmail.setText(prefs.getString("email", "khanh@example.com"));
        edtEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(edtEmail);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.edit_profile)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String email = edtEmail.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.error_empty_name,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prefs.edit().putString("name", name).putString("email", email).apply();
                    loadProfile();
                    Toast.makeText(requireContext(), R.string.toast_profile_saved,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Sao lưu file tasks.json ra thư mục riêng của app (không cần quyền). */
    private void backupData() {
        TaskFileDatabase db = new TaskFileDatabase(requireContext());
        File source = db.getFile();
        if (!source.exists()) {
            Toast.makeText(requireContext(), R.string.toast_backup_empty,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File backupDir = new File(requireContext().getExternalFilesDir(null), "backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File dest = new File(backupDir, "tasks_backup_" + stamp + ".json");
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            Toast.makeText(requireContext(),
                    getString(R.string.toast_backup_done, dest.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.toast_backup_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.about_text)
                .setPositiveButton(R.string.close, null)
                .show();
    }
}
