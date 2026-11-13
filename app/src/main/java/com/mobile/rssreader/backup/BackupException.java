package com.mobile.rssreader.backup;

/**
 * 备份/恢复过程中的可读异常：消息直接展示给用户，
 * 不把 JSONException、IOException 原文抛到界面上。
 */
public class BackupException extends Exception {

    public BackupException(String message) {
        super(message);
    }

    public BackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
