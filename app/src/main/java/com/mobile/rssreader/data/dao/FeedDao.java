package com.mobile.rssreader.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mobile.rssreader.data.entity.Feed;
import com.mobile.rssreader.data.entity.FeedWithUnread;

import java.util.List;

@Dao
public interface FeedDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Feed feed);

    @Update
    void update(Feed feed);

    @Delete
    void delete(Feed feed);

    @Query("SELECT * FROM feeds ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<Feed>> observeAll();

    @Query("SELECT * FROM feeds ORDER BY title COLLATE NOCASE ASC")
    List<Feed> getAllSync();

    /**
     * 订阅列表：带未读数。子查询在 SQLite 内完成，比 Java 层逐条统计少 N 次查询。
     */
    @Query("SELECT f.*, (SELECT COUNT(*) FROM articles a WHERE a.feedId = f.id AND a.read = 0) AS unreadCount "
            + "FROM feeds f ORDER BY f.title COLLATE NOCASE ASC")
    LiveData<List<FeedWithUnread>> observeAllWithUnread();

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    Feed findByUrl(String url);

    @Query("SELECT * FROM feeds WHERE id = :id LIMIT 1")
    Feed findById(long id);

    @Query("SELECT COUNT(*) FROM feeds")
    int count();

    @Query("UPDATE feeds SET lastFetchedAt = :time, failCount = 0 WHERE id = :feedId")
    void markFetchSuccess(long feedId, long time);

    @Query("UPDATE feeds SET failCount = failCount + 1 WHERE id = :feedId")
    void markFetchFailed(long feedId);
}
