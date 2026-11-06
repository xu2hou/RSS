package com.mobile.rssreader.util;

import java.util.regex.Pattern;

/**
 * RSS 的 description / content 里常混着 HTML 与转义实体，
 * 列表页只需要一段干净摘要，这里做最小必要的清洗。
 */
public final class HtmlUtils {

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT = Pattern.compile(
            "<(script|style)[^>]*>.*?</\\1>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACES = Pattern.compile("[\\t\\x0B\\f\\r]+");
    private static final Pattern MANY_NEWLINES = Pattern.compile("\\n{3,}");

    private HtmlUtils() {
    }

    /** HTML → 纯文本，并折叠多余空白 */
    public static String toPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String text = SCRIPT.matcher(html).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = unescape(text);
        text = SPACES.matcher(text).replaceAll(" ");
        text = text.replaceAll(" *\n *", "\n").trim();
        return MANY_NEWLINES.matcher(text).replaceAll("\n\n").trim();
    }

    public static String unescape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&nbsp;", " ")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }

    /** 截断到指定长度，用于列表摘要 */
    public static String ellipsize(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max).trim() + "…";
    }
}
