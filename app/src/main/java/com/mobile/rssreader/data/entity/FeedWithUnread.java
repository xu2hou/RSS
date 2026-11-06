package com.mobile.rssreader.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/**
 * 订阅列表用的投影：Feed + 该源未读数。
 * 未读数在 SQL 里用子查询算好，避免在 Java 层做 N 次查询。
 */
public class FeedWithUnread {

    @Embedded
    public Feed feed;

    @ColumnInfo(name = "unreadCount")
    public int unreadCount;
}
