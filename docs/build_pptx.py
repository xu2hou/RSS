# -*- coding: utf-8 -*-
"""生成《202405100113-邬逸帆-项目汇报》PPTX（10 页，16:9）。"""
import os

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.util import Emu, Inches, Pt

BASE = os.path.dirname(os.path.abspath(__file__))
IMAGES = os.path.join(BASE, "images")
OUT = os.path.join(BASE, "..", "202405100113-邬逸帆-项目汇报.pptx")

BRAND = RGBColor(0x3C, 0x6E, 0x9F)
DARK = RGBColor(0x1A, 0x1C, 0x1E)
GREY = RGBColor(0x5A, 0x60, 0x68)
LIGHT = RGBColor(0xF4, 0xF6, 0xF8)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
FONT = "微软雅黑"

W = Inches(13.333)
H = Inches(7.5)


def blank(prs):
    return prs.slides.add_slide(prs.slide_layouts[6])


def rect(slide, x, y, w, h, color, line=False):
    from pptx.enum.shapes import MSO_SHAPE
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, y, w, h)
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    if line:
        shape.line.color.rgb = color
    else:
        shape.line.fill.background()
    shape.shadow.inherit = False
    return shape


def text(slide, x, y, w, h, content, size=18, color=DARK, bold=False,
         align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, spacing=1.2):
    box = slide.shapes.add_textbox(x, y, w, h)
    tf = box.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    lines = content if isinstance(content, list) else [content]
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        run = p.add_run()
        run.text = line
        run.font.size = Pt(size)
        run.font.bold = bold
        run.font.color.rgb = color
        run.font.name = FONT
        # 中文需要显式设置 eastAsia
        rPr = run.font._rPr
        ea = rPr.makeelement('{http://schemas.openxmlformats.org/drawingml/2006/main}eastAsia', {})
        ea.set('typeface', FONT)
        rPr.append(ea)
    return box


def header(slide, title, subtitle=None):
    rect(slide, 0, 0, W, Inches(0.12), BRAND)
    text(slide, Inches(0.7), Inches(0.42), Inches(11.9), Inches(0.7), title, size=30, bold=True)
    if subtitle:
        text(slide, Inches(0.72), Inches(1.14), Inches(11.9), Inches(0.4), subtitle, size=14, color=GREY)


def bullets(slide, items, x=0.8, y=1.75, w=6.0, size=17, gap=0.62):
    for i, item in enumerate(items):
        text(slide, Inches(x + 0.28), Inches(y + i * gap), Inches(w), Inches(gap),
             "•  " + item, size=size, spacing=1.15)


def picture(slide, name, x, y, width):
    path = os.path.join(IMAGES, name)
    if os.path.exists(path):
        slide.shapes.add_picture(path, Inches(x), Inches(y), width=Inches(width))


def picture_h(slide, name, center_x, y, height_in):
    """按高度缩放并水平居中（竖屏截图按宽度缩放会撑破版面）。"""
    path = os.path.join(IMAGES, name)
    if os.path.exists(path):
        from PIL import Image
        with Image.open(path) as im:
            ratio = im.width / im.height
        w = height_in * ratio
        slide.shapes.add_picture(path, Inches(center_x - w / 2), Inches(y), height=Inches(height_in))
        return w
    return 0


def build():
    prs = Presentation()
    prs.slide_width = W
    prs.slide_height = H

    # 1 封面
    s = blank(prs)
    rect(s, 0, 0, W, H, WHITE)
    rect(s, 0, Inches(2.05), W, Inches(0.08), BRAND)
    text(s, Inches(0.9), Inches(1.1), Inches(11.5), Inches(0.9),
         "阅流 RSS · 本地优先的 RSS 阅读器", size=40, bold=True)
    text(s, Inches(0.9), Inches(2.35), Inches(11.5), Inches(0.6),
         "主 App（B3 RSS 阅读器） + 伴生模块（P6 备份与恢复）", size=20, color=BRAND)
    text(s, Inches(0.9), Inches(3.5), Inches(6), Inches(2.2), [
        "邬逸帆   202405100113",
        "物联网 241",
        "指导教师：邱景",
        "2026 年 11 月 13 日",
    ], size=16, color=GREY, spacing=1.5)
    text(s, Inches(7.2), Inches(3.5), Inches(5.2), Inches(2.2), [
        "Java 17 · 原生 Android",
        "Room / OkHttp / XmlPullParser / WorkManager",
        "MVVM（ViewModel + LiveData + 线程池）",
        "GitHub: xu2hou/RSS",
    ], size=13, color=GREY, spacing=1.5)

    # 2 选题与定位
    s = blank(prs)
    header(s, "一、选题与定位", "为什么做一个「本地优先」的 RSS 阅读器")
    bullets(s, [
        "RSS = 订阅固定信息源，而不是被算法推荐；国内可用客户端大多已停更",
        "主流方案把订阅与缓存放在云端，离线完全不可用",
        "本项目：订阅与文章全部落在手机本地，断网也能读已缓存内容",
        "题目：B3 RSS 阅读器（B 档·进阶级），伴生模块 P6 备份与恢复",
    ], w=11.5, size=18, gap=0.85)
    rect(s, Inches(0.8), Inches(5.6), Inches(11.7), Inches(1.2), LIGHT)
    text(s, Inches(1.05), Inches(5.78), Inches(11.2), Inches(0.9),
         "一句话定位：原生 Android 实现、可离线阅读、支持自动备份与恢复的 RSS 阅读器。",
         size=17, color=BRAND, bold=True, anchor=MSO_ANCHOR.MIDDLE)

    # 3 架构
    s = blank(prs)
    header(s, "二、系统架构", "MVVM 分层 + 显式线程模型")
    layers = [
        ("UI 层  Activity / Fragment（ViewBinding）", "只观察 LiveData、只调用 ViewModel"),
        ("ViewModel  AndroidViewModel + LiveData", "持有 Repository，不持有 View；旋转不丢状态"),
        ("Repository  Feed / Article Repository", "编排：抓取 → 解析 → 事务入库"),
        ("数据源  Room / OkHttp / SharedPreferences", "90% 逻辑可单测，不依赖模拟器"),
    ]
    for i, (left, right) in enumerate(layers):
        y = 1.8 + i * 1.15
        rect(s, Inches(0.85), Inches(y), Inches(5.6), Inches(0.85), BRAND if i % 2 == 0 else RGBColor(0x5A, 0x8C, 0xBD))
        text(s, Inches(1.0), Inches(y + 0.12), Inches(5.3), Inches(0.6), left, size=15, color=WHITE, bold=True)
        text(s, Inches(6.8), Inches(y + 0.18), Inches(6), Inches(0.6), right, size=14, color=GREY)
    text(s, Inches(0.85), Inches(6.75), Inches(11.6), Inches(0.5),
         "Java 下用「ExecutorService + LiveData」等价替代 Kotlin 协程：diskIO 单线程保序，networkIO 三线程并发。",
         size=13, color=GREY)

    # 4 双格式解析
    s = blank(prs)
    header(s, "三、关键实现①：RSS 2.0 与 Atom 双格式解析", "两种格式结构不同，同一个解析器要都能吃")
    text(s, Inches(0.8), Inches(1.7), Inches(11.8), Inches(0.5),
         "差异点：RSS 的链接是标签文本，Atom 的链接是属性；正文标签也不同。", size=16, color=GREY)
    rect(s, Inches(0.8), Inches(2.3), Inches(5.7), Inches(1.5), LIGHT)
    text(s, Inches(1.0), Inches(2.42), Inches(5.3), Inches(1.3), [
        "RSS 2.0",
        "<link>https://sspai.com/post/118587</link>",
        "<description>摘要 + &lt;a&gt;…</description>",
    ], size=13, spacing=1.4)
    rect(s, Inches(6.75), Inches(2.3), Inches(5.7), Inches(1.5), LIGHT)
    text(s, Inches(6.95), Inches(2.42), Inches(5.3), Inches(1.3), [
        "Atom",
        '<link rel="alternate" href="https://…"/>',
        '<content type="html"><![CDATA[…]]></content>',
    ], size=13, spacing=1.4)
    text(s, Inches(0.8), Inches(4.15), Inches(11.8), Inches(2.3), [
        "•  判据：<link> 有没有 href 属性 —— 有则 Atom 形态，无则等标签文本",
        "•  关闭命名空间处理，按「标签名 + 前缀」匹配，兼容 content:encoded、dc:date",
        "•  setInput(stream, null) 让解析器按 BOM / <?xml encoding?> 自判编码（UTF-8 / GBK 都能读）",
        "•  抽成两层：外层用系统 Xml，内层可注入解析器 → 普通 JVM 单测就能跑回归",
    ], size=15, spacing=1.55)

    # 5 排序与健康度
    s = blank(prs)
    header(s, "四、关键实现②：推荐排序与源健康度", "自定义业务逻辑，不是增删改查")
    rect(s, Inches(0.8), Inches(1.75), Inches(5.9), Inches(2.5), LIGHT)
    text(s, Inches(1.0), Inches(1.9), Inches(5.5), Inches(2.2), [
        "评分模型（本地纯函数）",
        "raw = 1.0 + 3.0×收藏 + 1.0×未读 + 2.0×health",
        "score = raw / (ageHours/6 + 1)^1.2",
        "",
        "时间衰减让新文章有机会压过老的高分文章",
    ], size=14, spacing=1.4)
    text(s, Inches(7.0), Inches(1.75), Inches(5.5), Inches(0.5), "失败退避（指数式）", size=16, bold=True)
    rows = [("失败 1 次", "10 分钟后重试"), ("失败 2 次", "1 小时"), ("失败 3 次", "6 小时·标记异常"), ("≥4 次", "24 小时")]
    for i, (a, b) in enumerate(rows):
        y = 2.35 + i * 0.62
        rect(s, Inches(7.0), Inches(y), Inches(5.4), Inches(0.5), LIGHT)
        text(s, Inches(7.15), Inches(y + 0.07), Inches(2.2), Inches(0.4), a, size=13, bold=True)
        text(s, Inches(9.3), Inches(y + 0.07), Inches(3.0), Inches(0.4), b, size=13, color=GREY)
    text(s, Inches(0.8), Inches(4.7), Inches(11.8), Inches(1.8), [
        "•  health ∈ [0,1]：0 次失败 = 1.0，1 次 = 0.67，达到阈值 3 次 = 0 → 经常抓失败的源在推荐流里自然降权",
        "•  后台刷新时跳过未到期的源，一个坏源不会拖垮整轮刷新",
        "•  两个算法都写成无 Android 依赖的纯函数，可直接被 JUnit 断言（共 11 个用例）",
    ], size=14, spacing=1.5)

    # 6 后台与权限
    s = blank(prs)
    header(s, "五、关键实现③：后台刷新与权限降级", "课程要求的「硬能力」与「拒绝权限后仍可用」")
    text(s, Inches(0.8), Inches(1.7), Inches(5.9), Inches(0.5), "WorkManager 定时刷新", size=17, bold=True)
    text(s, Inches(0.8), Inches(2.2), Inches(5.8), Inches(2.2), [
        "•  周期 6 小时，约束「联网才跑」",
        "•  唯一任务名 + UPDATE 策略，避免重复登记",
        "•  设置页实时显示「后台任务状态：ENQUEUED」",
        "•  失败按退避重试，不把任务标成永久失败",
    ], size=14, spacing=1.5)
    text(s, Inches(7.0), Inches(1.7), Inches(5.6), Inches(0.5), "通知权限被拒 → 降级", size=17, bold=True)
    text(s, Inches(7.0), Inches(2.2), Inches(5.6), Inches(2.2), [
        "•  页面如实提示「已拒绝通知权限，功能仍可正常使用」",
        "•  刷新与阅读完全不受影响，不崩溃不静默失败",
        "•  发通知前查权限，并捕获 SecurityException 兜底",
    ], size=14, spacing=1.5)
    picture_h(s, "06-notification.png", 9.35, 4.3, 2.4)
    picture_h(s, "05-refresh-error-retry.png", 10.75, 4.3, 2.4)
    text(s, Inches(8.0), Inches(6.85), Inches(5.0), Inches(0.4),
         "通知 / 错误态与重试", size=11, color=GREY, align=PP_ALIGN.CENTER)

    # 7 界面
    s = blank(prs)
    header(s, "六、界面展示", "5 个界面，覆盖「列表 → 详情」导航")
    for i, (name, cap) in enumerate([
        ("01-feeds-empty-state.png", "订阅列表：未读徽标"),
        ("02-articles-recommend.png", "文章列表：推荐/未读/收藏"),
        ("03-article-detail.png", "详情：WebView 正文"),
        ("04-detail-dark.png", "详情：深色模式"),
    ]):
        center = 1.75 + i * 3.1
        picture_h(s, name, center, 1.8, 3.4)
        text(s, Inches(center - 1.5), Inches(5.35), Inches(3.0), Inches(0.4), cap,
             size=12, color=GREY, align=PP_ALIGN.CENTER)
    text(s, Inches(0.8), Inches(6.05), Inches(11.8), Inches(0.8),
         "三态 UI 统一封装为 StateView：加载 / 空 / 错误（带重试）；列表为空时铺满，有内容时用 Snackbar 不打断阅读。",
         size=13, color=GREY, align=PP_ALIGN.CENTER)

    # 8 伴生模块
    s = blank(prs)
    header(s, "七、伴生模块 P6：备份与恢复", "独立可启动，读写主 App 的真实数据")
    text(s, Inches(0.8), Inches(1.7), Inches(5.9), Inches(3.4), [
        "独立入口",
        "•  桌面第二个图标（LAUNCHER）",
        "•  应用快捷方式 / 主 App 设置页也能进",
        "备份",
        "•  纯 JSON + schemaVersion 版本号",
        "•  私有目录自动命名，也可 SAF 导出",
        "•  每日自动备份，只留最近 7 份",
        "恢复",
        "•  按 url / link 去重合并，事务内完成",
        "•  可选覆盖阅读与收藏状态",
    ], size=13, spacing=1.35)
    picture_h(s, "07-backup-module.png", 9.1, 1.75, 3.3)
    rect(s, Inches(0.8), Inches(5.5), Inches(11.7), Inches(1.4), LIGHT)
    text(s, Inches(1.05), Inches(5.62), Inches(11.2), Inches(1.15), [
        "数据打通的取舍：不复用 .db 文件（WAL 下快照可能不含最新事务），也不跨进程开 Provider",
        "直接注入同一个 Room 实例，用 DAO 读全量、事务写回 —— 快照一致、事务安全、代码最简",
    ], size=13, color=GREY, spacing=1.4)

    # 9 质量
    s = blank(prs)
    header(s, "八、质量保证", "30 个单测 + 12 项手工用例 + 真实踩坑记录")
    text(s, Inches(0.8), Inches(1.7), Inches(5.6), Inches(3.6), [
        "单元测试（全部通过）",
        "•  DateUtils / HtmlUtils：时间与 HTML 清洗",
        "•  RssParser：三份离线样本做格式回归",
        "•  ArticleRanker / FeedHealth：算法数值",
        "•  BackupPayload：JSON 往返与版本兼容",
    ], size=14, spacing=1.6)
    text(s, Inches(6.9), Inches(1.7), Inches(5.7), Inches(3.6), [
        "真实踩到的坑（都进文档了）",
        "•  Android 15 边到边：内容被状态栏压住",
        "•  AGP 8 默认不生成 BuildConfig",
        "•  WorkManager 的 workdb 被写坏成 0 字节 → 静默不执行",
        "•  断网时抛英文异常，用户看不懂",
    ], size=14, spacing=1.6)
    rect(s, Inches(0.8), Inches(5.4), Inches(11.7), Inches(1.5), LIGHT)
    text(s, Inches(1.05), Inches(5.55), Inches(11.2), Inches(1.2), [
        "验证方式不靠「看代码应该没问题」：",
        "任务是否登记 → dumpsys jobscheduler；真的会跑 → cmd jobscheduler run -f 看日志；窗口是否避让 → 看截图。",
    ], size=13, color=GREY, spacing=1.4)

    # 10 结尾
    s = blank(prs)
    header(s, "九、不足与展望", None)
    text(s, Inches(0.8), Inches(1.6), Inches(11.8), Inches(3.6), [
        "•  尚未支持 OPML 批量导入导出（RSS 客户端通用标准）",
        "•  正文直接渲染源方 HTML，未做阅读模式净化",
        "•  备份为全量快照，数据量大时文件偏大（87 篇约 340 KB）",
        "•  只做本地备份与合并恢复，尚未做多设备云同步与冲突合并",
    ], size=17, spacing=1.7)
    rect(s, Inches(0.8), Inches(5.2), Inches(11.7), Inches(1.6), LIGHT)
    text(s, Inches(0.8), Inches(5.5), Inches(11.7), Inches(1.0),
         "谢谢，请老师提问", size=26, bold=True, color=BRAND, align=PP_ALIGN.CENTER)
    text(s, Inches(0.8), Inches(6.35), Inches(11.7), Inches(0.4),
         "源码与提交记录：https://github.com/xu2hou/RSS", size=12, color=GREY, align=PP_ALIGN.CENTER)

    prs.save(OUT)
    print("saved:", os.path.abspath(OUT), "slides:", len(prs.slides))


if __name__ == "__main__":
    build()
