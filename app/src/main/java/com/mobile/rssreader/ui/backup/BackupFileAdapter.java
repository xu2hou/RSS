package com.mobile.rssreader.ui.backup;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.rssreader.R;
import com.mobile.rssreader.databinding.ItemBackupFileBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 备份记录列表：文件名、时间、大小。
 * 「文章条数」需要读文件解析，放到点击后的详情里，列表不做重活。
 */
public class BackupFileAdapter extends ListAdapter<File, BackupFileAdapter.VH> {

    public interface Listener {
        void onFileClick(File file);

        void onMoreClick(File file);
    }

    private static final DiffUtil.ItemCallback<File> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull File oldItem, @NonNull File newItem) {
            return oldItem.lastModified() == newItem.lastModified()
                    && oldItem.length() == newItem.length();
        }
    };

    private final Listener listener;

    public BackupFileAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBackupFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {

        private final ItemBackupFileBinding binding;

        VH(ItemBackupFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(File file, Listener listener) {
            binding.tvName.setText(file.getName());
            String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                    .format(new Date(file.lastModified()));
            double kb = file.length() / 1024.0;
            binding.tvMeta.setText(String.format(Locale.CHINA, "%s · %.1f KB", time, kb));

            binding.getRoot().setOnClickListener(v -> listener.onFileClick(file));
            binding.btnMore.setOnClickListener(v -> listener.onMoreClick(file));
        }
    }
}
