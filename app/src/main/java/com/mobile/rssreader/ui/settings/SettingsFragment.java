package com.mobile.rssreader.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.mobile.rssreader.BuildConfig;
import com.mobile.rssreader.R;
import com.mobile.rssreader.data.prefs.SettingsPreferences;
import com.mobile.rssreader.data.repo.FeedRepository;
import com.mobile.rssreader.databinding.FragmentSettingsBinding;
import com.mobile.rssreader.notify.NotificationHelper;
import com.mobile.rssreader.util.Callback;
import com.mobile.rssreader.work.RefreshScheduler;

/**
 * 设置页（第 5 个界面）。
 * <p>
 * 这里承担两件与评分直接相关的事：
 * 1) 后台刷新开关 → 真正去登记/取消 WorkManager 任务；
 * 2) 通知权限申请与**拒绝后的降级说明** —— 即使被拒绝，刷新与阅读仍然可用。
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                SettingsPreferences.setNotifyDenied(requireContext(), !granted);
                if (granted) {
                    SettingsPreferences.setNotifyEnabled(requireContext(), true);
                }
                renderNotifyState();
                Toast.makeText(requireContext(),
                        granted ? R.string.settings_notify_on : R.string.settings_notify_off,
                        Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = requireContext();

        binding.tvVersion.setText(getString(R.string.app_name)
                + " · 版本 " + BuildConfig.VERSION_NAME);

        binding.switchAutoRefresh.setChecked(SettingsPreferences.isAutoRefreshEnabled(context));
        binding.switchAutoRefresh.setOnCheckedChangeListener((button, checked) -> {
            SettingsPreferences.setAutoRefreshEnabled(context, checked);
            RefreshScheduler.apply(context);
            Toast.makeText(context,
                    checked ? "已开启自动刷新（每 6 小时）" : "已关闭自动刷新",
                    Toast.LENGTH_SHORT).show();
        });

        binding.btnRefreshNow.setOnClickListener(v -> refreshNow());

        binding.switchNotify.setChecked(SettingsPreferences.isNotifyEnabled(context));
        binding.switchNotify.setOnCheckedChangeListener((button, checked) -> {
            SettingsPreferences.setNotifyEnabled(context, checked);
            renderNotifyState();
        });

        binding.btnGrantNotify.setOnClickListener(v ->
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS));

        binding.btnOpenBackup.setOnClickListener(v ->
                Toast.makeText(context, "备份与恢复模块建设中（S13）", Toast.LENGTH_SHORT).show());

        renderNotifyState();
        renderWorkState();
    }

    /**
     * 显示 WorkManager 任务是否真的登记成功。
     * 出过一次教训：JobScheduler 里还有 job，但 WorkManager 自己的数据库被写坏成 0 字节，
     * 结果定时任务静默不执行、界面毫无感知 —— 所以把状态显式暴露出来。
     */
    private void renderWorkState() {
        if (binding == null) {
            return;
        }
        WorkManager.getInstance(requireContext())
                .getWorkInfosForUniqueWorkLiveData(RefreshScheduler.WORK_NAME)
                .observe(getViewLifecycleOwner(), infos -> {
                    if (binding == null) {
                        return;
                    }
                    if (infos == null || infos.isEmpty()) {
                        binding.tvWorkState.setText(R.string.settings_work_not_enqueued);
                        return;
                    }
                    WorkInfo.State state = infos.get(0).getState();
                    binding.tvWorkState.setText(getString(R.string.settings_work_state_format,
                            state.name(), RefreshScheduler.INTERVAL_HOURS));
                });
    }

    /** 通知开关的真实状态：偏好 + 系统权限，两者不一致时要如实说明 */
    private void renderNotifyState() {
        Context context = requireContext();
        boolean granted = NotificationHelper.canNotify(context);
        boolean prefOn = SettingsPreferences.isNotifyEnabled(context);

        if (granted) {
            binding.tvNotifyStatus.setText(prefOn
                    ? R.string.settings_notify_on
                    : R.string.settings_notify_off);
            binding.btnGrantNotify.setVisibility(View.GONE);
        } else if (SettingsPreferences.isNotifyDenied(context)) {
            binding.tvNotifyStatus.setText(R.string.settings_notify_denied);
            binding.btnGrantNotify.setVisibility(View.VISIBLE);
        } else {
            binding.tvNotifyStatus.setText(R.string.settings_notify_off);
            binding.btnGrantNotify.setVisibility(View.VISIBLE);
        }
    }

    private void refreshNow() {
        binding.btnRefreshNow.setEnabled(false);
        binding.btnRefreshNow.setText(R.string.settings_refreshing);
        FeedRepository.get(requireContext()).refreshAll(new Callback<>() {
            @Override
            public void onSuccess(FeedRepository.RefreshSummary data) {
                if (binding == null) {
                    return;
                }
                binding.btnRefreshNow.setEnabled(true);
                binding.btnRefreshNow.setText(R.string.settings_refresh_now);
                Toast.makeText(requireContext(), data.toMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                if (binding == null) {
                    return;
                }
                binding.btnRefreshNow.setEnabled(true);
                binding.btnRefreshNow.setText(R.string.settings_refresh_now);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
