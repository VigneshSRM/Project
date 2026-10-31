"""
Master Build Orchestrator for the Infeara Medical System (IMS)
SRMIST BCA Final Project Report — Exact Format of
'example docs/DC2332306010017_SECURE_DATA_SHARING_USING_RSA (2).pdf'
(Minimum 90+ Pages Target, Synchronized PDF and DOCX Deliverables).
"""
import os
import sys
import pymupdf

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(BASE_DIR)
sys.path.insert(0, BASE_DIR)

from report_engine import AcademicReportBuilder
from sec_00_prelim_and_ch1_ch2 import populate_abstract_ch1_ch2
from sec_01_ch3_and_ch4 import populate_ch3_and_ch4
from sec_02_ch5_and_ch6 import populate_ch5_and_ch6


def main():
    assets_dir = os.path.join(BASE_DIR, "assets")
    logo_path = os.path.join(assets_dir, "srm_logo_pdf_0.jpeg")
    if not os.path.exists(logo_path):
        logo_path = os.path.join(assets_dir, "srm_logo_docx_image1.jpeg")

    builder = AcademicReportBuilder(
        project_title="INFEARA MEDICAL SYSTEM (IMS)",
        abstract_heading_title="Infeara Medical System (IMS): Multi-Tenant Clinical, e-Prescription and Pharmacy POS Platform",
        student_name="VIGNESH M",
        reg_no="DC2431006010004",
        degree_title="BACHELOR OF COMPUTER APPLICATIONS",
        guide_upper="Dr. A. THIRUMURTHI RAJA M.C.A., M.E., PH.D.,",
        guide_mixed="Dr.A.Thirumurthi Raja",
        month_year="NOV 2026",
        srm_logo_path=logo_path,
    )

    populate_abstract_ch1_ch2(builder, assets_dir)
    populate_ch3_and_ch4(builder)
    populate_ch5_and_ch6(builder, assets_dir, REPO_ROOT)

    docx_out = os.path.join(BASE_DIR, "IMS_BCA_Project_Report.docx")
    pdf_out = os.path.join(BASE_DIR, "IMS_BCA_Project_Report.pdf")

    page_map = builder.build_both(docx_out, pdf_out)

    pdf = pymupdf.open(pdf_out)
    print(f"=== BUILD SUMMARY ===")
    print(f"DOCX Path : {docx_out} ({os.path.getsize(docx_out) / (1024*1024):.2f} MB)")
    print(f"PDF Path  : {pdf_out} ({os.path.getsize(pdf_out) / (1024*1024):.2f} MB)")
    print(f"Total PDF Pages: {len(pdf)}")
    for item in builder.toc_entries:
        if item["show_page"] and item["key"]:
            print(f"  {item['label']:<48} -> Page {page_map.get(item['key'], '?')}")
        else:
            print(f"  {item['label']}")


if __name__ == "__main__":
    main()
