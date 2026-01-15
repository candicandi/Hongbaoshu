#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 Markdown 原文（一级标题为章节，【】包裹为注释）转换为技术方案约定的 JSON 结构。

用法：
    python scripts/md_to_json.py --input scripts/原文.md --output scripts/text.json \
        --title "毛主席语录" --author "毛泽东" --edition "1966"

约定：
- 一级标题（# 开头）表示新章节，按出现顺序生成 id（默认 2 位前导零）。
- 段落：连续非空行合并为一个自然段。
- 注释：内容被全角方括号【】包裹的段落标记为 annotation，其他为 text。
"""

import argparse
import json
import re
from pathlib import Path
from typing import Dict, List, Optional


def parse_markdown(
    path: Path, id_width: int = 2, para_width: int = 3, sentence_width: int = 3
) -> Dict[str, List[Dict[str, object]]]:
    chapters: List[Dict[str, object]] = []
    current: Optional[Dict[str, object]] = None
    paragraph_lines: List[str] = []
    para_index: int = 0

    annotation_pattern = re.compile(r"【([^】]+)】")
    sentence_delimiters = {"，", "。"}

    def split_sentences(text: str, para_id: str) -> List[Dict[str, str]]:
        sentences: List[Dict[str, str]] = []
        buf = []
        sentence_idx = 0
        for ch in text:
            buf.append(ch)
            if ch in sentence_delimiters:
                sentence_idx += 1
                sentences.append(
                    {
                        "id": f"{para_id}-s{str(sentence_idx).zfill(sentence_width)}",
                        "content": "".join(buf).strip(),
                    }
                )
                buf = []
        tail = "".join(buf).strip()
        if tail:
            sentence_idx += 1
            sentences.append(
                {
                    "id": f"{para_id}-s{str(sentence_idx).zfill(sentence_width)}",
                    "content": tail,
                }
            )
        return sentences

    def append_paragraph(
        target: Dict[str, object],
        para_id: str,
        ptype: str,
        content: str,
        ref: Optional[str] = None,
    ) -> None:
        entry: Dict[str, str] = {"id": para_id, "type": ptype, "content": content}
        if ref:
            entry["ref"] = ref
        if ptype == "text":
            entry["sentences"] = split_sentences(content, para_id)
        target.setdefault("paragraphs", []).append(entry)

    def next_para_id(target: Dict[str, object]) -> str:
        nonlocal para_index
        para_index += 1
        return f"{target['id']}-{str(para_index).zfill(para_width)}"

    def flush_paragraph(target: Optional[Dict[str, object]]) -> None:
        nonlocal paragraph_lines
        if target is None:
            paragraph_lines = []
            return
        text = "\n".join(paragraph_lines).strip()
        paragraph_lines = []
        if not text:
            return
        last_end = 0
        emitted_text_in_block = False
        for match in annotation_pattern.finditer(text):
            pre = text[last_end : match.start()].strip()
            if pre:
                append_paragraph(target, next_para_id(target), "text", pre)
                emitted_text_in_block = True
            ann = match.group(1).strip()
            if ann:
                append_paragraph(
                    target,
                    next_para_id(target),
                    "annotation",
                    ann,
                    ref="prev" if emitted_text_in_block else None,
                )
            last_end = match.end()
        tail = text[last_end:].strip()
        if tail:
            append_paragraph(target, next_para_id(target), "text", tail)

    heading_pattern = re.compile(r"^#\s+(.*)$")
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            stripped = line.rstrip("\n")
            heading_match = heading_pattern.match(stripped)
            if heading_match:
                flush_paragraph(current)
                chapter_id = str(len(chapters) + 1).zfill(id_width)
                para_index = 0
                current = {
                    "id": chapter_id,
                    "title": heading_match.group(1).strip(),
                    "paragraphs": [],
                }
                chapters.append(current)
                continue

            if stripped.strip() == "":
                flush_paragraph(current)
            else:
                paragraph_lines.append(stripped)

    flush_paragraph(current)
    return {"chapters": chapters}


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert Markdown to book JSON.")
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("scripts/原文.md"),
        help="Markdown source file",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("scripts/text.json"),
        help="JSON output path",
    )
    parser.add_argument("--title", default="毛主席语录", help="Book title")
    parser.add_argument("--author", default="毛泽东", help="Author name")
    parser.add_argument("--edition", default="1966", help="Edition or version")
    parser.add_argument(
        "--id-width", type=int, default=2, help="Zero-pad width for chapter ids"
    )
    parser.add_argument(
        "--para-width", type=int, default=3, help="Zero-pad width for paragraph ids"
    )
    parser.add_argument(
        "--sentence-width",
        type=int,
        default=3,
        help="Zero-pad width for sentence ids",
    )
    args = parser.parse_args()

    parsed = parse_markdown(
        args.input,
        id_width=args.id_width,
        para_width=args.para_width,
        sentence_width=args.sentence_width,
    )
    data = {
        "book": {
            "title": args.title,
            "author": args.author,
            "edition": args.edition,
        },
        "chapters": parsed["chapters"],
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"Wrote {len(data['chapters'])} chapters to {args.output}")


if __name__ == "__main__":
    main()
