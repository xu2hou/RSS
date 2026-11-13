package com.mobile.rssreader.data.net;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

/**
 * 把底层异常翻译成用户能看懂、且能据此行动的中文文案。
 * <p>
 * 直接把 {@code e.getMessage()} 抛到界面上会得到 "Unable to resolve host ..." 这类英文，
 * 对课程验收和真实用户都不合格，因此统一在这里做映射。
 */
public final class NetworkErrorMapper {

    private NetworkErrorMapper() {
    }

    public static String toUserMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误，请稍后重试";
        }
        // 解析异常通常被包在 IOException 里，沿 cause 链找一遍（带环保护）
        Throwable current = throwable;
        int guard = 0;
        while (current != null && guard++ < 10) {
            if (current instanceof UnknownHostException || current instanceof NoRouteToHostException) {
                return "网络不可用：无法解析服务器地址，请检查网络连接";
            }
            if (current instanceof ConnectException) {
                return "网络不可用：连接服务器失败，请检查网络";
            }
            if (current instanceof SocketTimeoutException) {
                return "连接超时：服务器响应太慢，请稍后重试";
            }
            if (current instanceof SSLException || current instanceof CertificateException) {
                return "安全连接失败：证书校验未通过或网络被拦截";
            }
            if (current instanceof XmlPullParserException) {
                return "解析失败：该地址返回的内容不是有效的 RSS / Atom";
            }
            Throwable cause = current.getCause();
            if (cause == current) {
                break;
            }
            current = cause;
        }
        String raw = throwable.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "加载失败，请稍后重试";
        }
        if (raw.startsWith("HTTP ") || raw.startsWith("该地址") || raw.startsWith("源文件")
                || raw.startsWith("响应") || raw.startsWith("服务器返回")) {
            return raw; // 已经是业务层可读文案
        }
        if (throwable instanceof IOException) {
            return "网络请求失败，请稍后重试";
        }
        return "加载失败：" + raw;
    }

    /** 给错误页补一句可操作的建议 */
    public static String withSuggestion(String message) {
        return message + "\n\n可尝试：检查网络连接后点「重试」，或到「添加订阅」重新填写地址。";
    }
}
