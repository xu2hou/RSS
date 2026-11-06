package com.mobile.rssreader.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 订阅源。对应 RSS/Atom 的 channel / feed 元信息。
 */
@Entity(tableName = "feeds", indices = {@Index(value = {"url"}, unique = true)})
public class Feed {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 源标题，解析失败时退化为域名 */
    @NonNull
    public String title = "";

    /** 订阅地址（唯一约束，用于去重） */
    @NonNull
    public String url = "";

    /** 站点主页 */
    public String siteUrl;

    public String description;

    /** 最近一次成功抓取时间（毫秒） */
    public long lastFetchedAt;

    /** 连续失败次数：用于健康度显示与抓取退避 */
    public int failCount;

    /** 是否参与后台自动刷新 */
    public boolean enabled = true;

    public long createdAt = System.currentTimeMillis();
}
