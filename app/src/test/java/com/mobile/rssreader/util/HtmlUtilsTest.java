package com.mobile.rssreader.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HtmlUtilsTest {

    @Test
    public void stripsTagsAndCollapsesWhitespace() {
        String html = "<p>第一段</p>\n\n\n<p>第二段</p>";
        String text = HtmlUtils.toPlainText(html);
        assertTrue(text.contains("第一段"));
        assertTrue(text.contains("第二段"));
        assertTrue("不应连续出现三个换行", !text.contains("\n\n\n"));
    }

    @Test
    public void removesScriptAndStyleBlocks() {
        String html = "<style>body{color:red}</style><p>正文</p><script>alert(1)</script>";
        String text = HtmlUtils.toPlainText(html);
        assertTrue(text.contains("正文"));
        assertTrue("样式表内容不应出现在摘要里", !text.contains("color:red"));
        assertTrue("脚本内容不应出现在摘要里", !text.contains("alert"));
    }

    @Test
    public void unescapesCommonEntities() {
        assertEquals("a & b", HtmlUtils.unescape("a &amp; b"));
        assertEquals("\"引号\"", HtmlUtils.unescape("&quot;引号&quot;"));
        assertEquals("it's", HtmlUtils.unescape("it&#39;s"));
        assertEquals("a<b>c", HtmlUtils.unescape("a&lt;b&gt;c"));
    }

    @Test
    public void ellipsizeKeepsShortTextAndTruncatesLong() {
        assertEquals("短文本", HtmlUtils.ellipsize("短文本", 10));
        String longText = "这是一段很长很长的摘要内容，需要被截断显示";
        String cut = HtmlUtils.ellipsize(longText, 6);
        assertTrue(cut.endsWith("…"));
        assertEquals(7, cut.length());
    }

    @Test
    public void nullInputIsSafe() {
        assertEquals("", HtmlUtils.toPlainText(null));
        assertEquals("", HtmlUtils.unescape(null));
        assertEquals("", HtmlUtils.ellipsize(null, 10));
    }
}
