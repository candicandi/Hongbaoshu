#!/usr/bin/env python3
"""
Generate an EPUB from the Chinese markdown source ``原文.md``.

Features:
- Uses first-level headings (#) as chapters and builds the table of contents.
- Adds a 2-em text indent to every paragraph for Chinese typography.
- Embeds the provided cover image.

Run: python md_to_epub.py
"""
from __future__ import annotations

import datetime
import html
import re
import tempfile
import zipfile
from pathlib import Path
from typing import List, Optional, Sequence, Tuple

TITLE = "毛主席语录"
INPUT_MD = Path("原文.md")
COVER_IMAGE = Path("cover.png")
OUTPUT_EPUB = Path("毛主席语录.epub")


Paragraph = Tuple[str, Optional[str]]
Section = Tuple[str, List[Paragraph]]


def split_citation(paragraph: str) -> Paragraph:
    """
    If a paragraph ends with citation markers wrapped in Chinese brackets 【...】,
    split it out. Example:
    正文内容。【《中国社会各阶级的分析》……】
    """
    # Look for the last opening bracket; assume citation is at end.
    start = paragraph.rfind("【")
    end = paragraph.rfind("】")
    if start == -1 or end == -1 or end < start:
        return paragraph, None
    body = paragraph[:start].rstrip()
    citation = paragraph[start + 1 : end].strip()
    if not body or not citation:
        return paragraph, None
    return body, citation


def parse_markdown(md_text: str) -> List[Section]:
    """
    Parse markdown text into a list of (title, paragraphs).
    Only level-1 headings start chapters; paragraphs are separated by blank lines.
    """
    sections: List[Section] = []
    current_title = None
    current_lines: List[str] = []

    def flush():
        nonlocal current_title, current_lines
        if current_title is None:
            return
        paragraphs: List[Paragraph] = []
        buf: List[str] = []
        for raw_line in current_lines:
            line = raw_line.strip()
            if not line:
                if buf:
                    text = " ".join(buf).strip()
                    paragraphs.append(split_citation(text))
                    buf = []
                continue
            buf.append(line)
        if buf:
            text = " ".join(buf).strip()
            paragraphs.append(split_citation(text))
        sections.append((current_title, paragraphs))
        current_title = None
        current_lines = []

    for line in md_text.splitlines():
        if line.startswith("#"):
            heading = line.lstrip("#").strip()
            if heading:
                flush()
                current_title = heading
                continue
        current_lines.append(line)
    flush()

    if not sections:
        raise ValueError("未找到任何一级标题，无法生成章节。")
    return sections


def slug_for(index: int) -> str:
    # Use stable ASCII ids to avoid surprises with non-ASCII anchors.
    return f"section-{index + 1}"


def build_content_xhtml(sections: Sequence[Section]) -> str:
    parts = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<!DOCTYPE html>',
        '<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">',
        "<head>",
        f"  <title>{html.escape(TITLE)}</title>",
        '  <link rel="stylesheet" type="text/css" href="style.css"/>',
        "</head>",
        "<body>",
    ]
    for idx, (title, paragraphs) in enumerate(sections):
        section_id = slug_for(idx)
        parts.append(f'  <section id="{section_id}">')
        parts.append(f"    <h1>{html.escape(title)}</h1>")
        for body, citation in paragraphs:
            parts.append(f"    <p>{html.escape(body)}</p>")
            if citation:
                parts.append(f'    <p class="citation">{html.escape(citation)}</p>')
        parts.append("  </section>")
    parts.append("</body>")
    parts.append("</html>")
    return "\n".join(parts)


def build_nav_xhtml(sections: Sequence[Section]) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<!DOCTYPE html>',
        '<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">',
        "<head>",
        f"  <title>{html.escape(TITLE)} - 目录</title>",
        '  <link rel="stylesheet" type="text/css" href="style.css"/>',
        "</head>",
        "<body>",
        '  <nav epub:type="toc" id="toc">',
        "    <h1>目录</h1>",
        "    <ol>",
    ]
    for idx, (title, _) in enumerate(sections):
        section_id = slug_for(idx)
        lines.append(f'      <li><a href="content.xhtml#{section_id}">{html.escape(title)}</a></li>')
    lines += [
        "    </ol>",
        "  </nav>",
        "</body>",
        "</html>",
    ]
    return "\n".join(lines)


def build_cover_xhtml() -> str:
    return "\n".join(
        [
            '<?xml version="1.0" encoding="utf-8"?>',
            '<!DOCTYPE html>',
            '<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">',
            "<head>",
            f"  <title>{html.escape(TITLE)} - 封面</title>",
            '  <link rel="stylesheet" type="text/css" href="style.css"/>',
            "</head>",
            '<body class="cover-page">',
            '  <div class="cover-wrapper">',
            '    <img src="cover.png" alt="封面"/>',
            "  </div>",
            "</body>",
            "</html>",
        ]
    )


def build_style_css() -> str:
    return "\n".join(
        [
            "body {",
            "  margin: 0 auto;",
            "  padding: 1.5rem;",
            "  font-family: serif;",
            "  line-height: 1.6;",
            "}",
            "h1 {",
            "  margin: 1.5rem 0 1rem;",
            "  text-align: center;",
            "}",
            "p {",
            "  margin: 0 0 1em 0;",
            "  text-indent: 2em;",  # Chinese reading habit
            "}",
            ".citation {",
            "  margin: 0 0 1.2em 0;",
            "  text-indent: 0;",
            "  padding-left: 2em;",
            "  font-size: 0.9em;",
            "  color: #555;",
            "  border-left: 3px solid #999;",
            "}",
            ".cover-page {",
            "  display: flex;",
            "  align-items: center;",
            "  justify-content: center;",
            "  min-height: 100vh;",
            "}",
            ".cover-wrapper img {",
            "  max-width: 100%;",
            "  height: auto;",
            "  display: block;",
            "  margin: 0 auto;",
            "}",
        ]
    )


def build_content_opf() -> str:
    utc_now = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    now = utc_now.isoformat().replace("+00:00", "Z")
    return "\n".join(
        [
            '<?xml version="1.0" encoding="utf-8"?>',
            '<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid">',
            "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">",
            f"    <dc:identifier id=\"bookid\">urn:uuid:{int(utc_now.timestamp())}</dc:identifier>",
            f"    <dc:title>{html.escape(TITLE)}</dc:title>",
            "    <dc:language>zh-CN</dc:language>",
            f"    <meta property=\"dcterms:modified\">{now}</meta>",
            '    <meta name="cover" content="cover-image"/>',
            "  </metadata>",
            "  <manifest>",
            '    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>',
            '    <item id="style" href="style.css" media-type="text/css"/>',
            '    <item id="content" href="content.xhtml" media-type="application/xhtml+xml"/>',
            '    <item id="cover" href="cover.xhtml" media-type="application/xhtml+xml"/>',
            '    <item id="cover-image" href="cover.png" media-type="image/png" properties="cover-image"/>',
            "  </manifest>",
            "  <spine>",
            '    <itemref idref="cover"/>',
            '    <itemref idref="content"/>',
            "  </spine>",
            "</package>",
        ]
    )


def build_container_xml() -> str:
    return "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8"?>',
            '<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">',
            "  <rootfiles>",
            '    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>',
            "  </rootfiles>",
            "</container>",
        ]
    )


def write_epub(sections: Sequence[Tuple[str, Sequence[str]]]) -> None:
    if not COVER_IMAGE.exists():
        raise FileNotFoundError(f"封面文件不存在: {COVER_IMAGE}")
    with tempfile.TemporaryDirectory() as tmpdir:
        tmp = Path(tmpdir)
        meta_inf = tmp / "META-INF"
        oebps = tmp / "OEBPS"
        meta_inf.mkdir(parents=True, exist_ok=True)
        oebps.mkdir(parents=True, exist_ok=True)

        # Core files
        (tmp / "mimetype").write_text("application/epub+zip", encoding="utf-8")
        (meta_inf / "container.xml").write_text(build_container_xml(), encoding="utf-8")
        (oebps / "content.opf").write_text(build_content_opf(), encoding="utf-8")
        (oebps / "nav.xhtml").write_text(build_nav_xhtml(sections), encoding="utf-8")
        (oebps / "content.xhtml").write_text(build_content_xhtml(sections), encoding="utf-8")
        (oebps / "cover.xhtml").write_text(build_cover_xhtml(), encoding="utf-8")
        (oebps / "style.css").write_text(build_style_css(), encoding="utf-8")
        cover_target = oebps / "cover.png"
        cover_target.write_bytes(COVER_IMAGE.read_bytes())

        # Package into EPUB with correct mimetype ordering.
        with zipfile.ZipFile(OUTPUT_EPUB, "w") as zf:
            zf.writestr("mimetype", "application/epub+zip", compress_type=zipfile.ZIP_STORED)
            for file_path in sorted(meta_inf.rglob("*")):
                arcname = file_path.relative_to(tmp).as_posix()
                zf.write(file_path, arcname, compress_type=zipfile.ZIP_DEFLATED)
            for file_path in sorted(oebps.rglob("*")):
                arcname = file_path.relative_to(tmp).as_posix()
                zf.write(file_path, arcname, compress_type=zipfile.ZIP_DEFLATED)
    print(f"生成完成：{OUTPUT_EPUB}")


def main() -> None:
    if not INPUT_MD.exists():
        raise FileNotFoundError(f"未找到源文件: {INPUT_MD}")
    text = INPUT_MD.read_text(encoding="utf-8")
    sections = parse_markdown(text)
    write_epub(sections)


if __name__ == "__main__":
    main()
