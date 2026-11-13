package com.mobile.rssreader.ui.backup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mobile.rssreader.R;
import com.mobile.rssreader.databinding.ActivityBackupBinding;

import java.io.File;

/**
 * 伴生模块入口（P6 备份与恢复）。
 * <p>
 * 这是一个**独立可启动**的 Activity：既有自己的桌面图标（LAUNCHER），
 * 也能从主 App 设置页或应用快捷方式进入。数据通过 {@code BackupManager}
 * 直接读写主 App 的 Room 数据库，文件分享走 FileProvider。
 */
public class BackupActivity extends AppCompatActivity implements BackupFileAdapter.Listener {

    private ActivityBackupBinding binding;
    private BackupViewModel viewModel;
    private BackupFileAdapter adapter;

    private final ActivityResultLauncher<String> createDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> {
                        if (uri != null) {
                            viewModel.exportTo(uri);
                        }
                    });

    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) {
                            confirmRestore(null, uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(BackupViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new BackupFileAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.btnBackupNow.setOnClickListener(v -> viewModel.backupNow());
        binding.btnExport.setOnClickListener(v ->
                createDocument.launch("yueliu-backup.json"));
        binding.btnImport.setOnClickListener(v ->
                openDocument.launch(new String[]{"application/json", "text/plain", "*/*"}));

        binding.switchAutoBackup.setChecked(viewModel.isAutoBackupEnabled());
        binding.switchAutoBackup.setOnCheckedChangeListener((button, checked) ->
                viewModel.setAutoBackup(checked));

        observe();
        viewModel.loadSummary();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void observe() {
        viewModel.getSummary().observe(this, text -> binding.tvSummary.setText(text));

        viewModel.getBusy().observe(this, busy -> {
            boolean running = busy != null && busy;
            binding.btnBackupNow.setEnabled(!running);
            binding.btnExport.setEnabled(!running);
            binding.btnImport.setEnabled(!running);
        });

        viewModel.getMessage().observe(this, event -> {
            if (event == null) {
                return;
            }
            String text = event.getContentIfNotHandled();
            if (text != null) {
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getBackups().observe(this, files -> {
            if (files == null || files.isEmpty()) {
                adapter.submitList(null);
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.recycler.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.recycler.setVisibility(View.VISIBLE);
                adapter.submitList(files);
            }
        });
    }

    /** 恢复前的确认：明确问「要不要覆盖已有阅读/收藏状态」，合并策略由用户决定 */
    private void confirmRestore(File file, Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_restore_confirm_title)
                .setMessage(R.string.backup_restore_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setNeutralButton(R.string.backup_restore_keep, (d, w) -> doRestore(file, uri, false))
                .setPositiveButton(R.string.backup_restore_override, (d, w) -> doRestore(file, uri, true))
                .show();
    }

    private void doRestore(File file, Uri uri, boolean override) {
        if (uri != null) {
            viewModel.importFrom(uri, override);
        } else if (file != null) {
            viewModel.restoreFromFile(file, override);
        }
    }

    @Override
    public void onFileClick(File file) {
        confirmRestore(file, null);
    }

    @Override
    public void onMoreClick(File file) {
        new MaterialAlertDialogBuilder(this)
                .setItems(new CharSequence[]{
                        getString(R.string.backup_menu_restore),
                        getString(R.string.backup_menu_share),
                        getString(R.string.backup_menu_delete)
                }, (dialog, which) -> {
                    if (which == 0) {
                        confirmRestore(file, null);
                    } else if (which == 1) {
                        shareFile(file);
                    } else {
                        new MaterialAlertDialogBuilder(this)
                                .setMessage(R.string.backup_delete_confirm)
                                .setNegativeButton(R.string.action_cancel, null)
                                .setPositiveButton(R.string.backup_menu_delete, (d, w) ->
                                        viewModel.delete(file))
                                .show();
                    }
                })
                .show();
    }

    /** 通过 FileProvider 把备份文件分享给其它 App（评分点「分享/导出」） */
    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.backup_menu_share)));
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "无法分享：文件路径未在 FileProvider 中声明",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshList();
        viewModel.loadSummary();
    }
}
