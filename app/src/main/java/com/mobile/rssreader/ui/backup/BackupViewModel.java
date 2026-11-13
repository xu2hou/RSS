package com.mobile.rssreader.ui.backup;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mobile.rssreader.backup.BackupException;
import com.mobile.rssreader.backup.BackupManager;
import com.mobile.rssreader.backup.BackupScheduler;
import com.mobile.rssreader.data.prefs.SettingsPreferences;
import com.mobile.rssreader.util.AppExecutors;
import com.mobile.rssreader.util.Event;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 伴生模块的 ViewModel：所有文件 I/O 与数据库操作都在后台线程，
 * 页面只负责渲染结果与提示，权限/异常统统转成可读文案。
 */
public class BackupViewModel extends AndroidViewModel {

    private final BackupManager manager;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<List<File>> backups = new MutableLiveData<>();
    private final MutableLiveData<String> summary = new MutableLiveData<>();

    public BackupViewModel(@NonNull Application application) {
        super(application);
        manager = new BackupManager(application);
        refreshList();
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public LiveData<Event<String>> getMessage() {
        return message;
    }

    public LiveData<List<File>> getBackups() {
        return backups;
    }

    public LiveData<String> getSummary() {
        return summary;
    }

    public void refreshList() {
        AppExecutors.get().diskIO().execute(() -> {
            List<File> files = manager.listBackups();
            backups.postValue(files);
        });
    }
    /** 备份到私有目录 */
    public void backupNow() {
        busy.setValue(true);
        AppExecutors.get().diskIO().execute(() -> {
            try {
                File file = manager.backupToLocal();
                int count = manager.countArticlesIn(file);
                message.postValue(new Event<>("备份完成：" + file.getName() + "（" + count + " 篇）"));
            } catch (BackupException e) {
                message.postValue(new Event<>(e.getMessage()));
            } finally {
                busy.postValue(false);
                refreshList();
                loadSummary();
            }
        });
    }

    /** 导出到 SAF 选中的位置 */
    public void exportTo(Uri uri) {
        busy.setValue(true);
        AppExecutors.get().diskIO().execute(() -> {
            try (OutputStream out = getApplication().getContentResolver().openOutputStream(uri)) {
                if (out == null) {
                    message.postValue(new Event<>("无法写入所选位置"));
                    return;
                }
                int count = manager.exportTo(out);
                message.postValue(new Event<>("已导出 " + count + " 篇文章"));
            } catch (BackupException | java.io.IOException e) {
                message.postValue(new Event<>("导出失败：" + e.getMessage()));
            } finally {
                busy.postValue(false);
            }
        });
    }

    /** 从 SAF 选中的文件恢复 */
    public void importFrom(Uri uri, boolean overrideStates) {
        busy.setValue(true);
        AppExecutors.get().diskIO().execute(() -> {
            try (InputStream in = getApplication().getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    message.postValue(new Event<>("无法读取所选文件"));
                    return;
                }
                BackupManager.RestoreResult result = manager.restore(in, overrideStates);
                message.postValue(new Event<>(result.toMessage()));
            } catch (BackupException | java.io.IOException e) {
                message.postValue(new Event<>(e.getMessage()));
            } finally {
                busy.postValue(false);
                refreshList();
                loadSummary();
            }
        });
    }

    /** 从本机备份记录恢复 */
    public void restoreFromFile(File file, boolean overrideStates) {
        busy.setValue(true);
        AppExecutors.get().diskIO().execute(() -> {
            try {
                BackupManager.RestoreResult result = manager.restoreFromFile(file, overrideStates);
                message.postValue(new Event<>(result.toMessage()));
            } catch (BackupException e) {
                message.postValue(new Event<>(e.getMessage()));
            } finally {
                busy.postValue(false);
                refreshList();
                loadSummary();
            }
        });
    }

    public void delete(File file) {
        AppExecutors.get().diskIO().execute(() -> {
            boolean ok = file.delete();
            message.postValue(new Event<>(ok ? "已删除备份文件" : "删除失败"));
            refreshList();
        });
    }

    public void setAutoBackup(boolean enabled) {
        SettingsPreferences.setAutoBackupEnabled(getApplication(), enabled);
        BackupScheduler.apply(getApplication());
        message.setValue(new Event<>(enabled ? "已开启每日自动备份" : "已关闭每日自动备份"));
    }

    public boolean isAutoBackupEnabled() {
        return SettingsPreferences.isAutoBackupEnabled(getApplication());
    }

    /** 顶部概览：当前数据量 */
    public void loadSummary() {
        AppExecutors.get().diskIO().execute(() -> {
            com.mobile.rssreader.data.AppDatabase db =
                    com.mobile.rssreader.data.AppDatabase.get(getApplication());
            int feeds = db.feedDao().count();
            int articles = db.articleDao().count();
            summary.postValue("当前数据：" + feeds + " 个订阅源、" + articles + " 篇文章");
        });
    }
}
