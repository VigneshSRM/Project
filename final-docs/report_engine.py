"""
Dual-Target Academic Report Engine (DOCX + PDF) for SRMIST BCA Final Project Report.
Renders an identical structured document AST to:
  1. Editable Microsoft Word (.docx) using python-docx (Times New Roman 12pt, 1.5 spacing)
  2. Print-ready PDF (.pdf) using ReportLab Platypus (STIXGeneral Times-metric serif 12pt, 1.5 spacing)
Supports two-pass exact page-number resolution for Table of Contents, List of Figures, and List of Tables.
"""
import os
import html
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import inch
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle,
    Image as RLImage, PageBreak, NextPageTemplate, Flowable, KeepTogether, Preformatted
)
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT, TA_JUSTIFY
from PIL import Image as PILImage

# Register STIXGeneral (Times-compatible academic serif) & DejaVuSansMono
MPL_FONTS = "/home/user/Project/.venv/lib/python3.11/site-packages/matplotlib/mpl-data/fonts/ttf"
pdfmetrics.registerFont(TTFont("TimesAcad", f"{MPL_FONTS}/STIXGeneral.ttf"))
pdfmetrics.registerFont(TTFont("TimesAcad-Bold", f"{MPL_FONTS}/STIXGeneralBol.ttf"))
pdfmetrics.registerFont(TTFont("TimesAcad-Italic", f"{MPL_FONTS}/STIXGeneralItalic.ttf"))
pdfmetrics.registerFont(TTFont("TimesAcad-BoldItalic", f"{MPL_FONTS}/STIXGeneralBolIta.ttf"))
pdfmetrics.registerFontFamily(
    "TimesAcad",
    normal="TimesAcad",
    bold="TimesAcad-Bold",
    italic="TimesAcad-Italic",
    boldItalic="TimesAcad-BoldItalic",
)
pdfmetrics.registerFont(TTFont("MonoCode", f"{MPL_FONTS}/DejaVuSansMono.ttf"))
pdfmetrics.registerFont(TTFont("MonoCode-Bold", f"{MPL_FONTS}/DejaVuSansMono-Bold.ttf"))


def to_roman(n):
    vals = [(100, 'c'), (90, 'xc'), (50, 'l'), (40, 'xl'),
            (10, 'x'), (9, 'ix'), (5, 'v'), (4, 'iv'), (1, 'i')]
    out = []
    for v, sym in vals:
        while n >= v:
            out.append(sym)
            n -= v
    return "".join(out) or "i"


class BookmarkFlowable(Flowable):
    """Zero-height flowable that records the exact page label where an item lands."""
    def __init__(self, key, tracker, chapter_title=None):
        super().__init__()
        self.key = key
        self.tracker = tracker
        self.chapter_title = chapter_title
        self.width = 0
        self.height = 0

    def draw(self):
        canv = self.canv
        state = getattr(canv, "_ims_state", {"mode": "prelim", "prelim_start": 1, "main_start": None, "chapter": ""})
        pno = canv.getPageNumber()
        if self.chapter_title:
            state["chapter"] = self.chapter_title
        if state["mode"] == "prelim":
            label = to_roman(pno - state["prelim_start"] + 1)
        else:
            label = str(pno - state["main_start"] + 1)
        self.tracker[self.key] = label


class ModeSwitchFlowable(Flowable):
    """Switches PDF page numbering from Roman preliminary to Arabic main body starting at 1."""
    def __init__(self, new_mode):
        super().__init__()
        self.new_mode = new_mode
        self.width = 0
        self.height = 0

    def draw(self):
        state = self.canv._ims_state
        state["mode"] = self.new_mode
        if self.new_mode == "main" and state["main_start"] is None:
            state["main_start"] = self.canv.getPageNumber()


# ---------------- DOCX XML HELPERS ----------------
def docx_cell_bg(cell, hex_color):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{hex_color}"/>'))


def docx_cell_margins(cell, top=90, bottom=90, left=130, right=130):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    ))


def docx_cell_borders(cell, color="CBD5E1", sz="4"):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:left w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:right w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'</w:tcBorders>'
    ))


def docx_repeat_header(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement('w:tblHeader'))
    trPr.append(OxmlElement('w:cantSplit'))


def docx_cant_split(row):
    trPr = row._tr.get_or_add_trPr()
    trPr.append(OxmlElement('w:cantSplit'))


def docx_add_page_number_field(run):
    fldChar1 = parse_xml(r'<w:fldChar %s w:fldCharType="begin"/>' % nsdecls('w'))
    instrText = parse_xml(r'<w:instrText %s xml:space="preserve"> PAGE </w:instrText>' % nsdecls('w'))
    fldChar2 = parse_xml(r'<w:fldChar %s w:fldCharType="separate"/>' % nsdecls('w'))
    fldChar3 = parse_xml(r'<w:fldChar %s w:fldCharType="end"/>' % nsdecls('w'))
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)


def docx_set_section_pgnum(section, fmt="decimal", start=1):
    sectPr = section._sectPr
    for child in list(sectPr):
        if child.tag == qn('w:pgNumType'):
            sectPr.remove(child)
    pgNumType = parse_xml(f'<w:pgNumType {nsdecls("w")} w:fmt="{fmt}" w:start="{start}"/>')
    sectPr.append(pgNumType)


def docx_set_page_border(section):
    sectPr = section._sectPr
    pgBorders = parse_xml(
        f'<w:pgBorders {nsdecls("w")} w:offsetFrom="page">'
        f'<w:top w:val="double" w:sz="12" w:space="24" w:color="0F2744"/>'
        f'<w:left w:val="double" w:sz="12" w:space="24" w:color="0F2744"/>'
        f'<w:bottom w:val="double" w:sz="12" w:space="24" w:color="0F2744"/>'
        f'<w:right w:val="double" w:sz="12" w:space="24" w:color="0F2744"/>'
        f'</w:pgBorders>'
    )
    sectPr.append(pgBorders)


class AcademicReportBuilder:
    """
    Collects structured academic document blocks once and renders both
    SRMIST-compliant DOCX and synchronized PDF with two-pass TOC/LOF/LOT pagination.
    """
    def __init__(self, project_title, short_title, student_name, reg_no, degree_title,
                 guide_name, guide_title, dept_line1, dept_line2, month_year, srm_logo_path):
        self.project_title = project_title
        self.short_title = short_title
        self.student_name = student_name
        self.reg_no = reg_no
        self.degree_title = degree_title
        self.guide_name = guide_name
        self.guide_title = guide_title
        self.dept_line1 = dept_line1
        self.dept_line2 = dept_line2
        self.month_year = month_year
        self.srm_logo_path = srm_logo_path

        self.prelim_blocks = []
        self.body_blocks = []
        self.toc_entries = []   # (level, key, label)
        self.lof_entries = []   # (key, fig_num, caption)
        self.lot_entries = []   # (key, tbl_num, caption)
        self._id_counter = 0

    def _next_key(self, prefix):
        self._id_counter += 1
        return f"{prefix}_{self._id_counter:04d}"

    # -------- Block Recording API --------
    def add_prelim_heading(self, title, toc_label=None):
        key = self._next_key("prelim")
        self.toc_entries.append((1, key, toc_label or title.title()))
        self.prelim_blocks.append(("prelim_h1", key, title))

    def add_prelim_p(self, text, bold_prefix=None, align="justify", space_after=8):
        self.prelim_blocks.append(("p", text, bold_prefix, align, space_after))

    def add_prelim_table(self, headers, rows, col_widths=None):
        self.prelim_blocks.append(("raw_table", headers, rows, col_widths))

    def add_prelim_page_break(self):
        self.prelim_blocks.append(("page_break",))

    def add_toc_placeholder(self):
        key = self._next_key("prelim")
        self.toc_entries.append((1, key, "Table of Contents"))
        self.prelim_blocks.append(("toc_placeholder", key))

    def add_lof_placeholder(self):
        key = self._next_key("prelim")
        self.toc_entries.append((1, key, "List of Figures"))
        self.prelim_blocks.append(("lof_placeholder", key))

    def add_lot_placeholder(self):
        key = self._next_key("prelim")
        self.toc_entries.append((1, key, "List of Tables"))
        self.prelim_blocks.append(("lot_placeholder", key))

    # -------- Main Body Recording API --------
    def start_chapter(self, chap_num, chap_title):
        """Starts a new numbered chapter (or unnumbered major part like REFERENCES / APPENDICES)."""
        key = self._next_key("chap")
        if chap_num is not None:
            full_label = f"CHAPTER {chap_num}: {chap_title.upper()}"
            running = f"Chapter {chap_num}: {chap_title}"
        else:
            full_label = chap_title.upper()
            running = chap_title
        self.toc_entries.append((0, key, full_label))
        self.body_blocks.append(("chapter", key, chap_num, chap_title.upper(), running))

    def h2(self, sec_num, title):
        key = self._next_key("sec")
        label = f"{sec_num} {title}" if sec_num else title
        self.toc_entries.append((1, key, label))
        self.body_blocks.append(("h2", key, label))

    def h3(self, sub_num, title):
        key = self._next_key("sub")
        label = f"{sub_num} {title}" if sub_num else title
        self.toc_entries.append((2, key, label))
        self.body_blocks.append(("h3", key, label))

    def p(self, text, bold_prefix=None, space_after=7):
        self.body_blocks.append(("p", text, bold_prefix, "justify", space_after))

    def bullet(self, bold_prefix, text):
        self.body_blocks.append(("bullet", bold_prefix, text))

    def table(self, tbl_num, caption, headers, rows, col_widths=None):
        key = self._next_key("tbl")
        self.lot_entries.append((key, tbl_num, caption))
        self.body_blocks.append(("table", key, tbl_num, caption, headers, rows, col_widths))

    def figure(self, fig_num, caption, image_path, width_in=5.8):
        key = self._next_key("fig")
        self.lof_entries.append((key, fig_num, caption))
        self.body_blocks.append(("figure", key, fig_num, caption, image_path, width_in))

    def code(self, title, code_text):
        wrapped_lines = []
        for raw in code_text.strip("\n").splitlines():
            if len(raw) <= 81:
                wrapped_lines.append(raw)
            else:
                lead = len(raw) - len(raw.lstrip(" "))
                cont_indent = " " * min(lead + 4, 16)
                rem = raw
                first = True
                while len(rem) > 81:
                    limit = 81
                    cut = rem.rfind(" ", lead + 10 if first else len(cont_indent) + 8, limit + 1)
                    if cut <= 0:
                        cut = limit
                    wrapped_lines.append(rem[:cut].rstrip())
                    rem = cont_indent + rem[cut:].lstrip()
                    first = False
                if rem.strip():
                    wrapped_lines.append(rem)
        self.body_blocks.append(("code", title, "\n".join(wrapped_lines)))

    def page_break(self):
        self.body_blocks.append(("page_break",))

    # ==========================================================================
    # PDF RENDERER (ReportLab Platypus, Two-Pass Exact Page Numbers)
    # ==========================================================================
    def _build_pdf_once(self, pdf_path, page_map):
        tracker = {}
        W, H = A4  # 595.27 x 841.89 pt (8.27 x 11.69 in)
        left_m = 1.25 * inch
        right_m = 1.00 * inch
        top_m = 1.00 * inch
        bot_m = 1.00 * inch
        usable_w = W - left_m - right_m  # 6.02 in = 433.27 pt

        def draw_cover(canvas, doc):
            canvas._ims_state = {"mode": "prelim", "prelim_start": 1, "main_start": None, "chapter": ""}
            canvas.saveState()
            canvas.setStrokeColor(colors.HexColor("#0F2744"))
            canvas.setLineWidth(2.2)
            canvas.rect(28, 28, W - 56, H - 56)
            canvas.setLineWidth(0.7)
            canvas.rect(33, 33, W - 66, H - 66)
            canvas.restoreState()

        def draw_prelim(canvas, doc):
            canvas.saveState()
            state = canvas._ims_state
            pno = canvas.getPageNumber()
            rom = to_roman(pno - state["prelim_start"] + 1)
            canvas.setFont("TimesAcad", 10.5)
            canvas.setFillColor(colors.HexColor("#334155"))
            canvas.drawCentredString(W / 2, 0.62 * inch, rom)
            canvas.restoreState()

        def draw_main(canvas, doc):
            canvas.saveState()
            state = canvas._ims_state
            if state["main_start"] is None:
                state["main_start"] = canvas.getPageNumber()
            arab = canvas.getPageNumber() - state["main_start"] + 1
            # Header
            canvas.setStrokeColor(colors.HexColor("#CBD5E1"))
            canvas.setLineWidth(0.6)
            canvas.line(left_m, H - 0.72 * inch, W - right_m, H - 0.72 * inch)
            canvas.setFont("TimesAcad-Italic", 9.2)
            canvas.setFillColor(colors.HexColor("#475569"))
            canvas.drawString(left_m, H - 0.64 * inch, self.short_title)
            canvas.drawRightString(W - right_m, H - 0.64 * inch, state.get("chapter", ""))
            # Footer
            canvas.line(left_m, 0.75 * inch, W - right_m, 0.75 * inch)
            canvas.setFont("TimesAcad", 10.5)
            canvas.setFillColor(colors.HexColor("#0F172A"))
            canvas.drawCentredString(W / 2, 0.54 * inch, f"Page {arab}")
            canvas.restoreState()

        frame = Frame(left_m, bot_m, usable_w, H - top_m - bot_m, id="normal",
                      leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        doc = BaseDocTemplate(
            pdf_path, pagesize=A4,
            leftMargin=left_m, rightMargin=right_m, topMargin=top_m, bottomMargin=bot_m,
            title=self.project_title, author=self.student_name
        )
        doc.addPageTemplates([
            PageTemplate(id="Cover", frames=[Frame(0.9 * inch, 0.75 * inch, W - 1.8 * inch, H - 1.5 * inch, id="cov")], onPage=draw_cover),
            PageTemplate(id="Prelim", frames=[frame], onPageEnd=draw_prelim),
            PageTemplate(id="Main", frames=[frame], onPageEnd=draw_main),
        ])

        # Styles
        s_body = ParagraphStyle("Body", fontName="TimesAcad", fontSize=12, leading=18, alignment=TA_JUSTIFY, spaceAfter=7, textColor=colors.HexColor("#111827"))
        s_center = ParagraphStyle("Center", parent=s_body, alignment=TA_CENTER)
        s_right = ParagraphStyle("Right", parent=s_body, alignment=TA_RIGHT)
        s_bullet = ParagraphStyle("Bullet", parent=s_body, leftIndent=18, firstLineIndent=-10, spaceAfter=5)
        s_chap_no = ParagraphStyle("ChapNo", fontName="TimesAcad-Bold", fontSize=14, leading=18, alignment=TA_CENTER, spaceAfter=4, textColor=colors.HexColor("#1E3A8A"))
        s_chap_ti = ParagraphStyle("ChapTi", fontName="TimesAcad-Bold", fontSize=16, leading=22, alignment=TA_CENTER, spaceAfter=16, textColor=colors.HexColor("#0F2744"))
        s_prelim_h = ParagraphStyle("PrelimH", fontName="TimesAcad-Bold", fontSize=15, leading=20, alignment=TA_CENTER, spaceAfter=16, textColor=colors.HexColor("#0F2744"))
        s_h2 = ParagraphStyle("H2", fontName="TimesAcad-Bold", fontSize=13.5, leading=18, spaceBefore=13, spaceAfter=6, keepWithNext=True, textColor=colors.HexColor("#0F2744"))
        s_h3 = ParagraphStyle("H3", fontName="TimesAcad-Bold", fontSize=12, leading=16.5, spaceBefore=10, spaceAfter=4, keepWithNext=True, textColor=colors.HexColor("#1E3A8A"))
        s_cap = ParagraphStyle("Cap", fontName="TimesAcad-BoldItalic", fontSize=10.2, leading=14, alignment=TA_CENTER, spaceBefore=4, spaceAfter=10, textColor=colors.HexColor("#334155"))
        s_tbl_cap = ParagraphStyle("TblCap", fontName="TimesAcad-Bold", fontSize=10.5, leading=14, alignment=TA_LEFT, spaceBefore=8, spaceAfter=4, keepWithNext=True, textColor=colors.HexColor("#0F2744"))
        s_code_ti = ParagraphStyle("CodeTi", fontName="TimesAcad-Bold", fontSize=10, leading=13, spaceBefore=8, spaceAfter=3, keepWithNext=True, textColor=colors.HexColor("#1E3A8A"))
        s_code = ParagraphStyle("Code", fontName="MonoCode", fontSize=7.8, leading=10.4, textColor=colors.HexColor("#0F172A"))
        s_th = ParagraphStyle("TH", fontName="TimesAcad-Bold", fontSize=9.5, leading=12.5, alignment=TA_LEFT, textColor=colors.white)
        s_td = ParagraphStyle("TD", fontName="TimesAcad", fontSize=9.3, leading=12.5, alignment=TA_LEFT, textColor=colors.HexColor("#0F172A"))

        story = []

        # ---- 1. COVER / TITLE PAGE (SRMIST BCA Format) ----
        story.append(Spacer(1, 10))
        story.append(Paragraph(html.escape(self.project_title),
                               ParagraphStyle("CovTitle", fontName="TimesAcad-Bold", fontSize=15.5, leading=21, alignment=TA_CENTER, spaceAfter=18)))
        story.append(Paragraph("A PROJECT REPORT SUBMITTED TO",
                               ParagraphStyle("CovSub1", fontName="TimesAcad-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=8)))
        story.append(Paragraph("SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
                               ParagraphStyle("CovSub2", fontName="TimesAcad-Bold", fontSize=13, leading=17, alignment=TA_CENTER, spaceAfter=14)))
        story.append(Paragraph("IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE<br/>AWARD OF THE DEGREE OF",
                               ParagraphStyle("CovSub3", fontName="TimesAcad-Bold", fontSize=12, leading=17, alignment=TA_CENTER, spaceAfter=14)))
        story.append(Paragraph(html.escape(self.degree_title),
                               ParagraphStyle("CovDeg", fontName="TimesAcad-Bold", fontSize=15.5, leading=20, alignment=TA_CENTER, spaceAfter=14)))
        story.append(Paragraph("BY",
                               ParagraphStyle("CovBy", fontName="TimesAcad-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=8)))
        story.append(Paragraph(html.escape(self.student_name),
                               ParagraphStyle("CovStu", fontName="TimesAcad-Bold", fontSize=14, leading=18, alignment=TA_CENTER, spaceAfter=4)))
        story.append(Paragraph(f"REG. NO. {html.escape(self.reg_no)}",
                               ParagraphStyle("CovReg", fontName="TimesAcad-Bold", fontSize=12.5, leading=17, alignment=TA_CENTER, spaceAfter=16)))
        story.append(Paragraph("UNDER THE GUIDANCE OF",
                               ParagraphStyle("CovUg", fontName="TimesAcad-Bold", fontSize=12, leading=16, alignment=TA_CENTER, spaceAfter=8)))
        story.append(Paragraph(html.escape(self.guide_name),
                               ParagraphStyle("CovGn", fontName="TimesAcad-Bold", fontSize=12.5, leading=16, alignment=TA_CENTER, spaceAfter=3)))
        story.append(Paragraph(html.escape(self.guide_title),
                               ParagraphStyle("CovGt", fontName="TimesAcad-Bold", fontSize=11.5, leading=15, alignment=TA_CENTER, spaceAfter=12)))
        if os.path.exists(self.srm_logo_path):
            im = PILImage.open(self.srm_logo_path)
            iw, ih = im.size
            w_pt = 2.45 * inch
            h_pt = w_pt * (ih / iw)
            logo = RLImage(self.srm_logo_path, width=w_pt, height=h_pt)
            logo.hAlign = "CENTER"
            story.append(logo)
            story.append(Spacer(1, 12))
        for ln in [self.dept_line1, self.dept_line2,
                   "SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
                   "(Deemed to be university u/s 3 of UGC Act, 1956)",
                   "Kattankulathur – 603 203",
                   "Chennai, Tamilnadu",
                   self.month_year]:
            story.append(Paragraph(html.escape(ln),
                                   ParagraphStyle("CovBot", fontName="TimesAcad-Bold", fontSize=12, leading=16.5, alignment=TA_CENTER)))

        story.append(NextPageTemplate("Prelim"))
        story.append(PageBreak())

        # ---- 2. BONAFIDE CERTIFICATE ----
        bon_key = "prelim_bonafide"
        story.append(BookmarkFlowable(bon_key, tracker))
        story.append(Paragraph("BONAFIDE CERTIFICATE", s_prelim_h))
        story.append(Spacer(1, 10))
        bon_txt = (
            f'This is to certify that the project report titled <b>“{html.escape(self.project_title)}”</b> '
            f'is a bonafide work carried out by <b>{html.escape(self.student_name)} ({html.escape(self.reg_no)})</b> '
            f'under my supervision for the award of the Degree of <b>{html.escape(self.degree_title.title())}</b>. '
            f'To my knowledge the work reported herein is the original work done by the candidate.'
        )
        story.append(Paragraph(bon_txt, ParagraphStyle("BonBody", parent=s_body, fontSize=12.5, leading=21, spaceAfter=48)))

        sig_style = ParagraphStyle("Sig", fontName="TimesAcad", fontSize=11.5, leading=16)
        sig_bold = ParagraphStyle("SigB", fontName="TimesAcad-Bold", fontSize=11.5, leading=16)
        sig_tbl = Table([
            [Paragraph("<b>PROJECT GUIDE</b>", sig_bold), Paragraph("<b>HEAD OF DEPARTMENT</b>", sig_bold)],
            [Spacer(1, 28), Spacer(1, 28)],
            [Paragraph("<b>Dr. A. Thirumurthi Raja</b><br/>Assistant Professor,<br/>SRMIST-DDE,", sig_style),
             Paragraph("<b>Dr. A. Thirumurthi Raja</b><br/>Program Coordinator,<br/>Department of Computer Applications,<br/>Directorate of Distance Education (DDE),<br/>SRMIST,", sig_style)],
            [Spacer(1, 56), Spacer(1, 56)],
            [Paragraph("<b>INTERNAL EXAMINER</b>", sig_bold), Paragraph("<b>EXTERNAL EXAMINER</b>", sig_bold)],
        ], colWidths=[usable_w * 0.48, usable_w * 0.52])
        sig_tbl.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 2)]))
        story.append(sig_tbl)
        story.append(PageBreak())

        # ---- 3. DECLARATION ----
        dec_key = "prelim_declaration"
        story.append(BookmarkFlowable(dec_key, tracker))
        story.append(Paragraph("DECLARATION", s_prelim_h))
        story.append(Spacer(1, 10))
        dec_txt = (
            f'I, <b>{html.escape(self.student_name)} ({html.escape(self.reg_no)})</b>, hereby declare that the project report entitled '
            f'<b>“{html.escape(self.project_title)}”</b> submitted to <b>SRM Institute of Science and Technology</b> '
            f'in partial fulfillment of the requirements for the award of the Degree of '
            f'<b>{html.escape(self.degree_title.title())}</b> is a record of original software engineering and academic project work '
            f'carried out by me under the supervision and guidance of <b>Dr. A. Thirumurthi Raja, M.C.A., M.E., Ph.D.</b>, '
            f'Assistant Professor, Department of Computer Applications, Directorate of Distance Education, SRMIST. '
            f'I further declare that this project report or any part thereof has not been submitted earlier to any other '
            f'University or Institution for the award of any degree, diploma, or fellowship.'
        )
        story.append(Paragraph(dec_txt, ParagraphStyle("DecBody", parent=s_body, fontSize=12.5, leading=21, spaceAfter=64)))
        dec_tbl = Table([
            [Paragraph("Place: Chennai<br/>Date: November 2026", sig_style),
             Paragraph(f"<b>{html.escape(self.student_name)}</b><br/>({html.escape(self.reg_no)})<br/>Signature of the Candidate",
                       ParagraphStyle("SigR", parent=sig_style, alignment=TA_RIGHT))]
        ], colWidths=[usable_w * 0.5, usable_w * 0.5])
        story.append(dec_tbl)
        story.append(PageBreak())

        # ---- 4. ACKNOWLEDGEMENT ----
        ack_key = "prelim_ack"
        story.append(BookmarkFlowable(ack_key, tracker))
        story.append(Paragraph("ACKNOWLEDGEMENT", s_prelim_h))
        ack_paras = [
            "With profound gratitude to the ALMIGHTY, I take this chance to thank the people who helped me to complete this project.",
            "I take this as a right opportunity to say THANKS to my parents who are there to stand with me always with the words “YOU CAN”.",
            "I am thankful to <b>Dr. T. R. Paarivendhar, Chancellor</b>, SRM Institute of Science &amp; Technology who gave us the platform to establish ourselves and reach greater heights.",
            "I express my deep sense of gratitude to the Director <b>Dr. Manoranjan Pon Ram</b> for his wholehearted support and encouragement.",
            "I earnestly thank <b>Dr. A. Thirumurthi Raja, Programme Coordinator</b>, BCA/MCA — Directorate of Distance Education, SRM Institute of Science &amp; Technology who always encourages us to pursue novel engineering solutions.",
            "It is my delight to thank my project guide <b>Dr. A. Thirumurthi Raja</b>, Assistant Professor, Department of Computer Applications (DDE), for his invaluable help, technical insights, encouragement, constructive suggestions, and continuous guidance throughout the analysis, design, and implementation phases of this project.",
            "I convey my sincere gratitude to all the faculty members of the Department of Computer Applications who extended their support through valuable comments and suggestions during the project reviews.",
            "A great note of gratitude to my friends and colleagues who helped in carrying out this project work to a successful completion."
        ]
        for ap in ack_paras:
            story.append(Paragraph(ap, ParagraphStyle("AckP", parent=s_body, spaceAfter=10)))
        story.append(Spacer(1, 24))
        story.append(Paragraph(f"<b>{html.escape(self.student_name)}</b>", s_right))
        story.append(PageBreak())

        def make_rl_table(headers, rows, col_widths):
            if col_widths:
                total_in = sum(col_widths)
                widths = [usable_w * (w / total_in) for w in col_widths]
            else:
                widths = [usable_w / len(headers)] * len(headers)
            data = [[Paragraph(html.escape(str(h)), s_th) for h in headers]]
            for r in rows:
                data.append([Paragraph(html.escape(str(c)).replace("\n", "<br/>"), s_td) for c in r])
            t = Table(data, colWidths=widths, repeatRows=1)
            style_cmds = [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0F2744")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("GRID", (0, 0), (-1, -1), 0.45, colors.HexColor("#CBD5E1")),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
            for r_i in range(1, len(data)):
                if r_i % 2 == 0:
                    style_cmds.append(("BACKGROUND", (0, r_i), (-1, r_i), colors.HexColor("#F8FAFC")))
            t.setStyle(TableStyle(style_cmds))
            return t

        def make_leader_table(entries):
            rows_data = []
            for lvl, label, pg in entries:
                indent = 0 if lvl == 0 else (12 if lvl == 1 else 26)
                fnt = "TimesAcad-Bold" if lvl == 0 else "TimesAcad"
                sz = 11.2 if lvl == 0 else (10.6 if lvl == 1 else 10.0)
                st_l = ParagraphStyle(f"TL_{lvl}", fontName=fnt, fontSize=sz, leading=14.5, leftIndent=indent,
                                      textColor=colors.HexColor("#0F2744" if lvl == 0 else "#1E293B"))
                st_r = ParagraphStyle(f"TR_{lvl}", fontName=fnt, fontSize=sz, leading=14.5, alignment=TA_RIGHT,
                                      textColor=colors.HexColor("#0F2744" if lvl == 0 else "#1E293B"))
                rows_data.append([Paragraph(html.escape(label), st_l), Paragraph(html.escape(str(pg)), st_r)])
            t = Table(rows_data, colWidths=[usable_w - 48, 48])
            t.setStyle(TableStyle([
                ("VALIGN", (0, 0), (-1, -1), "BOTTOM"),
                ("LINEBELOW", (0, 0), (-1, -1), 0.35, colors.HexColor("#E2E8F0")),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
                ("RIGHTPADDING", (0, 0), (-1, -1), 0),
            ]))
            return t

        # ---- 5. DYNAMIC PRELIMINARY BLOCKS (Abstract, TOC, LOF, LOT, Abbreviations) ----
        fixed_prelim = [
            (1, "Title Page", "i"),
            (1, "Bonafide Certificate", page_map.get(bon_key, "ii")),
            (1, "Declaration", page_map.get(dec_key, "iii")),
            (1, "Acknowledgement", page_map.get(ack_key, "iv")),
        ]

        for blk in self.prelim_blocks:
            kind = blk[0]
            if kind == "prelim_h1":
                _, key, title = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph(html.escape(title), s_prelim_h))
            elif kind == "p":
                _, text, bp, align, sa = blk
                st = s_center if align == "center" else (s_right if align == "right" else s_body)
                if sa != 7:
                    st = ParagraphStyle("TmpP", parent=st, spaceAfter=sa)
                full = f"<b>{html.escape(bp)}</b> {html.escape(text)}" if bp else html.escape(text)
                story.append(Paragraph(full, st))
            elif kind == "raw_table":
                _, headers, rows, cw = blk
                story.append(make_rl_table(headers, rows, cw))
                story.append(Spacer(1, 8))
            elif kind == "page_break":
                story.append(PageBreak())
            elif kind == "toc_placeholder":
                _, key = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph("TABLE OF CONTENTS", s_prelim_h))
                all_toc = list(fixed_prelim)
                for lvl, tkey, tlabel in self.toc_entries:
                    all_toc.append((lvl, tlabel, page_map.get(tkey, "1")))
                story.append(make_leader_table(all_toc))
                story.append(PageBreak())
            elif kind == "lof_placeholder":
                _, key = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph("LIST OF FIGURES", s_prelim_h))
                lof_rows = [(1, f"Figure {fn}: {cap}", page_map.get(fkey, "1")) for fkey, fn, cap in self.lof_entries]
                story.append(make_leader_table(lof_rows))
                story.append(PageBreak())
            elif kind == "lot_placeholder":
                _, key = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph("LIST OF TABLES", s_prelim_h))
                lot_rows = [(1, f"Table {tn}: {cap}", page_map.get(tkey, "1")) for tkey, tn, cap in self.lot_entries]
                story.append(make_leader_table(lot_rows))

        # ---- 6. SWITCH TO MAIN BODY (Arabic Page 1+) ----
        story.append(NextPageTemplate("Main"))
        story.append(PageBreak())
        story.append(ModeSwitchFlowable("main"))

        first_chapter = True
        for blk in self.body_blocks:
            kind = blk[0]
            if kind == "chapter":
                _, key, chap_num, chap_title, running = blk
                if not first_chapter:
                    story.append(PageBreak())
                first_chapter = False
                story.append(BookmarkFlowable(key, tracker, chapter_title=running))
                if chap_num is not None:
                    story.append(Paragraph(f"CHAPTER {chap_num}", s_chap_no))
                story.append(Paragraph(html.escape(chap_title), s_chap_ti))
                story.append(Spacer(1, 4))
            elif kind == "h2":
                _, key, label = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph(html.escape(label), s_h2))
            elif kind == "h3":
                _, key, label = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph(html.escape(label), s_h3))
            elif kind == "p":
                _, text, bp, align, sa = blk
                st = s_body if sa == 7 else ParagraphStyle("BodySA", parent=s_body, spaceAfter=sa)
                full = f"<b>{html.escape(bp)}</b> {html.escape(text)}" if bp else html.escape(text)
                story.append(Paragraph(full, st))
            elif kind == "bullet":
                _, bp, text = blk
                full = f"• <b>{html.escape(bp)}</b> {html.escape(text)}" if bp else f"• {html.escape(text)}"
                story.append(Paragraph(full, s_bullet))
            elif kind == "table":
                _, key, tbl_num, caption, headers, rows, cw = blk
                story.append(BookmarkFlowable(key, tracker))
                story.append(Paragraph(f"Table {html.escape(tbl_num)}: {html.escape(caption)}", s_tbl_cap))
                story.append(make_rl_table(headers, rows, cw))
                story.append(Spacer(1, 8))
            elif kind == "figure":
                _, key, fig_num, caption, img_path, width_in = blk
                if os.path.exists(img_path):
                    im = PILImage.open(img_path)
                    iw, ih = im.size
                    w_pt = min(width_in * inch, usable_w)
                    h_pt = w_pt * (ih / iw)
                    max_h = 6.2 * inch
                    if h_pt > max_h:
                        h_pt = max_h
                        w_pt = h_pt * (iw / ih)
                    rl_im = RLImage(img_path, width=w_pt, height=h_pt)
                    rl_im.hAlign = "CENTER"
                    fig_flowable = rl_im
                    if os.path.basename(img_path).startswith("real_"):
                        ft = Table([[rl_im]], colWidths=[w_pt + 4])
                        ft.hAlign = "CENTER"
                        ft.setStyle(TableStyle([
                            ("BOX", (0, 0), (-1, -1), 0.65, colors.HexColor("#CBD5E1")),
                            ("LEFTPADDING", (0, 0), (-1, -1), 2),
                            ("RIGHTPADDING", (0, 0), (-1, -1), 2),
                            ("TOPPADDING", (0, 0), (-1, -1), 2),
                            ("BOTTOMPADDING", (0, 0), (-1, -1), 2),
                        ]))
                        fig_flowable = ft
                    story.append(Spacer(1, 6))
                    story.append(KeepTogether([
                        BookmarkFlowable(key, tracker),
                        fig_flowable,
                        Paragraph(f"Figure {html.escape(fig_num)}: {html.escape(caption)}", s_cap)
                    ]))
            elif kind == "code":
                _, title, code_text = blk
                if title:
                    story.append(Paragraph(html.escape(title), s_code_ti))
                lines = code_text.strip("\n").splitlines()
                # Chunk long code listings cleanly across pages (max 38 lines per shaded box)
                for chunk_start in range(0, len(lines), 38):
                    chunk = "\n".join(lines[chunk_start:chunk_start + 38])
                    pre = Preformatted(chunk, s_code, maxLineLength=96)
                    ct = Table([[pre]], colWidths=[usable_w])
                    ct.setStyle(TableStyle([
                        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F8FAFC")),
                        ("BOX", (0, 0), (-1, -1), 0.6, colors.HexColor("#CBD5E1")),
                        ("LEFTPADDING", (0, 0), (-1, -1), 8),
                        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                        ("TOPPADDING", (0, 0), (-1, -1), 6),
                        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                    ]))
                    story.append(ct)
                    story.append(Spacer(1, 5))
            elif kind == "page_break":
                story.append(PageBreak())

        doc.build(story)
        return tracker

    # ==========================================================================
    # WORD (.DOCX) RENDERER (SRMIST BCA Format, Synchronized Page Numbers)
    # ==========================================================================
    def _build_docx(self, docx_path, page_map):
        doc = docx.Document()

        # Default Normal style: Times New Roman 12pt
        normal = doc.styles["Normal"]
        normal.font.name = "Times New Roman"
        normal.font.size = Pt(12)

        def setup_sec(sec, fmt="lowerRoman", start=1, border=False):
            sec.page_width = Inches(8.27)
            sec.page_height = Inches(11.69)
            sec.left_margin = Inches(1.25)
            sec.right_margin = Inches(1.00)
            sec.top_margin = Inches(1.00)
            sec.bottom_margin = Inches(1.00)
            docx_set_section_pgnum(sec, fmt=fmt, start=start)
            if border:
                docx_set_page_border(sec)

        # ---- SECTION 0: COVER PAGE ----
        sec_cov = doc.sections[0]
        setup_sec(sec_cov, fmt="lowerRoman", start=1, border=True)
        sec_cov.different_first_page_header_footer = True

        def cov_line(txt, sz=12, bold=True, space_after=8):
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(space_after)
            p.paragraph_format.line_spacing = 1.25
            r = p.add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(sz)
            r.font.bold = bold

        cov_line(self.project_title, sz=16, bold=True, space_after=14)
        cov_line("A PROJECT REPORT SUBMITTED TO", sz=12, bold=True, space_after=6)
        cov_line("SRM INSTITUTE OF SCIENCE AND TECHNOLOGY", sz=13, bold=True, space_after=12)
        cov_line("IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE AWARD OF THE DEGREE OF", sz=12, bold=True, space_after=12)
        cov_line(self.degree_title, sz=16, bold=True, space_after=12)
        cov_line("BY", sz=12, bold=True, space_after=6)
        cov_line(self.student_name, sz=14, bold=True, space_after=3)
        cov_line(f"REG. NO. {self.reg_no}", sz=12.5, bold=True, space_after=12)
        cov_line("UNDER THE GUIDANCE OF", sz=12, bold=True, space_after=6)
        cov_line(self.guide_name, sz=12.5, bold=True, space_after=2)
        cov_line(self.guide_title, sz=11.5, bold=True, space_after=10)

        if os.path.exists(self.srm_logo_path):
            p_logo = doc.add_paragraph()
            p_logo.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_logo.paragraph_format.space_after = Pt(10)
            p_logo.add_run().add_picture(self.srm_logo_path, width=Inches(2.4))

        for b_ln in [self.dept_line1, self.dept_line2,
                     "SRM INSTITUTE OF SCIENCE AND TECHNOLOGY",
                     "(Deemed to be university u/s 3 of UGC Act, 1956)",
                     "Kattankulathur – 603 203",
                     "Chennai, Tamilnadu",
                     self.month_year]:
            cov_line(b_ln, sz=12, bold=True, space_after=3)

        # ---- SECTION 1: PRELIMINARY PAGES (ii, iii, iv...) ----
        sec_pre = doc.add_section(WD_SECTION.NEW_PAGE)
        setup_sec(sec_pre, fmt="lowerRoman", start=2, border=False)
        sec_pre.different_first_page_header_footer = False
        sec_pre.header.is_linked_to_previous = False
        sec_pre.footer.is_linked_to_previous = False
        pf = sec_pre.footer.paragraphs[0]
        pf.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_pf = pf.add_run()
        r_pf.font.name = "Times New Roman"
        r_pf.font.size = Pt(10.5)
        docx_add_page_number_field(r_pf)

        def add_docx_prelim_title(txt):
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_before = Pt(4)
            p.paragraph_format.space_after = Pt(16)
            r = p.add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(15)
            r.font.bold = True
            r.font.color.rgb = RGBColor(15, 39, 68)

        def add_docx_p(txt, bp=None, align="justify", sa=7, line_sp=1.5):
            p = doc.add_paragraph()
            p.alignment = (WD_ALIGN_PARAGRAPH.CENTER if align == "center"
                           else WD_ALIGN_PARAGRAPH.RIGHT if align == "right"
                           else WD_ALIGN_PARAGRAPH.JUSTIFY)
            p.paragraph_format.line_spacing = line_sp
            p.paragraph_format.space_after = Pt(sa)
            if bp:
                rb = p.add_run(bp + " ")
                rb.font.name = "Times New Roman"
                rb.font.size = Pt(12)
                rb.font.bold = True
            r = p.add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(12)
            return p

        def add_docx_table(headers, rows, col_widths=None):
            t = doc.add_table(rows=len(rows) + 1, cols=len(headers))
            t.alignment = WD_TABLE_ALIGNMENT.CENTER
            docx_repeat_header(t.rows[0])
            for i, h in enumerate(headers):
                c = t.rows[0].cells[i]
                c.text = ""
                docx_cell_bg(c, "0F2744")
                docx_cell_margins(c, top=100, bottom=100, left=120, right=120)
                docx_cell_borders(c, "0F2744", "6")
                p = c.paragraphs[0]
                p.paragraph_format.space_after = Pt(0)
                r = p.add_run(str(h))
                r.font.name = "Times New Roman"
                r.font.size = Pt(10)
                r.font.bold = True
                r.font.color.rgb = RGBColor(255, 255, 255)
            for ri, row in enumerate(rows):
                tr = t.rows[ri + 1]
                docx_cant_split(tr)
                bg = "F8FAFC" if ri % 2 == 1 else "FFFFFF"
                for ci, val in enumerate(row):
                    c = tr.cells[ci]
                    c.text = ""
                    docx_cell_bg(c, bg)
                    docx_cell_margins(c, top=80, bottom=80, left=110, right=110)
                    docx_cell_borders(c, "CBD5E1", "4")
                    p = c.paragraphs[0]
                    p.paragraph_format.space_after = Pt(0)
                    p.paragraph_format.line_spacing = 1.15
                    r = p.add_run(str(val))
                    r.font.name = "Times New Roman"
                    r.font.size = Pt(9.5)
            if col_widths:
                total_w = sum(col_widths)
                for row in t.rows:
                    for ci, w in enumerate(col_widths):
                        row.cells[ci].width = Inches(6.02 * (w / total_w))
            sp = doc.add_paragraph()
            sp.paragraph_format.space_after = Pt(4)

        def add_docx_leader_list(entries):
            t = doc.add_table(rows=len(entries), cols=2)
            t.alignment = WD_TABLE_ALIGNMENT.CENTER
            for idx, (lvl, label, pg) in enumerate(entries):
                row = t.rows[idx]
                docx_cant_split(row)
                c0, c1 = row.cells[0], row.cells[1]
                c0.width = Inches(5.32)
                c1.width = Inches(0.70)
                docx_cell_margins(c0, top=45, bottom=45, left=40 + lvl * 180, right=40)
                docx_cell_margins(c1, top=45, bottom=45, left=40, right=40)
                p0 = c0.paragraphs[0]
                p0.paragraph_format.space_after = Pt(0)
                r0 = p0.add_run(label)
                r0.font.name = "Times New Roman"
                r0.font.size = Pt(11 if lvl == 0 else 10.5)
                r0.font.bold = (lvl == 0)
                p1 = c1.paragraphs[0]
                p1.alignment = WD_ALIGN_PARAGRAPH.RIGHT
                p1.paragraph_format.space_after = Pt(0)
                r1 = p1.add_run(str(pg))
                r1.font.name = "Times New Roman"
                r1.font.size = Pt(11 if lvl == 0 else 10.5)
                r1.font.bold = (lvl == 0)

        # Bonafide Certificate
        add_docx_prelim_title("BONAFIDE CERTIFICATE")
        p_bon = doc.add_paragraph()
        p_bon.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_bon.paragraph_format.line_spacing = 1.75
        p_bon.paragraph_format.space_after = Pt(42)
        for txt, b in [
            ("This is to certify that the project report titled ", False),
            (f"“{self.project_title}”", True),
            (" is a bonafide work carried out by ", False),
            (f"{self.student_name} ({self.reg_no})", True),
            (f" under my supervision for the award of the Degree of {self.degree_title.title()}. "
             "To my knowledge the work reported herein is the original work done by the candidate.", False),
        ]:
            r = p_bon.add_run(txt)
            r.font.name = "Times New Roman"
            r.font.size = Pt(12)
            r.font.bold = b

        stbl = doc.add_table(rows=3, cols=2)
        stbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        sigs = [
            ("PROJECT GUIDE\n\n\nDr. A. Thirumurthi Raja\nAssistant Professor,\nSRMIST-DDE,",
             "HEAD OF DEPARTMENT\n\n\nDr. A. Thirumurthi Raja\nProgram Coordinator,\nDepartment of Computer Applications,\nDirectorate of Distance Education (DDE),\nSRMIST,"),
            ("\n\n", "\n\n"),
            ("INTERNAL EXAMINER", "EXTERNAL EXAMINER"),
        ]
        for ri, (ltxt, rtxt) in enumerate(sigs):
            for ci, val in enumerate([ltxt, rtxt]):
                cell = stbl.rows[ri].cells[ci]
                cell.width = Inches(3.01)
                p = cell.paragraphs[0]
                p.paragraph_format.line_spacing = 1.35
                for li, ln in enumerate(val.split("\n")):
                    run = p.add_run((ln + "\n") if li < len(val.split("\n")) - 1 else ln)
                    run.font.name = "Times New Roman"
                    run.font.size = Pt(11.5)
                    run.font.bold = (li == 0 or "Thirumurthi" in ln or "EXAMINER" in ln)
        doc.add_page_break()

        # Declaration
        add_docx_prelim_title("DECLARATION")
        add_docx_p(
            f"I, {self.student_name} ({self.reg_no}), hereby declare that the project report entitled "
            f"“{self.project_title}” submitted to SRM Institute of Science and Technology in partial "
            f"fulfillment of the requirements for the award of the Degree of {self.degree_title.title()} "
            f"is a record of original software engineering and academic project work carried out by me "
            f"under the supervision and guidance of Dr. A. Thirumurthi Raja, M.C.A., M.E., Ph.D., "
            f"Assistant Professor, Department of Computer Applications, Directorate of Distance Education, "
            f"SRMIST. I further declare that this project report or any part thereof has not been submitted "
            f"earlier to any other University or Institution for the award of any degree or diploma.",
            sa=48, line_sp=1.75
        )
        add_docx_p("Place: Chennai\nDate: November 2026", sa=6, line_sp=1.4)
        add_docx_p(f"{self.student_name}\n({self.reg_no})\nSignature of the Candidate", align="right", sa=6, line_sp=1.4)
        doc.add_page_break()

        # Acknowledgement
        add_docx_prelim_title("ACKNOWLEDGEMENT")
        for ap in [
            "With profound gratitude to the ALMIGHTY, I take this chance to thank the people who helped me to complete this project.",
            "I take this as a right opportunity to say THANKS to my parents who are there to stand with me always with the words “YOU CAN”.",
            "I am thankful to Dr. T. R. Paarivendhar, Chancellor, SRM Institute of Science & Technology who gave us the platform to establish ourselves and reach greater heights.",
            "I express my deep sense of gratitude to the Director Dr. Manoranjan Pon Ram for his wholehearted support and encouragement.",
            "I earnestly thank Dr. A. Thirumurthi Raja, Programme Coordinator, BCA/MCA — Directorate of Distance Education, SRM Institute of Science & Technology who always encourages us to pursue novel engineering solutions.",
            "It is my delight to thank my project guide Dr. A. Thirumurthi Raja, Assistant Professor, Department of Computer Applications (DDE), for his help, support, encouragement, suggestions, and guidance throughout the development phases of the project.",
            "I convey my sincere gratitude to all the faculty members of the department who extended their support through valuable comments and suggestions during the reviews.",
            "A great note of gratitude to friends and colleagues who helped in carrying out this project work to a successful completion.",
        ]:
            add_docx_p(ap, sa=10)
        add_docx_p(self.student_name, align="right", bp="", sa=6)
        doc.add_page_break()

        fixed_prelim = [
            (1, "Title Page", "i"),
            (1, "Bonafide Certificate", page_map.get("prelim_bonafide", "ii")),
            (1, "Declaration", page_map.get("prelim_declaration", "iii")),
            (1, "Acknowledgement", page_map.get("prelim_ack", "iv")),
        ]

        for blk in self.prelim_blocks:
            kind = blk[0]
            if kind == "prelim_h1":
                add_docx_prelim_title(blk[2])
            elif kind == "p":
                _, text, bp, align, sa = blk
                add_docx_p(text, bp=bp, align=align, sa=sa)
            elif kind == "raw_table":
                _, headers, rows, cw = blk
                add_docx_table(headers, rows, cw)
            elif kind == "page_break":
                doc.add_page_break()
            elif kind == "toc_placeholder":
                add_docx_prelim_title("TABLE OF CONTENTS")
                all_toc = list(fixed_prelim) + [(lvl, tlabel, page_map.get(tkey, "1")) for lvl, tkey, tlabel in self.toc_entries]
                add_docx_leader_list(all_toc)
                doc.add_page_break()
            elif kind == "lof_placeholder":
                add_docx_prelim_title("LIST OF FIGURES")
                add_docx_leader_list([(1, f"Figure {fn}: {cap}", page_map.get(fk, "1")) for fk, fn, cap in self.lof_entries])
                doc.add_page_break()
            elif kind == "lot_placeholder":
                add_docx_prelim_title("LIST OF TABLES")
                add_docx_leader_list([(1, f"Table {tn}: {cap}", page_map.get(tk, "1")) for tk, tn, cap in self.lot_entries])

        # ---- SECTION 2: MAIN BODY (Arabic Page 1+) ----
        sec_main = doc.add_section(WD_SECTION.NEW_PAGE)
        setup_sec(sec_main, fmt="decimal", start=1, border=False)
        sec_main.header.is_linked_to_previous = False
        sec_main.footer.is_linked_to_previous = False

        hp = sec_main.header.paragraphs[0]
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hr = hp.add_run(f"{self.short_title}  |  BCA Project Report (SRMIST)")
        hr.font.name = "Times New Roman"
        hr.font.size = Pt(9.5)
        hr.font.italic = True
        hr.font.color.rgb = RGBColor(71, 85, 105)

        fp = sec_main.footer.paragraphs[0]
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        fr1 = fp.add_run("Page ")
        fr1.font.name = "Times New Roman"
        fr1.font.size = Pt(10.5)
        fr2 = fp.add_run()
        fr2.font.name = "Times New Roman"
        fr2.font.size = Pt(10.5)
        docx_add_page_number_field(fr2)

        first_chap = True
        for blk in self.body_blocks:
            kind = blk[0]
            if kind == "chapter":
                _, key, chap_num, chap_title, _ = blk
                if not first_chap:
                    doc.add_page_break()
                first_chap = False
                if chap_num is not None:
                    p_cn = doc.add_paragraph()
                    p_cn.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    p_cn.paragraph_format.space_before = Pt(4)
                    p_cn.paragraph_format.space_after = Pt(4)
                    rcn = p_cn.add_run(f"CHAPTER {chap_num}")
                    rcn.font.name = "Times New Roman"
                    rcn.font.size = Pt(14)
                    rcn.font.bold = True
                    rcn.font.color.rgb = RGBColor(30, 58, 138)
                p_ct = doc.add_paragraph()
                p_ct.alignment = WD_ALIGN_PARAGRAPH.CENTER
                p_ct.paragraph_format.space_after = Pt(16)
                rct = p_ct.add_run(chap_title)
                rct.font.name = "Times New Roman"
                rct.font.size = Pt(16)
                rct.font.bold = True
                rct.font.color.rgb = RGBColor(15, 39, 68)
            elif kind == "h2":
                _, _, label = blk
                p = doc.add_paragraph()
                p.paragraph_format.keep_with_next = True
                p.paragraph_format.space_before = Pt(13)
                p.paragraph_format.space_after = Pt(6)
                r = p.add_run(label)
                r.font.name = "Times New Roman"
                r.font.size = Pt(14)
                r.font.bold = True
                r.font.color.rgb = RGBColor(15, 39, 68)
            elif kind == "h3":
                _, _, label = blk
                p = doc.add_paragraph()
                p.paragraph_format.keep_with_next = True
                p.paragraph_format.space_before = Pt(10)
                p.paragraph_format.space_after = Pt(4)
                r = p.add_run(label)
                r.font.name = "Times New Roman"
                r.font.size = Pt(12)
                r.font.bold = True
                r.font.color.rgb = RGBColor(30, 58, 138)
            elif kind == "p":
                _, text, bp, align, sa = blk
                add_docx_p(text, bp=bp, align=align, sa=sa)
            elif kind == "bullet":
                _, bp, text = blk
                p = doc.add_paragraph(style="List Bullet")
                p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
                p.paragraph_format.line_spacing = 1.4
                p.paragraph_format.space_after = Pt(5)
                if bp:
                    rb = p.add_run(bp + " ")
                    rb.font.name = "Times New Roman"
                    rb.font.size = Pt(12)
                    rb.font.bold = True
                rt = p.add_run(text)
                rt.font.name = "Times New Roman"
                rt.font.size = Pt(12)
            elif kind == "table":
                _, _, tbl_num, caption, headers, rows, cw = blk
                pcap = doc.add_paragraph()
                pcap.paragraph_format.keep_with_next = True
                pcap.paragraph_format.space_before = Pt(8)
                pcap.paragraph_format.space_after = Pt(4)
                rcap = pcap.add_run(f"Table {tbl_num}: {caption}")
                rcap.font.name = "Times New Roman"
                rcap.font.size = Pt(10.5)
                rcap.font.bold = True
                rcap.font.color.rgb = RGBColor(15, 39, 68)
                add_docx_table(headers, rows, cw)
            elif kind == "figure":
                _, _, fig_num, caption, img_path, width_in = blk
                if os.path.exists(img_path):
                    pi = doc.add_paragraph()
                    pi.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    pi.paragraph_format.keep_with_next = True
                    pi.paragraph_format.space_before = Pt(8)
                    pi.paragraph_format.space_after = Pt(4)
                    pi.add_run().add_picture(img_path, width=Inches(min(width_in, 5.95)))
                    pc = doc.add_paragraph()
                    pc.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    pc.paragraph_format.space_after = Pt(10)
                    rc = pc.add_run(f"Figure {fig_num}: {caption}")
                    rc.font.name = "Times New Roman"
                    rc.font.size = Pt(10.5)
                    rc.font.bold = True
                    rc.font.italic = True
                    rc.font.color.rgb = RGBColor(51, 65, 85)
            elif kind == "code":
                _, title, code_text = blk
                if title:
                    pt = doc.add_paragraph()
                    pt.paragraph_format.keep_with_next = True
                    pt.paragraph_format.space_before = Pt(8)
                    pt.paragraph_format.space_after = Pt(3)
                    rt = pt.add_run(title)
                    rt.font.name = "Times New Roman"
                    rt.font.size = Pt(10.5)
                    rt.font.bold = True
                    rt.font.color.rgb = RGBColor(30, 58, 138)
                ct = doc.add_table(rows=1, cols=1)
                ct.alignment = WD_TABLE_ALIGNMENT.CENTER
                cell = ct.rows[0].cells[0]
                cell.width = Inches(6.02)
                docx_cell_bg(cell, "F8FAFC")
                docx_cell_margins(cell, top=110, bottom=110, left=140, right=140)
                docx_cell_borders(cell, "CBD5E1", "4")
                cp = cell.paragraphs[0]
                cp.paragraph_format.line_spacing = 1.08
                cp.paragraph_format.space_after = Pt(0)
                cr = cp.add_run(code_text.strip("\n"))
                cr.font.name = "Consolas"
                cr.font.size = Pt(8.5)
                cr.font.color.rgb = RGBColor(15, 23, 42)
                sp = doc.add_paragraph()
                sp.paragraph_format.space_after = Pt(5)
            elif kind == "page_break":
                doc.add_page_break()

        doc.save(docx_path)

    def build_both(self, docx_path, pdf_path):
        print("Pass 1: Computing exact layout & page numbers...")
        tmp_pdf = pdf_path + ".pass1.tmp"
        page_map = self._build_pdf_once(tmp_pdf, {})
        if os.path.exists(tmp_pdf):
            os.remove(tmp_pdf)
        print(f"Pass 1 complete: tracked {len(page_map)} headings, figures, and tables.")
        print("Pass 2: Rendering final synchronized PDF and DOCX...")
        final_map = self._build_pdf_once(pdf_path, page_map)
        self._build_docx(docx_path, final_map)
        return final_map
