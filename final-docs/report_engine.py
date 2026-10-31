"""
Exact SRMIST-DDE Reference Format Report Engine
Matches 'example docs/DC2332306010017_SECURE_DATA_SHARING_USING_RSA (2).pdf'
in page geometry, pure #000000 Times New Roman 14pt typography, 24pt leading,
front-matter order (Title Page -> Bonafide Certificate -> Acknowledgement ->
Dotted-Leader Table of Contents -> Abstract -> Chapters 1..6), dedicated diagram
pages, 2-per-page live screenshots, plain unboxed source code listings, and
IEEE numbered references. Outputs synchronized PDF and DOCX files.
"""
import os
import html
from PIL import Image as PILImage

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import inch
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT, TA_JUSTIFY
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle,
    Image as RLImage, PageBreak, KeepTogether, Flowable
)

from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls


def _register_times_fonts():
    dejavu_reg = "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"
    dejavu_bld = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
    if os.path.exists(dejavu_reg):
        pdfmetrics.registerFont(TTFont("SymSerif", dejavu_reg))
    if os.path.exists(dejavu_bld):
        pdfmetrics.registerFont(TTFont("SymSerif-Bold", dejavu_bld))


_register_times_fonts()


class BookmarkTrackerFlowable(Flowable):
    """Zero-height flowable recording exact 1-based physical PDF page number."""
    def __init__(self, key, tracker):
        super().__init__()
        self.key = key
        self.tracker = tracker
        self.width = 0
        self.height = 0

    def wrap(self, availWidth, availHeight):
        return (0, 0)

    def draw(self):
        self.tracker[self.key] = self.canv.getPageNumber()


class DottedTocLineFlowable(Flowable):
    """
    Renders an exact SRMIST-style Table of Contents line in 14pt Times New Roman:
      - Chapter banner line (page_str is None): '1. CHAPTER 1 –'
      - Section line with dotted leader: '1.0 INTRODUCTION................................6'
    """
    def __init__(self, label, page_str, width, indent=0.0, font_name="Times-Roman", font_size=13.5, line_height=26.0):
        super().__init__()
        self.label = label
        self.page_str = page_str
        self.width = width
        self.indent = indent
        self.font_name = font_name
        self.font_size = font_size
        self.height = line_height

    def wrap(self, availWidth, availHeight):
        self.width = availWidth
        return (availWidth, self.height)

    def draw(self):
        c = self.canv
        c.saveState()
        c.setFillColor(colors.black)
        c.setFont(self.font_name, self.font_size)
        y = 6.0

        c.drawString(self.indent, y, self.label)

        if self.page_str is not None and str(self.page_str) != "":
            pg_s = str(self.page_str)
            c.drawRightString(self.width, y, pg_s)

            lbl_w = c.stringWidth(self.label, self.font_name, self.font_size)
            pg_w = c.stringWidth(pg_s, self.font_name, self.font_size)
            dot_w = c.stringWidth(".", self.font_name, self.font_size)
            start_x = self.indent + lbl_w + 3.0
            end_x = self.width - pg_w - 3.0
            if end_x > start_x + dot_w * 2:
                n_dots = int((end_x - start_x) / dot_w)
                dots = "." * n_dots
                c.drawRightString(end_x, y, dots)
        c.restoreState()


def _docx_cell_borders(cell, color="000000", sz="4", val="single"):
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:left w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:right w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)


def _docx_cell_margins(cell, top=80, bottom=80, left=110, right=110):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement("w:tcMar")
    for m, val in [("top", top), ("bottom", bottom), ("left", left), ("right", right)]:
        node = OxmlElement(f"w:{m}")
        node.set(qn("w:w"), str(val))
        node.set(qn("w:type"), "dxa")
        tcMar.append(node)
    tcPr.append(tcMar)


class AcademicReportBuilder:
    """
    Synchronized PDF + DOCX builder matching the exact visual & structural format of
    'example docs/DC2332306010017_SECURE_DATA_SHARING_USING_RSA (2).pdf'.
    """
    def __init__(
        self,
        project_title,
        abstract_heading_title,
        student_name,
        reg_no,
        degree_title="BACHELOR OF COMPUTER APPLICATIONS",
        guide_upper="Dr. A. THIRUMURTHI RAJA M.C.A., M.E., PH.D.,",
        guide_mixed="Dr.A.Thirumurthi Raja",
        month_year="NOV 2026",
        srm_logo_path=None,
    ):
        self.project_title = project_title
        self.abstract_heading_title = abstract_heading_title
        self.student_name = student_name
        self.reg_no = reg_no
        self.degree_title = degree_title
        self.guide_upper = guide_upper
        self.guide_mixed = guide_mixed
        self.month_year = month_year
        self.srm_logo_path = srm_logo_path

        self._seq = 0
        self.toc_entries = []      # list of dict: {kind: 'chap'|'sec', label, key, indent, show_page}
        self.abstract_paras = []   # list of strings
        self.body_blocks = []      # ordered content blocks for Chapters 1..6

    def _next_key(self, prefix="k"):
        self._seq += 1
        return f"{prefix}_{self._seq:04d}"

    # ------------------------------------------------------------------
    # Authoring API matching DC2332306010017_SECURE_DATA_SHARING_USING_RSA
    # ------------------------------------------------------------------
    def add_abstract_paragraph(self, text):
        self.abstract_paras.append(text)

    def start_chapter(self, chap_num, subtitle=None):
        """
        Adds '1. CHAPTER 1 –' to TOC and starts a new page with centered 'CHAPTER N'
        (plus optional centered subtitle like 'MODULES' in Chapter 3).
        """
        self.toc_entries.append({
            "kind": "chap",
            "label": f"{chap_num}. CHAPTER {chap_num} –",
            "key": None,
            "indent": 0.0,
            "show_page": False,
        })
        self.body_blocks.append(("chapter", chap_num, subtitle))

    def toc_only_entry(self, toc_label, key=None, indent=0.0, show_page=True):
        self.toc_entries.append({
            "kind": "sec",
            "label": toc_label,
            "key": key,
            "indent": indent,
            "show_page": show_page,
        })

    def section(self, num_str, title_upper, toc_label=None, toc_indent=0.0, show_page_in_toc=True,
                new_page=False, trailing_colon=True, render_heading=True):
        """
        Adds a numbered section (e.g., '1.0 INTRODUCTION:') and registers it in TOC.
        """
        key = self._next_key("sec")
        t_lbl = toc_label if toc_label is not None else f"{num_str} {title_upper}".strip()
        self.toc_entries.append({
            "kind": "sec",
            "label": t_lbl,
            "key": key,
            "indent": toc_indent,
            "show_page": show_page_in_toc,
        })
        disp = f"{num_str} {title_upper}".strip()
        if trailing_colon and not disp.endswith(":"):
            disp += ":"
        self.body_blocks.append(("section", key, disp, new_page, render_heading))
        return key

    def subheading(self, text, space_before=12, space_after=8):
        """Unnumbered or minor bold 14pt heading inside a section (not added to TOC)."""
        self.body_blocks.append(("subheading", text, space_before, space_after))

    def p(self, text, bold_prefix=None, first_indent=0, space_after=10):
        self.body_blocks.append(("p", text, bold_prefix, first_indent, space_after))

    def bullet(self, first, second=None, bold_prefix=None):
        """Square bullet '▪' matching ref_011.png & ref_027.png."""
        if bold_prefix is not None:
            bp, text = bold_prefix, first
        elif second is not None:
            bp, text = first, second
        else:
            bp, text = None, first
        self.body_blocks.append(("bullet", text, bp))

    def numbered_list(self, items):
        """Numbered module list '1. ...' matching ref_023.png."""
        self.body_blocks.append(("numlist", list(items)))

    def colon_spec_list(self, pairs):
        """
        Exact '▪ Label : Value' aligned specification block matching Page 11 (ref_011.png)
        for 1.3.1 HARDWARE REQUIREMENTS and 1.3.2 SOFTWARE REQUIREMENTS.
        """
        self.body_blocks.append(("colon_specs", list(pairs)))

    def diagram_page(self, num_str, heading_text, img_path, toc_label=None, toc_indent=18.0,
                     width_in=6.25, max_height_in=7.6, include_in_toc=True):
        """
        Dedicated diagram page matching Pages 12-17 (ref_012.png, ref_013.png):
        Starts on a new page, renders bold heading top-left, centers diagram below.
        """
        key = self._next_key("diag")
        if include_in_toc:
            t_lbl = toc_label if toc_label is not None else f"{num_str} {heading_text.rstrip(':').upper()}".strip()
            self.toc_entries.append({
                "kind": "sec",
                "label": t_lbl,
                "key": key,
                "indent": toc_indent,
                "show_page": True,
            })
        disp = f"{num_str} {heading_text}".strip() if num_str else heading_text
        self.body_blocks.append(("diagram_page", key, disp, img_path, width_in, max_height_in))

    def literature_item(self, title, year, author, abstract_text):
        """
        Exact Literature Survey entry matching Pages 18-22 (ref_018.png):
        TITLE: ... / YEAR OF PUBLISHING: ... / AUTHOR NAME: ... / ABSTRACT: ...
        """
        self.body_blocks.append(("lit_item", title, str(year), author, abstract_text))

    def table(self, title, headers, rows, col_widths=None):
        """Clean black-bordered academic table in pure Times New Roman #000000."""
        self.body_blocks.append(("table", title, headers, rows, col_widths))

    def screenshots_gallery(self, num_str, title_upper, screenshot_items):
        """
        Matches Section 6.2 SCREENSHOTS (Pages 60-63, ref_061.png):
        Starts on a new page, renders '6.2 SCREENSHOTS:' at top-left, and stacks
        2 crisp authentic application screenshots per page across consecutive pages.
        screenshot_items: list of (caption_str, img_path)
        """
        key = self._next_key("shots")
        self.toc_entries.append({
            "kind": "sec",
            "label": f"{num_str} {title_upper}".strip(),
            "key": key,
            "indent": 0.0,
            "show_page": True,
        })
        self.body_blocks.append(("screenshots", key, f"{num_str} {title_upper}:", list(screenshot_items)))

    def code_file(self, filename_header, code_text):
        """
        Matches Section 6.3 CODING (Pages 64-80, ref_075.png):
        Plain black Times New Roman filename header ('SecurityConfig.java:') followed
        by clean unboxed left-aligned code lines flowing naturally across pages.
        """
        wrapped = []
        for raw in code_text.strip("\n").splitlines():
            raw_clean = raw.replace("\t", "    ")
            if len(raw_clean) <= 84:
                wrapped.append(raw_clean)
            else:
                lead = len(raw_clean) - len(raw_clean.lstrip(" "))
                cont = " " * min(lead + 4, 16)
                rem = raw_clean
                first = True
                while len(rem) > 84:
                    cut = rem.rfind(" ", lead + 8 if first else len(cont) + 8, 85)
                    if cut <= 0:
                        cut = 84
                    wrapped.append(rem[:cut].rstrip())
                    rem = cont + rem[cut:].lstrip()
                    first = False
                if rem.strip():
                    wrapped.append(rem)
        self.body_blocks.append(("code_file", filename_header, wrapped))

    def page_break(self):
        self.body_blocks.append(("page_break",))

    # ==================================================================
    # PDF RENDERER (Two-Pass Exact Physical Page Numbers)
    # ==================================================================
    def _build_pdf_once(self, pdf_path, page_map):
        tracker = {}
        W, H = A4  # 595.27 x 841.89 pt
        left_m = 1.00 * inch     # 72.0 pt (exact match to ref PDF x=72.0)
        right_m = 0.78 * inch    # 56.16 pt -> x_right = 539.1 pt (exact match to ref PDF x=540..543)
        top_m = 1.00 * inch      # 72.0 pt (exact match to ref PDF y=72.7)
        bot_m = 0.95 * inch
        usable_w = W - left_m - right_m  # ~467.1 pt = 6.49 in

        frame = Frame(
            left_m, bot_m, usable_w, H - top_m - bot_m,
            id="main_frame", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0
        )
        doc = BaseDocTemplate(
            pdf_path, pagesize=A4,
            leftMargin=left_m, rightMargin=right_m, topMargin=top_m, bottomMargin=bot_m,
            title=self.project_title, author=self.student_name
        )
        # Plain white pages — NO borders, NO headers, NO footers (exact match to ref PDF!)
        doc.addPageTemplates([PageTemplate(id="PlainSRM", frames=[frame])])

        # Exact Typography Styles from PyMuPDF inspection of DC2332306010017 PDF
        s_body14 = ParagraphStyle(
            "Body14", fontName="Times-Roman", fontSize=14, leading=24,
            alignment=TA_JUSTIFY, spaceAfter=10, textColor=colors.black
        )
        s_body14_indent = ParagraphStyle(
            "Body14Ind", parent=s_body14, firstLineIndent=36
        )
        s_chap = ParagraphStyle(
            "Chap", fontName="Times-Bold", fontSize=14, leading=20,
            alignment=TA_CENTER, spaceBefore=0, spaceAfter=18, textColor=colors.black
        )
        s_sec = ParagraphStyle(
            "Sec", fontName="Times-Bold", fontSize=14, leading=20,
            alignment=TA_LEFT, spaceBefore=12, spaceAfter=12, keepWithNext=True, textColor=colors.black
        )
        s_sub = ParagraphStyle(
            "Sub", fontName="Times-Bold", fontSize=14, leading=20,
            alignment=TA_LEFT, spaceBefore=12, spaceAfter=8, keepWithNext=True, textColor=colors.black
        )
        s_bullet = ParagraphStyle(
            "SqBullet", fontName="Times-Roman", fontSize=14, leading=24,
            alignment=TA_JUSTIFY, leftIndent=32, firstLineIndent=-18, spaceAfter=6, textColor=colors.black
        )
        s_numitem = ParagraphStyle(
            "NumItem", fontName="Times-Roman", fontSize=14, leading=24,
            alignment=TA_LEFT, leftIndent=36, firstLineIndent=-18, spaceAfter=4, textColor=colors.black
        )
        s_lit_lbl = ParagraphStyle(
            "LitLbl", fontName="Times-Roman", fontSize=14, leading=22,
            alignment=TA_LEFT, spaceAfter=12, textColor=colors.black
        )
        s_code_hdr = ParagraphStyle(
            "CodeHdr", fontName="Times-Bold", fontSize=13.5, leading=20,
            alignment=TA_LEFT, spaceBefore=14, spaceAfter=10, keepWithNext=True, textColor=colors.black
        )
        s_code_ln = ParagraphStyle(
            "CodeLn", fontName="Times-Roman", fontSize=11.5, leading=18.5,
            alignment=TA_LEFT, spaceAfter=2, textColor=colors.black
        )
        s_shot_cap = ParagraphStyle(
            "ShotCap", fontName="Times-Bold", fontSize=12, leading=16,
            alignment=TA_CENTER, spaceBefore=4, spaceAfter=14, textColor=colors.black
        )
        s_th = ParagraphStyle(
            "TH", fontName="Times-Bold", fontSize=11.0, leading=14.5,
            alignment=TA_LEFT, textColor=colors.black
        )
        s_td = ParagraphStyle(
            "TD", fontName="Times-Roman", fontSize=10.5, leading=14.0,
            alignment=TA_LEFT, textColor=colors.black
        )

        story = []

        # ==============================================================
        # PAGE 1: TITLE PAGE (Exact match to ref_001.png)
        # ==============================================================
        story.append(Spacer(1, 6))
        story.append(Paragraph(
            html.escape(self.project_title),
            ParagraphStyle("TP_Title", fontName="Times-Bold", fontSize=16, leading=22, alignment=TA_CENTER, spaceAfter=18)
        ))
        story.append(Paragraph(
            "A PROJECT REPORT SUBMITTED TO",
            ParagraphStyle("TP_1", fontName="Times-Roman", fontSize=14, leading=19, alignment=TA_CENTER, spaceAfter=14)
        ))
        story.append(Paragraph(
            "SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
            ParagraphStyle("TP_2", fontName="Times-Roman", fontSize=14, leading=19, alignment=TA_CENTER, spaceAfter=14)
        ))
        story.append(Paragraph(
            "IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE<br/>AWARD OF THE DEGREE OF",
            ParagraphStyle("TP_3", fontName="Times-Roman", fontSize=14, leading=22, alignment=TA_CENTER, spaceAfter=16)
        ))
        story.append(Paragraph(
            html.escape(self.degree_title),
            ParagraphStyle("TP_Deg", fontName="Times-Bold", fontSize=16, leading=21, alignment=TA_CENTER, spaceAfter=16)
        ))
        story.append(Paragraph(
            "BY",
            ParagraphStyle("TP_By", fontName="Times-Bold", fontSize=16, leading=20, alignment=TA_CENTER, spaceAfter=16)
        ))
        story.append(Paragraph(
            html.escape(self.student_name),
            ParagraphStyle("TP_Stu", fontName="Times-Roman", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=12)
        ))
        story.append(Paragraph(
            f"REG. NO. {html.escape(self.reg_no)}",
            ParagraphStyle("TP_Reg", fontName="Times-Roman", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=16)
        ))
        story.append(Paragraph(
            "UNDER THE GUIDANCE OF",
            ParagraphStyle("TP_UG", fontName="Times-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=12)
        ))
        story.append(Paragraph(
            html.escape(self.guide_upper),
            ParagraphStyle("TP_Gn", fontName="Times-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=12)
        ))
        story.append(Paragraph(
            "ASSISTANT PROFESSOR,",
            ParagraphStyle("TP_Gt", fontName="Times-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=12)
        ))
        if self.srm_logo_path and os.path.exists(self.srm_logo_path):
            im = PILImage.open(self.srm_logo_path)
            iw, ih = im.size
            lw = 2.62 * inch
            lh = lw * (ih / iw)
            logo = RLImage(self.srm_logo_path, width=lw, height=lh)
            logo.hAlign = "CENTER"
            story.append(logo)
            story.append(Spacer(1, 14))
        for footer_ln in [
            "DEPARTMENT OF COMPUTER APPLICATIONS",
            "DIRECTORATE OF DISTANCE EDUCATION",
            "SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
            "(Deemed to be university u/s 3 of UGC Act, 1956)",
            "Kattankulathur – 603 203",
            "Chennai, Tamilnadu",
            self.month_year,
        ]:
            story.append(Paragraph(
                html.escape(footer_ln),
                ParagraphStyle("TP_Bot", fontName="Times-Bold", fontSize=12, leading=18.5, alignment=TA_CENTER, spaceAfter=2)
            ))
        story.append(PageBreak())

        # ==============================================================
        # PAGE 2: BONAFIDE CERTIFICATE (Exact match to ref_002.png)
        # ==============================================================
        story.append(Spacer(1, 8))
        story.append(Paragraph(
            "BONAFIDE CERTIFICATE",
            ParagraphStyle("BC_H", fontName="Times-Bold", fontSize=14, leading=20, alignment=TA_CENTER, spaceAfter=26)
        ))
        bon_body = (
            f"This is to certify that the project report titled "
            f"<b>“{html.escape(self.project_title)}”</b> is a bonafide work carried out by "
            f"<b>{html.escape(self.student_name)}</b> (<b>{html.escape(self.reg_no)}</b>) under my "
            f"supervision for the award of the Degree of Bachelor of Computer Applications. "
            f"To my knowledge the work reported herein is the original work done by the candidate."
        )
        story.append(Paragraph(
            bon_body,
            ParagraphStyle("BC_P", fontName="Times-Roman", fontSize=12, leading=21, alignment=TA_JUSTIFY, firstLineIndent=28, spaceAfter=64)
        ))
        sig_l = ParagraphStyle("BC_SL", fontName="Times-Roman", fontSize=12, leading=22, alignment=TA_LEFT)
        sig_r = ParagraphStyle("BC_SR", fontName="Times-Roman", fontSize=12, leading=22, alignment=TA_CENTER)
        bon_tbl = Table([
            [
                Paragraph(
                    f"<b>PROJECT GUIDE</b><br/><b>{html.escape(self.guide_mixed)}</b><br/>"
                    f"Assistant Professor,<br/>SRMIST-DDE,",
                    sig_l
                ),
                Paragraph(
                    f"<b>HEAD OF DEPARTMENT</b><br/><b>{html.escape(self.guide_mixed)}</b><br/>"
                    f"Program Coordinator<br/>Department of Computer Applications,<br/>"
                    f"Directorate of Distance Education (DDE)<br/><b>SRMIST,</b>",
                    sig_r
                ),
            ],
            [Spacer(1, 56), Spacer(1, 56)],
            [
                Paragraph("<b>INTERNAL EXAMINER</b>", sig_l),
                Paragraph("<b>EXTERNAL EXAMINER</b>", sig_r),
            ],
        ], colWidths=[usable_w * 0.46, usable_w * 0.54])
        bon_tbl.setStyle(TableStyle([
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 0),
            ("RIGHTPADDING", (0, 0), (-1, -1), 0),
        ]))
        story.append(bon_tbl)
        story.append(PageBreak())

        # ==============================================================
        # PAGE 3: ACKNOWLEDGEMENT (Exact match to ref_003.png)
        # ==============================================================
        story.append(Spacer(1, 8))
        story.append(Paragraph(
            "ACKNOWLEDGEMENT",
            ParagraphStyle("ACK_H", fontName="Times-Bold", fontSize=14, leading=20, alignment=TA_CENTER, spaceAfter=20)
        ))
        ack_style = ParagraphStyle(
            "ACK_P", fontName="Times-Roman", fontSize=12, leading=21,
            alignment=TA_JUSTIFY, firstLineIndent=32, spaceAfter=14
        )
        ack_noind = ParagraphStyle("ACK_P0", parent=ack_style, firstLineIndent=0)
        story.append(Paragraph(
            "With profound gratitude to the ALMIGHTY, I take this chance to thank the people who "
            "helped me to complete this project.",
            ack_style
        ))
        story.append(Paragraph(
            "I take this as a right opportunity to say THANKS to my parents who are there to stand "
            "with me always with the words “ANYTHING IS ACHIEVABLE WITH HARDWORK AND PERSISTENCE”.",
            ack_noind
        ))
        story.append(Paragraph(
            "I am thankful to <b>Dr.T.R. Paarivendhar, Chancellor</b>, SRM Institute of Science &amp; "
            "Technology who gave us the platform to establish ourselves and reach greater heights.",
            ack_style
        ))
        story.append(Paragraph(
            "I express my deep sense of gratitude to the Director, Directorate of Distance Education, "
            "SRM Institute of Science &amp; Technology for their wholehearted support and encouragement.",
            ack_style
        ))
        story.append(Paragraph(
            f"With gratitude I like to thank my project guide <b>{html.escape(self.guide_mixed)}</b>, "
            "Assistant Professor, Department of Computer Applications (DDE), for his help, support, "
            "encouragement, suggestions, and guidance throughout the development phases of the project.",
            ack_style
        ))
        story.append(Paragraph(
            "I take this opportunity to extend my sincere gratitude to all the faculty members of the "
            "department who provided their support through valuable comments and suggestions during the reviews.",
            ack_style
        ))
        story.append(Paragraph(
            "A great note of gratitude to friends and colleagues who helped in carrying out this "
            "project work to a successful completion.",
            ack_style
        ))
        story.append(Spacer(1, 36))
        story.append(Paragraph(
            f"<b>{html.escape(self.student_name.title())}</b>",
            ParagraphStyle("ACK_Sig", fontName="Times-Bold", fontSize=12, leading=18, alignment=TA_RIGHT)
        ))
        story.append(PageBreak())

        # ==============================================================
        # PAGES 4+: TABLE OF CONTENTS (Exact match to ref_004.png & ref_005.png)
        # ==============================================================
        story.append(Paragraph(
            "TABLE OF CONTENTS",
            ParagraphStyle("TOC_H", fontName="Times-Roman", fontSize=14, leading=20, alignment=TA_CENTER, spaceAfter=18)
        ))
        for item in self.toc_entries:
            pg = page_map.get(item["key"], "7") if (item["show_page"] and item["key"]) else None
            story.append(DottedTocLineFlowable(
                label=item["label"],
                page_str=pg,
                width=usable_w,
                indent=item["indent"],
                font_name="Times-Roman",
                font_size=13.5,
                line_height=26.5,
            ))
        story.append(PageBreak())

        # ==============================================================
        # ABSTRACT PAGE (Exact match to ref_006.png — right after TOC!)
        # ==============================================================
        story.append(Paragraph(
            html.escape(self.abstract_heading_title),
            ParagraphStyle("ABS_Title", fontName="Times-Bold", fontSize=14, leading=20, alignment=TA_CENTER, spaceAfter=18)
        ))
        story.append(Paragraph(
            "Abstract:",
            ParagraphStyle("ABS_Lbl", fontName="Times-Bold", fontSize=14, leading=20, alignment=TA_LEFT, spaceAfter=12)
        ))
        for ap in self.abstract_paras:
            story.append(Paragraph(html.escape(ap), s_body14_indent))

        # ==============================================================
        # CHAPTERS 1..6 BODY (Exact match to ref_007.png .. ref_081.png)
        # ==============================================================
        def make_clean_bw_table(headers, rows, col_widths):
            if col_widths:
                tot = sum(col_widths)
                cw = [(w / tot) * usable_w for w in col_widths]
            else:
                cw = [usable_w / len(headers)] * len(headers)
            data = [[Paragraph(html.escape(str(h)), s_th) for h in headers]]
            for r in rows:
                data.append([Paragraph(html.escape(str(v)).replace("\n", "<br/>"), s_td) for v in r])
            t = Table(data, colWidths=cw, repeatRows=1)
            t.setStyle(TableStyle([
                ("GRID", (0, 0), (-1, -1), 0.6, colors.black),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]))
            return t

        for blk in self.body_blocks:
            btype = blk[0]

            if btype == "chapter":
                _, chap_num, subtitle = blk
                story.append(PageBreak())
                chap_txt = f"CHAPTER {chap_num}" if chap_num != 6 else "CHAPTER 6"
                story.append(Paragraph(chap_txt, s_chap))
                if subtitle:
                    story.append(Paragraph(html.escape(subtitle), s_chap))

            elif btype == "section":
                _, key, disp, new_page, render_heading = blk
                if new_page:
                    story.append(PageBreak())
                story.append(BookmarkTrackerFlowable(key, tracker))
                if render_heading:
                    story.append(Paragraph(html.escape(disp), s_sec))

            elif btype == "subheading":
                _, text, sb, sa = blk
                st = ParagraphStyle("SubCustom", parent=s_sub, spaceBefore=sb, spaceAfter=sa)
                story.append(Paragraph(html.escape(text), st))

            elif btype == "p":
                _, text, bp, first_ind, sa = blk
                st = s_body14 if (first_ind == 0 and sa == 10) else ParagraphStyle(
                    "BodyCustom", parent=s_body14, firstLineIndent=first_ind, spaceAfter=sa
                )
                full = f"<b>{html.escape(bp)}</b> {html.escape(text)}" if bp else html.escape(text)
                story.append(Paragraph(full, st))

            elif btype == "bullet":
                _, text, bp = blk
                full = f'<font name="SymSerif" size="11">▪</font>  <b>{html.escape(bp)}</b> {html.escape(text)}' if bp else f'<font name="SymSerif" size="11">▪</font>  {html.escape(text)}'
                story.append(Paragraph(full, s_bullet))

            elif btype == "numlist":
                _, items = blk
                for idx, it in enumerate(items, 1):
                    story.append(Paragraph(f"{idx}.  {html.escape(it)}", s_numitem))
                story.append(Spacer(1, 8))

            elif btype == "colon_specs":
                _, pairs = blk
                rows_sp = []
                st_sp = ParagraphStyle("SpecP", fontName="Times-Roman", fontSize=14, leading=19, textColor=colors.black)
                for lbl, val in pairs:
                    rows_sp.append([
                        Paragraph(f'<font name="SymSerif" size="11">▪</font>   {html.escape(lbl)}', st_sp),
                        Paragraph(":", st_sp),
                        Paragraph(html.escape(val), st_sp),
                    ])
                t_sp = Table(rows_sp, colWidths=[usable_w * 0.36, usable_w * 0.04, usable_w * 0.60])
                t_sp.setStyle(TableStyle([
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("LEFTPADDING", (0, 0), (-1, -1), 12),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 2),
                    ("TOPPADDING", (0, 0), (-1, -1), 1),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 1),
                ]))
                story.append(t_sp)
                story.append(Spacer(1, 10))

            elif btype == "diagram_inline":
                _, img_path, width_in, max_h_in = blk
                if os.path.exists(img_path):
                    im = PILImage.open(img_path)
                    iw, ih = im.size
                    w_pt = min(width_in * inch, usable_w)
                    h_pt = w_pt * (ih / iw)
                    max_h = max_h_in * inch
                    if h_pt > max_h:
                        h_pt = max_h
                        w_pt = h_pt * (iw / ih)
                    rl_im = RLImage(img_path, width=w_pt, height=h_pt)
                    rl_im.hAlign = "CENTER"
                    story.append(rl_im)

            elif btype == "diagram_page":
                _, key, disp, img_path, width_in, max_h_in = blk
                story.append(PageBreak())
                story.append(BookmarkTrackerFlowable(key, tracker))
                story.append(Paragraph(html.escape(disp), s_sec))
                story.append(Spacer(1, 14))
                if os.path.exists(img_path):
                    im = PILImage.open(img_path)
                    iw, ih = im.size
                    w_pt = min(width_in * inch, usable_w)
                    h_pt = w_pt * (ih / iw)
                    max_h = max_h_in * inch
                    if h_pt > max_h:
                        h_pt = max_h
                        w_pt = h_pt * (iw / ih)
                    rl_im = RLImage(img_path, width=w_pt, height=h_pt)
                    rl_im.hAlign = "CENTER"
                    story.append(rl_im)

            elif btype == "lit_item":
                _, title, year, author, abs_txt = blk
                story.append(Paragraph(f"<b>TITLE:</b> {html.escape(title)}", s_lit_lbl))
                story.append(Paragraph(f"<b>YEAR OF PUBLISHING:</b> {html.escape(year)}", s_lit_lbl))
                story.append(Paragraph(f"<b>AUTHOR NAME:</b> {html.escape(author)}", s_lit_lbl))
                story.append(Paragraph(f"<b>ABSTRACT:</b>   {html.escape(abs_txt)}", s_body14))
                story.append(Spacer(1, 10))

            elif btype == "table":
                _, title, headers, rows, cw = blk
                if title:
                    story.append(Paragraph(html.escape(title), s_sub))
                story.append(make_clean_bw_table(headers, rows, cw))
                story.append(Spacer(1, 12))

            elif btype == "screenshots":
                _, key, heading_disp, items = blk
                story.append(PageBreak())
                story.append(BookmarkTrackerFlowable(key, tracker))
                story.append(Paragraph(html.escape(heading_disp), s_sec))
                # Render 2 crisp authentic screenshots per page (matching ref_061.png!)
                for idx, (cap, img_path) in enumerate(items):
                    if idx > 0 and idx % 2 == 0:
                        story.append(PageBreak())
                        story.append(Spacer(1, 10))
                    if os.path.exists(img_path):
                        im = PILImage.open(img_path)
                        iw, ih = im.size
                        w_pt = usable_w - 6
                        h_pt = w_pt * (ih / iw)
                        max_shot_h = 3.65 * inch if idx < 2 else 4.05 * inch
                        if h_pt > max_shot_h:
                            h_pt = max_shot_h
                            w_pt = h_pt * (iw / ih)
                        rl_im = RLImage(img_path, width=w_pt, height=h_pt)
                        ft = Table([[rl_im]], colWidths=[w_pt + 2])
                        ft.hAlign = "CENTER"
                        ft.setStyle(TableStyle([
                            ("BOX", (0, 0), (-1, -1), 0.5, colors.HexColor("#94A3B8")),
                            ("LEFTPADDING", (0, 0), (-1, -1), 1),
                            ("RIGHTPADDING", (0, 0), (-1, -1), 1),
                            ("TOPPADDING", (0, 0), (-1, -1), 1),
                            ("BOTTOMPADDING", (0, 0), (-1, -1), 1),
                        ]))
                        story.append(ft)
                        story.append(Paragraph(html.escape(cap), s_shot_cap))
                        story.append(Spacer(1, 10))

            elif btype == "code_file":
                _, filename_header, lines = blk
                story.append(Paragraph(html.escape(filename_header), s_code_hdr))
                for ln in lines:
                    if not ln.strip():
                        story.append(Spacer(1, 8))
                    else:
                        ind_spaces = len(ln) - len(ln.lstrip(" "))
                        escaped = html.escape(ln.lstrip(" "))
                        if ind_spaces > 0:
                            escaped = ("&nbsp;" * ind_spaces) + escaped
                        story.append(Paragraph(escaped, s_code_ln))
                story.append(Spacer(1, 14))

            elif btype == "page_break":
                story.append(PageBreak())

        doc.build(story)
        return tracker

    # ==================================================================
    # DOCX RENDERER (Synchronized SRMIST Reference Format)
    # ==================================================================
    def _build_docx(self, docx_path, page_map):
        doc = Document()
        sec = doc.sections[0]
        sec.page_width = Inches(8.27)
        sec.page_height = Inches(11.69)
        sec.top_margin = Inches(1.0)
        sec.bottom_margin = Inches(0.95)
        sec.left_margin = Inches(1.0)
        sec.right_margin = Inches(0.78)

        normal = doc.styles["Normal"]
        normal.font.name = "Times New Roman"
        normal.font.size = Pt(14)
        normal.font.color.rgb = RGBColor(0, 0, 0)

        def add_p(text="", size=14, bold=False, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
                  line_sp=1.5, sa=10, sb=0, first_ind=0, keep_next=False, bp=None):
            p = doc.add_paragraph()
            p.alignment = align
            p.paragraph_format.line_spacing = line_sp
            p.paragraph_format.space_before = Pt(sb)
            p.paragraph_format.space_after = Pt(sa)
            p.paragraph_format.keep_with_next = keep_next
            if first_ind:
                p.paragraph_format.first_line_indent = Pt(first_ind)
            if bp:
                rb = p.add_run(bp + " ")
                rb.font.name = "Times New Roman"
                rb.font.size = Pt(size)
                rb.font.bold = True
                rb.font.color.rgb = RGBColor(0, 0, 0)
            if text:
                r = p.add_run(text)
                r.font.name = "Times New Roman"
                r.font.size = Pt(size)
                r.font.bold = bold
                r.font.color.rgb = RGBColor(0, 0, 0)
            return p

        # 1. TITLE PAGE
        add_p(self.project_title, size=16, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.25, sa=14)
        add_p("A PROJECT REPORT SUBMITTED TO", size=14, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=12)
        add_p("SRM INSTITUTE OF SCIENCE AND TECHNOLOGY", size=14, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=12)
        add_p("IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE\nAWARD OF THE DEGREE OF", size=14, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.3, sa=14)
        add_p(self.degree_title, size=16, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=14)
        add_p("BY", size=16, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=14)
        add_p(self.student_name, size=12, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=10)
        add_p(f"REG. NO. {self.reg_no}", size=12, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=14)
        add_p("UNDER THE GUIDANCE OF", size=12, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=10)
        add_p(self.guide_upper, size=12, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=10)
        add_p("ASSISTANT PROFESSOR,", size=12, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=10)
        if self.srm_logo_path and os.path.exists(self.srm_logo_path):
            lp = doc.add_paragraph()
            lp.alignment = WD_ALIGN_PARAGRAPH.CENTER
            lp.paragraph_format.space_after = Pt(12)
            lp.add_run().add_picture(self.srm_logo_path, width=Inches(2.60))
        for fl in [
            "DEPARTMENT OF COMPUTER APPLICATIONS",
            "DIRECTORATE OF DISTANCE EDUCATION",
            "SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
            "(Deemed to be university u/s 3 of UGC Act, 1956)",
            "Kattankulathur – 603 203",
            "Chennai, Tamilnadu",
            self.month_year,
        ]:
            add_p(fl, size=12, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.25, sa=2)
        doc.add_page_break()

        # 2. BONAFIDE CERTIFICATE
        add_p("BONAFIDE CERTIFICATE", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, sa=24)
        pb = doc.add_paragraph()
        pb.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        pb.paragraph_format.first_line_indent = Pt(28)
        pb.paragraph_format.line_spacing = 1.6
        pb.paragraph_format.space_after = Pt(54)
        for seg, is_b in [
            ("This is to certify that the project report titled ", False),
            (f"“{self.project_title}”", True),
            (" is a bonafide work carried out by ", False),
            (self.student_name, True),
            (" (", False),
            (self.reg_no, True),
            (") under my supervision for the award of the Degree of Bachelor of Computer Applications. "
             "To my knowledge the work reported herein is the original work done by the candidate.", False),
        ]:
            rn = pb.add_run(seg)
            rn.font.name = "Times New Roman"
            rn.font.size = Pt(12)
            rn.font.bold = is_b

        bt = doc.add_table(rows=2, cols=2)
        bt.alignment = WD_TABLE_ALIGNMENT.CENTER
        c00, c01 = bt.rows[0].cells[0], bt.rows[0].cells[1]
        c00.paragraphs[0].paragraph_format.line_spacing = 1.5
        for txt, bld in [
            ("PROJECT GUIDE\n", True),
            (f"{self.guide_mixed}\n", True),
            ("Assistant Professor,\nSRMIST-DDE,\n\n\n", False),
        ]:
            r = c00.paragraphs[0].add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(12)
            r.font.bold = bld

        c01.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
        c01.paragraphs[0].paragraph_format.line_spacing = 1.5
        for txt, bld in [
            ("HEAD OF DEPARTMENT\n", True),
            (f"{self.guide_mixed}\n", True),
            ("Program Coordinator\nDepartment of Computer Applications,\nDirectorate of Distance Education (DDE)\n", False),
            ("SRMIST,\n\n", True),
        ]:
            r = c01.paragraphs[0].add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(12)
            r.font.bold = bld

        for cell_ex, ex_lbl, al in [
            (bt.rows[1].cells[0], "INTERNAL EXAMINER", WD_ALIGN_PARAGRAPH.LEFT),
            (bt.rows[1].cells[1], "EXTERNAL EXAMINER", WD_ALIGN_PARAGRAPH.CENTER),
        ]:
            cell_ex.paragraphs[0].alignment = al
            rx = cell_ex.paragraphs[0].add_run(ex_lbl)
            rx.font.name = "Times New Roman"
            rx.font.size = Pt(12)
            rx.font.bold = True
        doc.add_page_break()

        # 3. ACKNOWLEDGEMENT
        add_p("ACKNOWLEDGEMENT", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, sa=20)
        for txt, fi in [
            ("With profound gratitude to the ALMIGHTY, I take this chance to thank the people who helped me to complete this project.", 32),
            ("I take this as a right opportunity to say THANKS to my parents who are there to stand with me always with the words “ANYTHING IS ACHIEVABLE WITH HARDWORK AND PERSISTENCE”.", 0),
            ("I am thankful to Dr.T.R. Paarivendhar, Chancellor, SRM Institute of Science & Technology who gave us the platform to establish ourselves and reach greater heights.", 32),
            ("I express my deep sense of gratitude to the Director, Directorate of Distance Education, SRM Institute of Science & Technology for their wholehearted support and encouragement.", 32),
            (f"With gratitude I like to thank my project guide {self.guide_mixed}, Assistant Professor, Department of Computer Applications (DDE), for his help, support, encouragement, suggestions, and guidance throughout the development phases of the project.", 32),
            ("I take this opportunity to extend my sincere gratitude to all the faculty members of the department who provided their support through valuable comments and suggestions during the reviews.", 32),
            ("A great note of gratitude to friends and colleagues who helped in carrying out this project work to a successful completion.", 32),
        ]:
            add_p(txt, size=12, line_sp=1.6, sa=12, first_ind=fi)
        add_p(self.student_name.title(), size=12, bold=True, align=WD_ALIGN_PARAGRAPH.RIGHT, sb=28, sa=6)
        doc.add_page_break()

        # 4. TABLE OF CONTENTS (Dotted Tab Leaders matching ref_004.png & ref_005.png)
        add_p("TABLE OF CONTENTS", size=14, bold=False, align=WD_ALIGN_PARAGRAPH.CENTER, sa=16)
        for item in self.toc_entries:
            tp = doc.add_paragraph()
            tp.paragraph_format.left_indent = Pt(item["indent"])
            tp.paragraph_format.line_spacing = 1.6
            tp.paragraph_format.space_after = Pt(2)
            tp.paragraph_format.tab_stops.add_tab_stop(
                Inches(6.48), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS
            )
            pg = page_map.get(item["key"], "7") if (item["show_page"] and item["key"]) else None
            txt_run = f"{item['label']}\t{pg}" if pg is not None else item["label"]
            rt = tp.add_run(txt_run)
            rt.font.name = "Times New Roman"
            rt.font.size = Pt(13.5)
        doc.add_page_break()

        # 5. ABSTRACT
        add_p(self.abstract_heading_title, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, sa=16)
        add_p("Abstract:", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sa=12)
        for ap in self.abstract_paras:
            add_p(ap, size=14, line_sp=1.55, sa=10, first_ind=36)

        # 6. CHAPTERS 1..6
        for blk in self.body_blocks:
            btype = blk[0]
            if btype == "chapter":
                _, chap_num, subtitle = blk
                doc.add_page_break()
                add_p(f"CHAPTER {chap_num}", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, sa=14)
                if subtitle:
                    add_p(subtitle, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, sa=14)
            elif btype == "section":
                _, _, disp, new_page, render_heading = blk
                if new_page:
                    doc.add_page_break()
                if render_heading:
                    add_p(disp, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sb=12, sa=10, keep_next=True)
            elif btype == "subheading":
                _, text, sb, sa = blk
                add_p(text, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sb=sb, sa=sa, keep_next=True)
            elif btype == "p":
                _, text, bp, first_ind, sa = blk
                add_p(text, size=14, line_sp=1.55, sa=sa, first_ind=first_ind, bp=bp)
            elif btype == "bullet":
                _, text, bp = blk
                p = add_p(text, size=14, line_sp=1.55, sa=6, bp=(f"▪   {bp}" if bp else "▪  "))
                p.paragraph_format.left_indent = Pt(28)
                p.paragraph_format.first_line_indent = Pt(-16)
            elif btype == "numlist":
                _, items = blk
                for idx, it in enumerate(items, 1):
                    p = add_p(f"{idx}.   {it}", size=14, align=WD_ALIGN_PARAGRAPH.LEFT, line_sp=1.55, sa=4)
                    p.paragraph_format.left_indent = Pt(28)
            elif btype == "colon_specs":
                _, pairs = blk
                st = doc.add_table(rows=len(pairs), cols=3)
                for r_i, (lbl, val) in enumerate(pairs):
                    for c_i, v_str in enumerate([f"▪   {lbl}", ":", val]):
                        rc = st.rows[r_i].cells[c_i].paragraphs[0].add_run(v_str)
                        rc.font.name = "Times New Roman"
                        rc.font.size = Pt(14)
                doc.add_paragraph()
            elif btype == "diagram_inline":
                _, img_path, width_in, _ = blk
                if os.path.exists(img_path):
                    pi = doc.add_paragraph()
                    pi.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    pi.add_run().add_picture(img_path, width=Inches(min(width_in, 6.25)))
            elif btype == "diagram_page":
                _, _, disp, img_path, width_in, _ = blk
                doc.add_page_break()
                add_p(disp, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sa=14)
                if os.path.exists(img_path):
                    pi = doc.add_paragraph()
                    pi.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    pi.add_run().add_picture(img_path, width=Inches(min(width_in, 6.25)))
            elif btype == "lit_item":
                _, title, year, author, abs_txt = blk
                add_p(title, size=14, align=WD_ALIGN_PARAGRAPH.LEFT, sa=10, bp="TITLE:")
                add_p(year, size=14, align=WD_ALIGN_PARAGRAPH.LEFT, sa=10, bp="YEAR OF PUBLISHING:")
                add_p(author, size=14, align=WD_ALIGN_PARAGRAPH.LEFT, sa=10, bp="AUTHOR NAME:")
                add_p(abs_txt, size=14, line_sp=1.55, sa=14, bp="ABSTRACT:")
            elif btype == "table":
                _, title, headers, rows, cw = blk
                if title:
                    add_p(title, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sb=10, sa=6, keep_next=True)
                t = doc.add_table(rows=1 + len(rows), cols=len(headers))
                t.alignment = WD_TABLE_ALIGNMENT.CENTER
                for ci, h in enumerate(headers):
                    c = t.rows[0].cells[ci]
                    _docx_cell_borders(c)
                    _docx_cell_margins(c)
                    rh = c.paragraphs[0].add_run(str(h))
                    rh.font.name = "Times New Roman"
                    rh.font.size = Pt(11)
                    rh.font.bold = True
                for ri, row in enumerate(rows, 1):
                    for ci, v in enumerate(row):
                        c = t.rows[ri].cells[ci]
                        _docx_cell_borders(c)
                        _docx_cell_margins(c)
                        rv = c.paragraphs[0].add_run(str(v))
                        rv.font.name = "Times New Roman"
                        rv.font.size = Pt(10.5)
                doc.add_paragraph()
            elif btype == "screenshots":
                _, _, heading_disp, items = blk
                doc.add_page_break()
                add_p(heading_disp, size=14, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sa=12)
                for idx, (cap, img_path) in enumerate(items):
                    if idx > 0 and idx % 2 == 0:
                        doc.add_page_break()
                    if os.path.exists(img_path):
                        pi = doc.add_paragraph()
                        pi.alignment = WD_ALIGN_PARAGRAPH.CENTER
                        pi.paragraph_format.space_after = Pt(4)
                        pi.add_run().add_picture(img_path, width=Inches(6.20))
                        add_p(cap, size=12, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, line_sp=1.2, sa=14)
            elif btype == "code_file":
                _, filename_header, lines = blk
                add_p(filename_header, size=13.5, bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, sb=12, sa=8, keep_next=True)
                for ln in lines:
                    add_p(ln, size=11.5, align=WD_ALIGN_PARAGRAPH.LEFT, line_sp=1.35, sa=2)
            elif btype == "page_break":
                doc.add_page_break()

        doc.save(docx_path)

    def build_both(self, docx_path, pdf_path):
        print("Pass 1: Computing exact 1-based page numbers...")
        tmp = pdf_path + ".pass1.tmp"
        p1 = self._build_pdf_once(tmp, {})
        if os.path.exists(tmp):
            os.remove(tmp)
        print(f"Pass 1 complete: tracked {len(p1)} TOC targets.")
        print("Pass 2: Rendering final synchronized PDF and DOCX...")
        p2 = self._build_pdf_once(pdf_path, p1)
        # If TOC expanded across pass1->pass2, run one convergence pass
        if p2 != p1:
            p2 = self._build_pdf_once(pdf_path, p2)
        self._build_docx(docx_path, p2)
        return p2
