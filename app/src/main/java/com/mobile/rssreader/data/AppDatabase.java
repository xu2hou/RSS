package com.mobile.rssreader.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mobile.rssreader.data.dao.ArticleDao;
import com.mobile.rssreader.data.dao.FeedDao;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.Feed;

/**
 * 应用唯一的 Room 数据库。
 * <p>
 * 伴生模块（备份与恢复）通过 {@link #DB_NAME} 与 ContentProvider 访问同一份数据，
 * 保证「主 App 与伴生模块数据打通」。
 */
@Database(entities = {Feed.class, Article.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "yueliu.db";

    private static volatile AppDatabase instance;

    public abstract FeedDao feedDao();

    public abstract ArticleDao articleDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME)
                            .build();
                }
            }
        }
        return instance;
    }
}
