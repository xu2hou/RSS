package com.mobile.rssreader.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mobile.rssreader.data.entity.Article;

import java.util.List;

@Dao
public interface ArticleDao {

    /** 返回值为每条插入的 rowId，冲突（已存在同 link）的行返回 -1 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long[] insertAll(List<Article> articles);

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    LiveData<List<Article>> observeAll();

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY publishedAt DESC")
    LiveData<List<Article>> observeByFeed(long feedId);

    @Query("SELECT * FROM articles WHERE starred = 1 ORDER BY publishedAt DESC")
    LiveData<List<Article>> observeStarred();

    /** 未读文章（阅读队列：所有源里没读过的） */
    @Query("SELECT * FROM articles WHERE read = 0 ORDER BY publishedAt DESC")
    LiveData<List<Article>> observeUnread();

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    LiveData<Article> observeById(long id);

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    Article findById(long id);

    @Query("SELECT COUNT(*) FROM articles WHERE read = 0")
    LiveData<Integer> observeUnreadTotal();

    @Query("UPDATE articles SET read = 1 WHERE id = :id")
    void markRead(long id);

    @Query("UPDATE articles SET starred = :starred WHERE id = :id")
    void setStarred(long id, boolean starred);

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT :limit")
    List<Article> getRecentSync(int limit);

    @Query("SELECT * FROM articles")
    List<Article> getAllSync();

    @Query("DELETE FROM articles WHERE feedId = :feedId")
    void deleteByFeed(long feedId);

    @Query("DELETE FROM articles WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM articles")
    int count();
}
