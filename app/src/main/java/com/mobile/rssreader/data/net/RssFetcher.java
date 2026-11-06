package com.mobile.rssreader.data.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * RSS 抓取：OkHttp 取字节流 → 交给 {@link RssParser} 解析。
 * <p>
 * 做了三件防御：超时、响应体大小上限（防止被超大文件拖死）、非 RSS 内容识别。
 */
public final class RssFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) YueLiuRss/1.0";

    private static final String ACCEPT =
            "application/rss+xml, application/atom+xml, application/xml;q=0.9, "
                    + "text/xml;q=0.9, */*;q=0.8";

    /** 单个 feed 最大 8MB，正常 RSS 只有几十 KB */
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", ACCEPT)
                    .build()))
            .build();

    private RssFetcher() {
    }

    /**
     * 把用户输入整理成可请求的地址：允许省略 scheme，拒绝非 http(s)。
     *
     * @return 规范化 URL；非法时返回 null
     */
    public static String normalizeUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        HttpUrl url = tryParse(value);
        if (url == null && !value.toLowerCase(Locale.ROOT).matches("^[a-z][a-z0-9+.-]*://.*")) {
            url = tryParse("https://" + value);
        }
        if (url == null) {
            return null;
        }
        String scheme = url.scheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        if (url.host().isEmpty()) {
            return null;
        }
        return url.toString();
    }

    private static HttpUrl tryParse(String value) {
        try {
            return HttpUrl.get(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /**
     * 抓取并解析。
     *
     * @throws IOException 网络错误、HTTP 错误码、内容不是 RSS
     */
    public static RssParser.ParsedFeed fetch(String url)
            throws IOException, org.xmlpull.v1.XmlPullParserException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("服务器返回 HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应内容为空");
            }
            long declared = body.contentLength();
            if (declared > MAX_BYTES) {
                throw new IOException("源文件过大（>8MB），已跳过");
            }
            byte[] raw = readLimited(body.byteStream());
            RssParser.ParsedFeed parsed = RssParser.parse(new ByteArrayInputStream(raw));
            if (parsed.items.isEmpty() && parsed.title.isEmpty()) {
                throw new IOException("该地址不是有效的 RSS / Atom 源");
            }
            if (parsed.title.isEmpty()) {
                parsed.title = extractHost(url);
            }
            if (parsed.siteUrl.isEmpty()) {
                parsed.siteUrl = url;
            }
            return parsed;
        }
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024);
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new IOException("源文件过大（>8MB），已跳过");
            }
            out.write(chunk, 0, read);
        }
        return out.toByteArray();
    }

    private static String extractHost(String url) {
        try {
            return HttpUrl.get(url).host();
        } catch (Exception e) {
            return url;
        }
    }
}
