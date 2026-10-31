"""
Master Build Orchestrator for the Infeara Medical System (IMS)
SRMIST BCA Final Project Report (90+ Pages Target).
Produces synchronized DOCX and PDF deliverables in final-docs/.
"""
import os
import sys
import pymupdf

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, BASE_DIR)

from report_engine import AcademicReportBuilder
from sec_00_prelim_and_ch1_ch2 import populate_prelim_and_ch1_ch2
from sec_01_ch3_and_ch4 import populate_ch3_and_ch4
from sec_02_ch5_and_ch6 import populate_ch5_and_ch6
from sec_03_ch7_ch8_refs_appx import populate_ch7_ch8_refs_appx


def main():
    assets_dir = os.path.join(BASE_DIR, "assets")
    logo_path = os.path.join(assets_dir, "srm_logo_docx_image1.jpeg")
    if not os.path.exists(logo_path):
        logo_path = os.path.join(assets_dir, "srm_logo_pdf_0.jpeg")

    builder = AcademicReportBuilder(
        project_title="INFEARA MEDICAL SYSTEM (IMS): MULTI-TENANT CLINICAL MANAGEMENT, E-PRESCRIPTION AND PHARMACY POS PLATFORM",
        short_title="INFEARA MEDICAL SYSTEM (IMS)",
        student_name="VIGNESH M",
        reg_no="DC2431006010004",
        degree_title="BACHELOR OF COMPUTER APPLICATIONS",
        guide_name="Dr. A. THIRUMURTHI RAJA M.C.A., M.E., PH.D.,",
        guide_title="ASSISTANT PROFESSOR, SRMIST-DDE,",
        dept_line1="DEPARTMENT OF COMPUTER APPLICATIONS",
        dept_line2="DIRECTORATE OF DISTANCE EDUCATION",
        month_year="NOV 2026",
        srm_logo_path=logo_path,
    )

    populate_prelim_and_ch1_ch2(builder)
    populate_ch3_and_ch4(builder, assets_dir)
    populate_ch5_and_ch6(builder, assets_dir)
    populate_ch7_ch8_refs_appx(builder)

    docx_out = os.path.join(BASE_DIR, "IMS_BCA_Project_Report.docx")
    pdf_out = os.path.join(BASE_DIR, "IMS_BCA_Project_Report.pdf")

    page_map = builder.build_both(docx_out, pdf_out)

    pdf = pymupdf.open(pdf_out)
    print(f"=== BUILD SUMMARY ===")
    print(f"DOCX Path : {docx_out} ({os.path.getsize(docx_out) / (1024*1024):.2f} MB)")
    print(f"PDF Path  : {pdf_out} ({os.path.getsize(pdf_out) / (1024*1024):.2f} MB)")
    print(f"Total PDF Pages: {len(pdf)}")
    for lvl, key, label in builder.toc_entries:
        if lvl == 0:
            print(f"  {label:<52} -> Page {page_map.get(key, '?')}")


if __name__ == "__main__":
    main()
