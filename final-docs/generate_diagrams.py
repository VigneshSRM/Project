"""
Generate all 11 publication-grade architectural/UML diagrams (300 DPI)
and 10 high-resolution UI screens matching the exact IMS CSS design tokens
for the SRMIST BCA Final Project Report on Infeara Medical System (IMS).
"""
import os
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image, ImageDraw, ImageFont

ASSETS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')
os.makedirs(ASSETS_DIR, exist_ok=True)

plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'Helvetica']


def box(ax, x, y, w, h, title, subtitle="", fc='#e0f2fe', ec='#0284c7',
        t_size=9.0, s_size=7.3, t_col='#0f172a', s_col='#334155', pad=0.35, lw=1.4):
    ax.add_patch(patches.FancyBboxPatch(
        (x, y), w, h, boxstyle=f"round,pad={pad}", fc=fc, ec=ec, lw=lw, zorder=2
    ))
    if subtitle:
        ax.text(x + w / 2, y + h * 0.66, title, ha='center', va='center',
                fontsize=t_size, fontweight='bold', color=t_col, zorder=3)
        ax.text(x + w / 2, y + h * 0.30, subtitle, ha='center', va='center',
                fontsize=s_size, color=s_col, linespacing=1.25, zorder=3)
    else:
        ax.text(x + w / 2, y + h / 2, title, ha='center', va='center',
                fontsize=t_size, fontweight='bold', color=t_col, linespacing=1.25, zorder=3)


def arrow(ax, x1, y1, x2, y2, label="", col='#334155', lw=1.4, ls='-',
          lbl_offset=(0, 1.2), fontsize=7.2, bidirectional=False):
    style = '<->' if bidirectional else '->'
    ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle=style, color=col, lw=lw, linestyle=ls,
                                shrinkA=0, shrinkB=0), zorder=4)
    if label:
        mx, my = (x1 + x2) / 2 + lbl_offset[0], (y1 + y2) / 2 + lbl_offset[1]
        ax.text(mx, my, label, ha='center', va='center', fontsize=fontsize,
                fontweight='bold', color='#1e293b',
                bbox=dict(boxstyle='round,pad=0.18', fc='white', ec='none', alpha=0.88),
                zorder=5)


# ==============================================================================
# 1. SYSTEM ARCHITECTURE DIAGRAM (6-Tier Modular Monolith)
# ==============================================================================
def gen_system_architecture():
    fig, ax = plt.subplots(figsize=(13, 9.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "Infeara Medical System (IMS) — Six-Tier Modular Monolith Architecture",
            ha='center', va='center', fontsize=14.5, fontweight='bold', color='#0f2744')

    # Tier 1: Client
    box(ax, 4, 86, 92, 7.5,
        "TIER 1 — CLIENT PRESENTATION LAYER (Desktop & Tablet Web Browsers)",
        "HTML5 Semantic Views  |  Corporate ERP CSS3 Design Tokens (app.css)  |  Progressive ES6 (app.js)  |  SVG Icon Sprite",
        fc='#e0f2fe', ec='#0284c7', t_size=10, s_size=8, t_col='#075985')
    arrow(ax, 50, 85.6, 50, 82.2, "HTTPS / Session Cookie + CSRF Token")

    # Tier 2: Security
    box(ax, 4, 73.5, 92, 8.0,
        "TIER 2 — SECURITY & GATEWAY FILTER CHAIN (Spring Security 6.x)",
        "LoginThrottleFilter (5 fails / 15m) -> UsernamePasswordAuthFilter (BCrypt) -> ClinicStatusFilter -> 6-Role RBAC Guard",
        fc='#fef3c7', ec='#d97706', t_size=10, s_size=8, t_col='#92400e')
    arrow(ax, 50, 73.1, 50, 69.8)

    # Tier 3: WebMVC Controllers
    ax.add_patch(patches.FancyBboxPatch((4, 55.5), 92, 13.5, boxstyle="round,pad=0.4",
                                        fc='#dbeafe', ec='#1d4ed8', lw=1.5, zorder=2))
    ax.text(50, 66.8, "TIER 3 — WEB MVC CONTROLLER & THYMELEAF LAYOUT TIER (Spring WebMVC + Layout Dialect)",
            ha='center', va='center', fontsize=10, fontweight='bold', color='#1e3a8a', zorder=3)
    portals = [
        ("Setup & Auth\n/setup, /login\n/register, /invite", 6.5),
        ("Super Admin\n/admin/clinics\n/pharmacies, /smtp", 21.8),
        ("Clinic Portal\n/clinic/patients\n/link-requests", 37.1),
        ("Doctor Portal\n/doctor/consult\n/prescriptions", 52.4),
        ("Patient Portal\n/patient/clinics\n/book, /history", 67.7),
        ("Pharmacy POS\n/pharmacy/pos\n/prices, /finance", 83.0),
    ]
    for txt, px in portals:
        box(ax, px, 56.8, 12.8, 7.6, txt, fc='#ffffff', ec='#60a5fa', t_size=7.1, pad=0.25, lw=1.0)
    arrow(ax, 50, 55.0, 50, 51.6)

    # Tier 4: Domain Services
    ax.add_patch(patches.FancyBboxPatch((4, 33.5), 92, 17.5, boxstyle="round,pad=0.4",
                                        fc='#dcfce7', ec='#15803d', lw=1.5, zorder=2))
    ax.text(50, 48.8, "TIER 4 — TRANSACTIONAL BUSINESS SERVICE LAYER (@Service, @Transactional, Domain Events)",
            ha='center', va='center', fontsize=10, fontweight='bold', color='#14532d', zorder=3)
    svcs = [
        ("Identity & Cryptography", "• SHA-256 AuthTokens\n• AES-256-GCM Cipher\n• PasswordPolicy (10+)\n• AuditService Logger", 6.5),
        ("Patient Linking Engine", "• Staff ID Verification\n• Duplicate DOB Guard\n• Non-Destructive Merge\n• History Preservation", 29.5),
        ("Clinical & e-Rx Core", "• SlotService + Pessimistic\n• Draft/Final Consults\n• Immutable Addenda\n• 32-Char CSPRNG Rx", 52.5),
        ("Pharmacy POS & Ledger", "• Case-Sensitive Verify\n• Over-Dispense Guard\n• Auto-Remember Prices\n• Auto Finance Income", 75.5),
    ]
    for title, sub, sx in svcs:
        box(ax, sx, 35.0, 20.0, 11.5, title, sub, fc='#ffffff', ec='#4ade80', t_size=8.0, s_size=7.0, pad=0.3, lw=1.0)
    arrow(ax, 50, 33.0, 50, 29.6)

    # Tier 5: Persistence
    box(ax, 4, 20.5, 92, 8.5,
        "TIER 5 — DATA ACCESS & ORM LAYER (Spring Data JPA + Hibernate 6 + HikariCP + Flyway)",
        "24 JPA Repositories  |  open-in-view=false  |  Pessimistic Row Locks (UPDLOCK) & @Version  |  Flyway V001–V009",
        fc='#f3e8ff', ec='#7e22ce', t_size=10, s_size=8, t_col='#581c87')
    arrow(ax, 34, 20.0, 34, 15.6, "JDBC / T-SQL")
    arrow(ax, 79, 20.0, 79, 15.6, "Outbox Poll (5s)")

    # Tier 6: Storage & Async Worker
    box(ax, 4, 4.5, 58, 10.5,
        "TIER 6A — RELATIONAL DATABASE (SQL Server 2022 / H2 Mode)",
        "27 Normalized Tables + vw_PrescriptionFulfilment  |  Latin1_General_100_BIN2 Rx Code\nFiltered Unique Slot Index (UX_Appt_Slot)  |  ROWVERSION Optimistic Concurrency",
        fc='#ffedd5', ec='#c2410c', t_size=9.2, s_size=7.4, t_col='#9a3412')

    box(ax, 65, 4.5, 31, 10.5,
        "TIER 6B — ASYNC EMAIL WORKER",
        "EmailDispatchWorker (@Scheduled)\nDB Outbox + Idempotency Keys\n1m / 5m / 15m Retry Backoff -> SMTP",
        fc='#ffe4e6', ec='#be123c', t_size=9.2, s_size=7.4, t_col='#9f1239')

    plt.tight_layout()
    out = os.path.join(ASSETS_DIR, 'architecture_diagram.png')
    plt.savefig(out, dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 2. SECURITY & FILTER CHAIN PIPELINE DIAGRAM
# ==============================================================================
def gen_security_pipeline():
    fig, ax = plt.subplots(figsize=(12.5, 6.8), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 95, "IMS Defense-in-Depth Security Filter Chain & Cryptographic Controls",
            ha='center', va='center', fontsize=13.5, fontweight='bold', color='#0f2744')

    steps = [
        (3, 62, 16, 22, "1. HTTP Request", "Client IP Extraction\nCSRF Token Check\nSession Cookie", '#e0f2fe', '#0284c7'),
        (23, 62, 16, 22, "2. Rate & Throttle", "AuthRateLimiter &\nLoginThrottleFilter\n(5 fails / 15 min)", '#fef3c7', '#d97706'),
        (43, 62, 16, 22, "3. Auth Provider", "DatabaseUserDetails\nBCrypt Verification\nEmail Verified Check", '#dcfce7', '#15803d'),
        (63, 62, 16, 22, "4. Clinic Guard", "ClinicStatusFilter\nInstant Logout if\nClinic SUSPENDED", '#ffe4e6', '#be123c'),
        (83, 62, 14, 22, "5. RBAC Router", "6-Role Matcher\n/admin /clinic\n/doctor /patient\n/pharmacy", '#ede9fe', '#6d28d9'),
    ]
    for x, y, w, h, t, s, fc, ec in steps:
        box(ax, x, y, w, h, t, s, fc=fc, ec=ec, t_size=8.4, s_size=7.0)
    for x1, x2 in [(19.4, 22.6), (39.4, 42.6), (59.4, 62.6), (79.4, 82.6)]:
        arrow(ax, x1, 73, x2, 73)

    # Bottom cryptographic & audit pillars
    pillars = [
        (4, 14, 28, 34, "Cryptographic Token Vault",
         "• 256-bit SecureRandom raw URL tokens\n• Stored strictly as BINARY(32) SHA-256\n• Single-use SELECT ... WITH (UPDLOCK)\n• Purpose-scoped TTLs:\n  VERIFY_EMAIL (24h), RESET_PWD (60m)\n  STAFF_INVITE (72h), EMAIL_CHANGE (24h)",
         '#f8fafc', '#475569'),
        (36, 14, 28, 34, "At-Rest Secret & Rx Protection",
         "• AES-256-GCM SecretCipher for SMTP\n  credentials (96-bit IV + 128-bit Tag)\n• 32-char CSPRNG Prescription Code\n  (Unambiguous 32-symbol alphabet)\n• SQL Collation Latin1_General_100_BIN2\n• SHA-256 pharmacy attempt hashing",
         '#f8fafc', '#475569'),
        (68, 14, 28, 34, "Immutable Compliance Audit Trail",
         "• AuditLogs records actor, role, IP,\n  patient_id, resource_id & outcome\n• Outcomes: SUCCESS, FAILURE, DENIED\n• Ownership resolved from Session User\n  (never client-supplied patientId)\n• Custom 403 / 404 / 500 error views",
         '#f8fafc', '#475569'),
    ]
    for x, y, w, h, t, s, fc, ec in pillars:
        box(ax, x, y, w, h, t, s, fc=fc, ec=ec, t_size=8.6, s_size=6.9, t_col='#0f2744')

    arrow(ax, 31, 61.5, 18, 48.8, "Token Verify")
    arrow(ax, 51, 61.5, 50, 48.8, "Secret/Rx Guard")
    arrow(ax, 89, 61.5, 82, 48.8, "Audit Events")

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'security_pipeline.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 3. DFD LEVEL 0 (CONTEXT DIAGRAM)
# ==============================================================================
def gen_dfd_level0():
    fig, ax = plt.subplots(figsize=(12, 7.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 95, "Data Flow Diagram — Level 0 (System Context Diagram)",
            ha='center', va='center', fontsize=14, fontweight='bold', color='#0f2744')

    # Central Process 0.0
    circle = patches.Ellipse((50, 50), 30, 26, fc='#dbeafe', ec='#1d4ed8', lw=2.2, zorder=2)
    ax.add_patch(circle)
    ax.text(50, 52, "Process 0.0\nInfeara Medical System\n(IMS Core Platform)",
            ha='center', va='center', fontsize=10, fontweight='bold', color='#1e3a8a', zorder=3)
    ax.text(50, 42.5, "Spring Boot 4.1.2 + SQL Server", ha='center', va='center', fontsize=7.5, color='#334155', zorder=3)

    # 5 External Entities
    box(ax, 4, 74, 21, 12, "SUPER ADMIN", "Platform Governance", fc='#fef3c7', ec='#d97706', pad=0.2)
    box(ax, 4, 18, 21, 12, "CLINIC ADMIN", "Front-Desk & Masters", fc='#dcfce7', ec='#15803d', pad=0.2)
    box(ax, 75, 74, 21, 12, "DOCTOR", "Clinical Practitioner", fc='#e0f2fe', ec='#0284c7', pad=0.2)
    box(ax, 75, 18, 21, 12, "PHARMACY STAFF", "POS & Finance", fc='#ffe4e6', ec='#be123c', pad=0.2)
    box(ax, 38, 3, 24, 11, "PATIENT", "Self-Service Portal", fc='#f3e8ff', ec='#7e22ce', pad=0.2)

    arrow(ax, 25.5, 79, 37.5, 59, "Clinic/Pharmacy Enrollment, SMTP", lbl_offset=(-4, 4.5), bidirectional=True)
    arrow(ax, 25.5, 25, 37.5, 42, "Schedules, Link Merge, Check-In", lbl_offset=(-5, -4.5), bidirectional=True)
    arrow(ax, 74.5, 79, 62.5, 59, "Consult Notes, Addenda, 32-Char e-Rx", lbl_offset=(5, 4.5), bidirectional=True)
    arrow(ax, 74.5, 25, 62.5, 42, "Rx Verify, POS Sale, Ledger Entry", lbl_offset=(5, -4.5), bidirectional=True)
    arrow(ax, 50, 14.5, 50, 36.5, "Registration, Slot Booking, Rx History", lbl_offset=(14.5, 0), bidirectional=True)

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'dfd_level0.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 4. DFD LEVEL 1 (SUBSYSTEM DECOMPOSITION)
# ==============================================================================
def gen_dfd_level1():
    fig, ax = plt.subplots(figsize=(13, 8.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 96, "Data Flow Diagram — Level 1 (IMS Functional Decomposition)",
            ha='center', va='center', fontsize=14, fontweight='bold', color='#0f2744')

    def proc(x, y, num, title):
        ax.add_patch(patches.Ellipse((x, y), 20, 14, fc='#e0f2fe', ec='#0284c7', lw=1.6, zorder=2))
        ax.text(x, y + 3.2, num, ha='center', va='center', fontsize=8, fontweight='bold', color='#0369a1', zorder=3)
        ax.text(x, y - 1.5, title, ha='center', va='center', fontsize=8, fontweight='bold', color='#0f172a', zorder=3)

    def store(x, y, code, name):
        ax.add_patch(patches.Rectangle((x - 11, y - 3.5), 22, 7, fc='#fef3c7', ec='none', zorder=2))
        ax.plot([x - 11, x + 11], [y + 3.5, y + 3.5], color='#b45309', lw=1.6, zorder=3)
        ax.plot([x - 11, x + 11], [y - 3.5, y - 3.5], color='#b45309', lw=1.6, zorder=3)
        ax.plot([x - 11, x - 11], [y - 3.5, y + 3.5], color='#b45309', lw=1.6, zorder=3)
        ax.text(x, y, f"{code}: {name}", ha='center', va='center', fontsize=7.5, fontweight='bold', color='#78350f', zorder=4)

    proc(15, 76, "Process 1.0", "Identity, RBAC &\nPatient Linking")
    proc(50, 76, "Process 2.0", "Clinic Discovery &\nSlot Booking")
    proc(85, 76, "Process 3.0", "Consultation &\nAddenda Engine")

    store(15, 53, "D1", "Users, Patients & Links")
    store(50, 53, "D2", "Schedules & Appointments")
    store(85, 53, "D3", "Consultations & Addenda")

    proc(20, 29, "Process 4.0", "32-Char e-Rx\nIssuance & Outbox")
    proc(54, 29, "Process 5.0", "Pharmacy Verify &\nPOS Billing")
    proc(86, 29, "Process 6.0", "Finance Ledger &\nAudit Reporting")

    store(35, 8, "D4", "Prescriptions & Dispenses")
    store(75, 8, "D5", "PharmacyFinance & AuditLogs")

    # Forward process & store arrows (xy is destination!)
    arrow(ax, 25.2, 76, 39.8, 76, "Verified Patient")
    arrow(ax, 60.2, 76, 74.8, 76, "Checked-In Appt")
    arrow(ax, 15, 68.8, 15, 56.8, "R/W", bidirectional=True, lbl_offset=(3.5, 0))
    arrow(ax, 50, 68.8, 50, 56.8, "Lock Slot", bidirectional=True, lbl_offset=(5, 0))
    arrow(ax, 85, 68.8, 85, 56.8, "Draft/Final", bidirectional=True, lbl_offset=(5.2, 0))

    arrow(ax, 78, 49.2, 27, 34.5, "Finalised Consult", lbl_offset=(-4, 2.5))
    arrow(ax, 30.2, 29, 43.8, 29, "32-Char Rx Code")
    arrow(ax, 64.2, 29, 75.8, 29, "Sale Income")

    arrow(ax, 24, 21.8, 32, 11.8, "Save Rx", lbl_offset=(-4.5, 0))
    arrow(ax, 50, 21.8, 39, 11.8, "Dispense Items", bidirectional=True, lbl_offset=(6.5, 0))
    arrow(ax, 84, 21.8, 77, 11.8, "Post Ledger / Audit", bidirectional=True, lbl_offset=(7.5, 0))

    plt.tight_layout()
    out1 = os.path.join(ASSETS_DIR, 'dfd_level1.png')
    out_legacy = os.path.join(ASSETS_DIR, 'dfd_level0_level1.png')
    plt.savefig(out1, dpi=300, bbox_inches='tight')
    plt.savefig(out_legacy, dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 5. DFD LEVEL 2 (PHARMACY POS & LEDGER AUTOMATION)
# ==============================================================================
def gen_dfd_level2_pos():
    fig, ax = plt.subplots(figsize=(12.5, 7.2), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 95, "Data Flow Diagram — Level 2 (Process 5.0: Pharmacy POS & Auto-Ledger Sync)",
            ha='center', va='center', fontsize=13.5, fontweight='bold', color='#0f2744')

    box(ax, 3, 72, 17, 13, "Pharmacist", "Scans 32-Char Code", fc='#ffe4e6', ec='#be123c', pad=0.2)
    box(ax, 26, 72, 20, 13, "5.1 Binary Rx Lookup", "Exact Case-Sensitive\nStatus & Expiry Check", fc='#e0f2fe', ec='#0284c7', pad=0.2)
    box(ax, 53, 72, 20, 13, "5.2 Qty Guard", "Calculate Remaining\nBlock Over-Dispense", fc='#e0f2fe', ec='#0284c7', pad=0.2)
    box(ax, 79, 72, 18, 13, "5.3 Auto-Pricer", "Pre-fill Catalog Price\nTax & Discount Calc", fc='#e0f2fe', ec='#0284c7', pad=0.2)

    box(ax, 26, 36, 22, 14, "5.4 Atomic Dispense", "Lock Rx Items (UPDLOCK)\nSave Receipt & Update\nRemembered Unit Price", fc='#dcfce7', ec='#15803d', pad=0.2)
    box(ax, 58, 36, 24, 14, "5.5 Finance Auto-Post", "Create INCOME Row Linked\nto dispense_id & Log\nAuditOutcome.SUCCESS", fc='#dcfce7', ec='#15803d', pad=0.2)

    box(ax, 6, 8, 25, 11, "D4a: Prescriptions &\nPrescriptionItems", fc='#fef3c7', ec='#d97706', pad=0.2)
    box(ax, 37, 8, 25, 11, "D4b: PharmacyMedicinePrices\n& PrescriptionDispenses", fc='#fef3c7', ec='#d97706', pad=0.2)
    box(ax, 68, 8, 26, 11, "D5: PharmacyFinanceEntries\n& PharmacyVerifications", fc='#fef3c7', ec='#d97706', pad=0.2)

    arrow(ax, 20.4, 78.5, 25.6, 78.5, "Rx Code")
    arrow(ax, 46.4, 78.5, 52.6, 78.5, "Valid Rx")
    arrow(ax, 73.4, 78.5, 78.6, 78.5, "Cart Lines")
    arrow(ax, 88, 71.5, 42, 50.5, "Confirmed Tender & Payment Method", lbl_offset=(8, 2.5))
    arrow(ax, 48.5, 43, 57.5, 43, "Receipt Total")

    arrow(ax, 34, 71.5, 18, 19.5, "Read Rx", lbl_offset=(-5, 2))
    arrow(ax, 37, 35.5, 48, 19.5, "Save Sale & Price", bidirectional=True, lbl_offset=(7, 0))
    arrow(ax, 70, 35.5, 80, 19.5, "Insert INCOME", lbl_offset=(6.5, 0))

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'dfd_level2_pos.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 6. USE CASE DIAGRAM
# ==============================================================================
def gen_use_case():
    fig, ax = plt.subplots(figsize=(13, 9.2), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "Infeara Medical System (IMS) — Unified UML Use Case Model",
            ha='center', va='center', fontsize=14.5, fontweight='bold', color='#0f2744')

    ax.add_patch(patches.FancyBboxPatch((23, 4), 54, 89, boxstyle="round,pad=0.8",
                                        fc='#f8fafc', ec='#334155', lw=1.8, linestyle='--', zorder=1))
    ax.text(50, 91, "IMS Multi-Tenant Application Boundary", ha='center', va='center',
            fontsize=10.5, fontweight='bold', color='#475569')

    def actor(x, y, name, col):
        ax.add_patch(patches.Circle((x, y + 2.5), 1.6, fc='#ffffff', ec=col, lw=1.8, zorder=3))
        ax.plot([x, x], [y + 0.9, y - 2.6], color=col, lw=1.8, zorder=3)
        ax.plot([x - 2.3, x + 2.3], [y - 0.3, y - 0.3], color=col, lw=1.8, zorder=3)
        ax.plot([x, x - 1.9], [y - 2.6, y - 5.2], color=col, lw=1.8, zorder=3)
        ax.plot([x, x + 1.9], [y - 2.6, y - 5.2], color=col, lw=1.8, zorder=3)
        ax.text(x, y - 7.6, name, ha='center', va='center', fontsize=8.5, fontweight='bold', color=col)

    actor(9, 80, "Super Admin", '#b45309')
    actor(9, 50, "Clinic Admin", '#15803d')
    actor(9, 20, "Doctor", '#0369a1')
    actor(91, 68, "Patient", '#7e22ce')
    actor(91, 26, "Pharmacy Admin\n& Pharmacist", '#be123c')

    ucs = [
        (50, 85.5, "UC-01: Bootstrap System (/setup) & Configure Encrypted SMTP", '#fef3c7', '#d97706'),
        (50, 79.0, "UC-02: Enroll Clinics/Pharmacies & Dispatch Staff Invites", '#fef3c7', '#d97706'),
        (50, 72.5, "UC-03: Monitor Platform KPIs, Suspensions & Global Patients", '#fef3c7', '#d97706'),
        (50, 65.5, "UC-04: Manage Doctors, Shift Schedules & CSV Medicine Formulary", '#dcfce7', '#15803d'),
        (50, 59.0, "UC-05: Verify Patient Identity & Approve/Merge Link Requests", '#dcfce7', '#15803d'),
        (50, 52.5, "UC-06: Operate Front-Desk Check-In, Reschedule & Cancellation", '#dcfce7', '#15803d'),
        (50, 45.5, "UC-07: Self-Register, Verify Email & Manage Linked Profile", '#f3e8ff', '#7e22ce'),
        (50, 39.0, "UC-08: Discover Active Clinics & Book Conflict-Free Time Slots", '#f3e8ff', '#7e22ce'),
        (50, 32.5, "UC-09: Conduct Clinical Consultation, Finalise & Append Addenda", '#dbeafe', '#2563eb'),
        (50, 26.0, "UC-10: Issue 32-Character Case-Sensitive Digital Prescription", '#dbeafe', '#2563eb'),
        (50, 19.0, "UC-11: Scan & Verify 32-Char Rx Code (Expiry / Remaining Guard)", '#ffe4e6', '#e11d48'),
        (50, 12.5, "UC-12: Execute Auto-Priced POS Billing & Partial Dispensing", '#ffe4e6', '#e11d48'),
        (50,  6.2, "UC-13: Reconcile Auto-Posted POS Income & Expense Finance Ledger", '#ffe4e6', '#e11d48'),
    ]
    for x, y, txt, fc, ec in ucs:
        ax.add_patch(patches.Ellipse((x, y), 46, 5.0, fc=fc, ec=ec, lw=1.2, zorder=3))
        ax.text(x, y, txt, ha='center', va='center', fontsize=7.4, fontweight='bold', color='#0f172a', zorder=4)

    for uy in [85.5, 79.0, 72.5]:
        ax.plot([13, 27], [80, uy], color='#b45309', lw=1.1, zorder=2)
    for uy in [65.5, 59.0, 52.5]:
        ax.plot([13, 27], [50, uy], color='#15803d', lw=1.1, zorder=2)
    for uy in [32.5, 26.0]:
        ax.plot([13, 27], [20, uy], color='#0369a1', lw=1.1, zorder=2)
    for uy in [59.0, 45.5, 39.0, 26.0]:
        ax.plot([87, 73], [68, uy], color='#7e22ce', lw=1.1, zorder=2)
    for uy in [19.0, 12.5, 6.2]:
        ax.plot([87, 73], [26, uy], color='#be123c', lw=1.1, zorder=2)

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'use_case_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 7. CLASS DIAGRAM
# ==============================================================================
def gen_class_diagram():
    fig, ax = plt.subplots(figsize=(13.5, 9.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97.2, "IMS Domain Model — UML Entity Class Diagram (JPA / Hibernate 6)",
            ha='center', va='center', fontsize=14, fontweight='bold', color='#0f2744')

    def cls_box(x, y, w, h, name, attrs, methods):
        ax.add_patch(patches.Rectangle((x, y), w, h, fc='#ffffff', ec='#1e293b', lw=1.2, zorder=2))
        ax.add_patch(patches.Rectangle((x, y + h - 3.6), w, 3.6, fc='#0f2744', ec='#1e293b', lw=1.0, zorder=3))
        ax.text(x + w / 2, y + h - 1.8, f"«Entity» {name}", ha='center', va='center',
                fontsize=7.3, fontweight='bold', color='#ffffff', zorder=4)
        cy = y + h - 5.1
        for a in attrs:
            ax.text(x + 0.8, cy, a, ha='left', va='center', fontsize=6.1, color='#1e293b', zorder=4)
            cy -= 1.65
        ax.plot([x, x + w], [cy + 0.6, cy + 0.6], color='#94a3b8', lw=0.8, zorder=3)
        cy -= 0.7
        for m in methods:
            ax.text(x + 0.8, cy, m, ha='left', va='center', fontsize=6.0, color='#1d4ed8', style='italic', zorder=4)
            cy -= 1.55

    cls_box(2, 69, 21, 24, "User",
            ["- userId: Long", "- email: String", "- pendingEmail: String", "- passwordHash: String",
             "- role: Role", "- status: UserStatus", "- rowVer: byte[]"],
            ["+ isEmailVerified(): boolean", "+ activate(): void"])

    cls_box(27, 69, 21, 24, "Clinic",
            ["- clinicId: Long", "- publicUuid: UUID", "- name, city, state: String",
             "- openingHours: String", "- status: ClinicStatus"],
            ["+ isActive(): boolean", "+ suspend(): void"])

    cls_box(52, 69, 21, 24, "Patient",
            ["- patientId: Long", "- publicPatientId: String", "- firstName, lastName: String",
             "- dob: LocalDate", "- sourceFlag: PatientSource", "- mergedInto: Patient"],
            ["+ fullName(): String", "+ isMerged(): boolean"])

    cls_box(77, 69, 21, 24, "PatientLinkRequest",
            ["- linkRequestId: Long", "- method: LinkMethod", "- status: LinkStatus",
             "- clinicPatientRecord: Patient", "- resolvedBy: User", "- resolutionNote: String"],
            ["+ approve(user): void", "+ reject(note): void"])

    cls_box(2, 36, 21, 25, "Doctor & Schedule",
            ["- doctorId: Long", "- specialization: String", "- weekday: int (0..6)",
             "- startTime, endTime: LocalTime", "- slotMinutes: int (15)"],
            ["+ generateSlots(date): List", "+ isValidSlot(ts): boolean"])

    cls_box(27, 36, 21, 25, "Appointment",
            ["- appointmentId: Long", "- publicAppointmentId: String", "- slotStart, slotEnd: LocalDateTime",
             "- status: AppointmentStatus", "- cancelReason: String", "- rowVer: byte[]"],
            ["+ checkIn(): void", "+ startConsult(): void"])

    cls_box(52, 36, 21, 25, "Consultation",
            ["- consultationId: Long", "- diagnosis: String", "- notes: String",
             "- status: ConsultationStatus", "- finalisedAt: LocalDateTime"],
            ["+ finalise(): void", "+ appendAddendum(): void"])

    cls_box(77, 36, 21, 25, "Prescription & Item",
            ["- prescriptionId: Long", "- prescriptionCode: String(32)", "- expiryDate: LocalDateTime",
             "- quantity, qtyDispensed: int", "- lifetime: boolean"],
            ["+ getRemainingQty(): int", "+ isExpired(): boolean"])

    cls_box(6, 4, 25, 24, "Pharmacy & PriceList",
            ["- pharmacyId: Long", "- licenceNumber: String", "- status: PharmacyStatus",
             "- medicine: String", "- unitPrice: BigDecimal"],
            ["+ verifyCode(code): Outcome", "+ rememberPrice(): void"])

    cls_box(38, 4, 26, 24, "PrescriptionDispense",
            ["- dispenseId: Long", "- verificationId: UUID", "- subtotal, discount: BigDecimal",
             "- taxRate, total: BigDecimal", "- paymentMethod: String"],
            ["+ computeTotals(): void", "+ voidSale(reason): void"])

    cls_box(71, 4, 25, 24, "PharmacyFinanceEntry",
            ["- entryId: Long", "- entryType: String(INCOME)", "- amount: BigDecimal",
             "- dispense: PrescriptionDispense", "- entryDate: LocalDate"],
            ["+ postPosIncome(): void", "+ recordExpense(): void"])

    arrow(ax, 23, 81, 27, 81, "N:1")
    arrow(ax, 23, 85, 52, 85, "")
    arrow(ax, 73, 81, 77, 81, "1:N")
    arrow(ax, 23, 48, 27, 48, "1:N")
    arrow(ax, 48, 48, 52, 48, "1:1")
    arrow(ax, 73, 48, 77, 48, "1:1")
    arrow(ax, 62, 69, 38, 61, "")
    arrow(ax, 87, 36, 51, 28, "1:N")
    arrow(ax, 31, 16, 38, 16, "1:N")
    arrow(ax, 64, 16, 71, 16, "1:1")

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'class_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 8. SEQUENCE DIAGRAM
# ==============================================================================
def gen_sequence_diagram():
    fig, ax = plt.subplots(figsize=(13, 9.2), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "IMS End-to-End Sequence Diagram: Booking, Consult, e-Rx & POS Auto-Ledger",
            ha='center', va='center', fontsize=13.5, fontweight='bold', color='#0f2744')

    lifelines = [
        ("Patient\nBrowser", 10, '#f3e8ff', '#7e22ce'),
        ("Doctor\nWorkspace", 29, '#dbeafe', '#1d4ed8'),
        ("Spring Boot\nServices", 51, '#dcfce7', '#15803d'),
        ("SQL Server 2022\n(ClinicFlow_Dev)", 73, '#ffedd5', '#c2410c'),
        ("Pharmacy POS\nTerminal", 91, '#ffe4e6', '#be123c'),
    ]
    for name, x, fc, ec in lifelines:
        box(ax, x - 7.5, 89.5, 15, 5.2, name, fc=fc, ec=ec, t_size=7.8, pad=0.2)
        ax.plot([x, x], [4, 89.5], color='#94a3b8', ls='--', lw=1.1, zorder=1)

    msgs = [
        (85.0, 10, 51, "1. POST /patient/book (clinicId, doctorId, slotStart)", False),
        (80.0, 51, 73, "2. findForUpdate(doctorId) + INSERT Appointments (UX_Appt_Slot)", False),
        (75.0, 29, 51, "3. POST /doctor/appointments/{id}/finalise (diagnosis, notes)", False),
        (70.0, 51, 73, "4. UPDATE Consultations SET status='FINAL', finalised_at=UTC", False),
        (64.5, 29, 51, "5. POST /doctor/appointments/{id}/prescription (items, validDays)", False),
        (59.0, 51, 73, "6. INSERT Prescriptions (32-char CSPRNG code) + PrescriptionItems", False),
        (53.5, 51, 10, "7. Queue EmailLogs Outbox & Render 32-Char Prescription Code", True),
        (47.5, 10, 91, "8. Present 32-Character Case-Sensitive Prescription ID at Counter", False),
        (42.0, 91, 51, "9. POST /pharmacy/pos (Analyze 32-char code)", False),
        (36.5, 51, 73, "10. Binary Collation Lookup + Check Expiry & Remaining Qty", False),
        (31.0, 51, 91, "11. Return Verified Cart Pre-Filled from PharmacyMedicinePrices", True),
        (25.0, 91, 51, "12. POST /pharmacy/dispense (qty, prices, discount, tax, UPI/Cash)", False),
        (19.0, 51, 73, "13. Lock RxItems + UPDATE qty_dispensed + INSERT Dispense Receipt", False),
        (13.0, 51, 73, "14. Upsert Remembered Prices + Auto-INSERT PharmacyFinanceEntries", False),
        (7.0,  51, 91, "15. Render Printable Sale Receipt & Updated Live Counter KPI", True),
    ]
    for y, x1, x2, txt, ret in msgs:
        col = '#059669' if ret else '#1e40af'
        ls = '--' if ret else '-'
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
                    arrowprops=dict(arrowstyle='->', color=col, lw=1.3, linestyle=ls))
        mx = (x1 + x2) / 2
        ax.text(mx, y + 1.1, txt, ha='center', va='bottom', fontsize=6.9, fontweight='bold',
                color='#0f172a', bbox=dict(boxstyle='round,pad=0.12', fc='white', ec='none', alpha=0.85))

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'sequence_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 9. ACTIVITY DIAGRAM
# ==============================================================================
def gen_activity_diagram():
    fig, ax = plt.subplots(figsize=(11.5, 9.8), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97.5, "IMS Clinical, Patient Linking & POS Dispensing Activity Workflow",
            ha='center', va='center', fontsize=13.5, fontweight='bold', color='#0f2744')

    ax.add_patch(patches.Circle((50, 93.2), 1.6, fc='#0f172a', ec='#0f172a'))
    arrow(ax, 50, 91.5, 50, 88.2)

    box(ax, 24, 83, 52, 5, "Patient Self-Registers, Verifies Email (24h Token) & Requests Clinic Link", fc='#e0f2fe', ec='#0284c7', t_size=8.2)
    arrow(ax, 50, 82.8, 50, 79.2)

    dia = patches.Polygon([[50, 79], [59, 75.5], [50, 72], [41, 75.5]], fc='#fef3c7', ec='#d97706', lw=1.4, zorder=3)
    ax.add_patch(dia)
    ax.text(50, 75.5, "Staff Verifies\nID & DOB?", ha='center', va='center', fontsize=7.0, fontweight='bold', color='#92400e', zorder=4)

    # Reject branch
    ax.plot([41, 17, 17], [75.5, 75.5, 69], color='#be123c', lw=1.3)
    arrow(ax, 17, 69, 17, 66.2, col='#be123c')
    ax.text(29, 76.8, "Mismatch / Reject", fontsize=7.2, fontweight='bold', color='#be123c', ha='center')
    box(ax, 5, 60.5, 24, 5.5, "Reject Link Request &\nRecord Audit Entry", fc='#ffe4e6', ec='#be123c', t_size=7.6)

    # Approve branch
    ax.plot([59, 76, 76], [75.5, 75.5, 69], color='#15803d', lw=1.3)
    arrow(ax, 76, 69, 76, 66.2, col='#15803d')
    ax.text(68, 76.8, "Verified", fontsize=7.2, fontweight='bold', color='#15803d', ha='center')
    box(ax, 57, 60.5, 38, 5.5, "Approve Account Record OR Non-Destructively\nMerge into Existing Clinic Patient Master", fc='#dcfce7', ec='#15803d', t_size=7.6)

    ax.plot([76, 76, 50], [60.3, 56.5, 56.5], color='#334155', lw=1.3)
    arrow(ax, 50, 56.5, 50, 53.5)

    steps = [
        (50, 48.5, "Book Available Doctor Time Slot (Pessimistic Lock + UX_Appt_Slot)", '#e0f2fe', '#0284c7'),
        (50, 39.5, "Doctor Conducts Consultation -> Finalises Clinical Record (+ Optional Addenda)", '#dbeafe', '#1d4ed8'),
        (50, 30.5, "Issue 32-Character Case-Sensitive e-Prescription & Queue Email Notification", '#f3e8ff', '#7e22ce'),
        (50, 21.5, "Pharmacy POS Scans Code -> Auto-Fills Remembered Prices -> Dispenses Qty", '#ffe4e6', '#be123c'),
        (50, 12.5, "Generate Sale Receipt & Auto-Post Net Total as INCOME in Finance Ledger", '#dcfce7', '#15803d'),
    ]
    for idx, (cx, cy, txt, fc, ec) in enumerate(steps):
        box(ax, cx - 28, cy, 56, 4.8, txt, fc=fc, ec=ec, t_size=8.0)
        if idx < len(steps) - 1:
            arrow(ax, 50, cy - 0.2, 50, steps[idx + 1][1] + 5.0)

    arrow(ax, 50, 12.3, 50, 8.2)
    ax.add_patch(patches.Circle((50, 6.2), 2.0, fc='#ffffff', ec='#0f172a', lw=1.6))
    ax.add_patch(patches.Circle((50, 6.2), 1.1, fc='#0f172a', ec='#0f172a'))

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'activity_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 10. STATE MACHINE DIAGRAM (APPOINTMENT & PRESCRIPTION LIFECYCLES)
# ==============================================================================
def gen_state_machine():
    fig, ax = plt.subplots(figsize=(12.5, 7.8), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 96, "IMS Domain Lifecycle State Machines — Appointment & e-Prescription Fulfilment",
            ha='center', va='center', fontsize=13, fontweight='bold', color='#0f2744')

    # Top panel: AppointmentStatus
    ax.add_patch(patches.FancyBboxPatch((3, 53), 94, 38, boxstyle="round,pad=0.4", fc='#f8fafc', ec='#94a3b8', lw=1.2))
    ax.text(50, 88.5, "A. Appointment Lifecycle State Machine (AppointmentStatus)", ha='center', va='center', fontsize=10, fontweight='bold', color='#1e3a8a')

    box(ax, 6, 70, 14, 8, "BOOKED", fc='#e0f2fe', ec='#0284c7', t_size=8.2)
    box(ax, 25, 70, 15, 8, "CONFIRMED", fc='#e0f2fe', ec='#0284c7', t_size=8.2)
    box(ax, 45, 70, 15, 8, "CHECKED_IN", fc='#fef3c7', ec='#d97706', t_size=8.2)
    box(ax, 65, 70, 16, 8, "IN_CONSULT", fc='#ede9fe', ec='#7c3aed', t_size=8.2)
    box(ax, 85, 70, 10, 8, "COMPLETED", fc='#dcfce7', ec='#15803d', t_size=7.6)

    box(ax, 25, 56, 18, 7, "CANCELLED\n(Releases UX_Appt_Slot)", fc='#ffe4e6', ec='#be123c', t_size=7.2)
    box(ax, 55, 56, 18, 7, "NO_SHOW\n(Releases UX_Appt_Slot)", fc='#f1f5f9', ec='#475569', t_size=7.2)

    arrow(ax, 20.3, 74, 24.7, 74)
    arrow(ax, 40.3, 74, 44.7, 74)
    arrow(ax, 60.3, 74, 64.7, 74)
    arrow(ax, 81.3, 74, 84.7, 74)
    arrow(ax, 13, 69.6, 28, 63.3, "Cancel", lbl_offset=(-3, -1))
    arrow(ax, 33, 69.6, 33, 63.3, "")
    arrow(ax, 52, 69.6, 62, 63.3, "Mark No-Show", lbl_offset=(6, 0))

    # Bottom panel: Prescription & Fulfilment
    ax.add_patch(patches.FancyBboxPatch((3, 6), 94, 41, boxstyle="round,pad=0.4", fc='#f8fafc', ec='#94a3b8', lw=1.2))
    ax.text(50, 44, "B. Prescription & Pharmacy Fulfilment State Machine (vw_PrescriptionFulfilment)", ha='center', va='center', fontsize=10, fontweight='bold', color='#14532d')

    box(ax, 6, 25, 18, 9, "VALID /\nNOT_DISPENSED", "qty_dispensed = 0", fc='#e0f2fe', ec='#0284c7', t_size=7.8, s_size=6.8)
    box(ax, 33, 25, 22, 9, "VALID /\nPARTIALLY_DISPENSED", "0 < disp < qty", fc='#fef3c7', ec='#d97706', t_size=7.8, s_size=6.8)
    box(ax, 64, 25, 22, 9, "VALID /\nFULLY_DISPENSED", "all items disp == qty", fc='#dcfce7', ec='#15803d', t_size=7.8, s_size=6.8)

    box(ax, 20, 9, 22, 8, "CANCELLED\n(Clinic/Doctor Revoked)", fc='#ffe4e6', ec='#be123c', t_size=7.5)
    box(ax, 55, 9, 24, 8, "EXPIRED\n(UTC > expiry_date)", fc='#f1f5f9', ec='#475569', t_size=7.5)

    arrow(ax, 24.4, 29.5, 32.6, 29.5, "Partial POS Sale")
    arrow(ax, 55.4, 29.5, 63.6, 29.5, "Remaining Qty Filled")
    arrow(ax, 15, 34.4, 75, 34.4, "Full Single-Visit Dispense", lbl_offset=(0, 2.2))
    arrow(ax, 15, 24.6, 28, 17.4, "Cancel Rx", lbl_offset=(-4, 0))
    arrow(ax, 44, 24.6, 63, 17.4, "TTL Elapsed", lbl_offset=(5, 0))

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'state_machine_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# 11. ER DIAGRAM (ALL 27 TABLES GROUPED CLEANLY, ZERO OVERLAPPING LINES)
# ==============================================================================
def gen_er_diagram():
    fig, ax = plt.subplots(figsize=(14, 10), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97.2, "IMS Relational Database Entity-Relationship (ER) Schema (27 Tables across V001–V009)",
            ha='center', va='center', fontsize=13.5, fontweight='bold', color='#0f2744')

    def tbl(x, y, w, h, name, cols, hdr='#0f2744'):
        ax.add_patch(patches.Rectangle((x, y), w, h, fc='#ffffff', ec='#334155', lw=1.1, zorder=2))
        ax.add_patch(patches.Rectangle((x, y + h - 3.0), w, 3.0, fc=hdr, ec='#334155', lw=1.0, zorder=3))
        ax.text(x + w / 2, y + h - 1.5, name, ha='center', va='center',
                fontsize=7.0, fontweight='bold', color='#ffffff', zorder=4)
        cy = y + h - 4.4
        for kind, cname in cols:
            col = '#b45309' if kind == 'PK' else ('#15803d' if kind == 'FK' else '#334155')
            wt = 'bold' if kind in ('PK', 'FK') else 'normal'
            prefix = f"{kind} " if kind else "   "
            ax.text(x + 0.6, cy, prefix + cname, ha='left', va='center',
                    fontsize=5.9, fontweight=wt, color=col, zorder=4)
            cy -= 1.52

    # Row 1: Identity & Multi-Tenant Enrollment (y=72..93)
    tbl(2, 72, 17.5, 21, "Users", [("PK", "user_id"), ("FK", "clinic_id"), ("", "email (UQ)"), ("", "pending_email"), ("", "password_hash"), ("", "role, status"), ("", "row_ver")], '#1e3a8a')
    tbl(22, 72, 17.5, 21, "AuthTokens", [("PK", "token_id"), ("FK", "user_id"), ("", "purpose (4 types)"), ("", "token_hash BIN(32)"), ("", "expires_at"), ("", "used_at")], '#1e3a8a')
    tbl(42, 72, 18.0, 21, "Clinics & Enrollments", [("PK", "clinic_id"), ("", "public_uuid (UQ)"), ("", "name, city, state"), ("", "opening_hours"), ("", "status"), ("FK", "enrolled_by (CE)")], '#0f766e')
    tbl(62.5, 72, 17.5, 21, "Patients & ClinicPts", [("PK", "patient_id"), ("FK", "user_id"), ("FK", "merged_into_id"), ("", "public_patient_id"), ("", "dob, blood_group"), ("", "source_flag")], '#7e22ce')
    tbl(82, 72, 16.5, 21, "PatientLinkRequests", [("PK", "link_request_id"), ("FK", "patient_id"), ("FK", "clinic_id"), ("FK", "clinic_pt_rec_id"), ("FK", "resolved_by"), ("", "status, note")], '#7e22ce')

    # Row 2: Clinical Operations & e-Prescriptions (y=39..64)
    tbl(2, 39, 17.5, 24, "Doctors & Links", [("PK", "doctor_id"), ("", "name, spec"), ("FK", "clinic_id (CD)"), ("", "fee (CD)"), ("FK", "user_id (DUL)"), ("", "weekday, slot_min"), ("", "start/end_time")], '#0369a1')
    tbl(22, 39, 17.5, 24, "Appointments", [("PK", "appointment_id"), ("", "public_appt_id"), ("FK", "patient_id"), ("FK", "clinic_id"), ("FK", "doctor_id"), ("", "slot_start (UX)"), ("", "status, row_ver")], '#0369a1')
    tbl(42, 39, 18.0, 24, "Consultations & Add.", [("PK", "consultation_id"), ("FK", "appointment_id"), ("", "diagnosis, notes"), ("", "status (DRAFT/FIN)"), ("", "finalised_at"), ("PK", "addendum_id (CA)"), ("FK", "author_user_id")], '#0369a1')
    tbl(62.5, 39, 17.5, 24, "Prescriptions", [("PK", "prescription_id"), ("", "rx_code CHAR(32)"), ("", "BIN2 Collation"), ("FK", "consultation_id"), ("FK", "patient_id, doc_id"), ("", "expiry_date"), ("", "status")], '#6d28d9')
    tbl(82, 39, 16.5, 24, "PrescriptionItems", [("PK", "prescription_item_id"), ("FK", "prescription_id"), ("", "medicine, dosage"), ("", "freq, duration"), ("", "quantity, unit"), ("", "qty_dispensed"), ("", "lifetime (BIT)")], '#6d28d9')

    # Row 3: Formulary, Pharmacy POS, Finance & Audit/Outbox (y=6..31)
    tbl(2, 6, 17.5, 24, "ClinicMedicines", [("PK", "clinic_medicine_id"), ("FK", "clinic_id"), ("", "medicine (UQ)"), ("", "dosage (UQ)"), ("", "default_frequency"), ("", "default_duration"), ("", "default_form")], '#0f766e')
    tbl(22, 6, 17.5, 24, "Pharmacies & Users", [("PK", "pharmacy_id"), ("", "licence_number"), ("", "status"), ("PK", "pharmacy_user_id"), ("FK", "user_id (UQ)"), ("", "role (ADMIN/PHARM)"), ("PK", "price_id (PMP)")], '#be123c')
    tbl(42, 6, 18.0, 24, "PharmacyVerifications", [("PK", "verification_id"), ("FK", "pharmacy_id"), ("FK", "pharmacy_user_id"), ("FK", "prescription_id"), ("", "attempt_hash(32)"), ("", "outcome (6 vals)"), ("", "ip_address")], '#be123c')
    tbl(62.5, 6, 17.5, 24, "PrescriptionDispenses", [("PK", "dispense_id"), ("FK", "prescription_id"), ("FK", "verification_id"), ("", "subtotal, discount"), ("", "tax_rate, total"), ("", "payment_method"), ("PK", "dispense_item_id")], '#be123c')
    tbl(82, 6, 16.5, 24, "Finance & Audit/Mail", [("PK", "entry_id (PFE)"), ("FK", "dispense_id"), ("", "entry_type, amount"), ("PK", "audit_id (AL)"), ("PK", "email_log_id (EL)"), ("", "idempotency_key"), ("PK", "settings_id (SS)")], '#334155')

    # Clean corridor routing between rows (corridors at y=67.5 and y=34.5, and horizontal gaps!)
    arrow(ax, 19.6, 82, 21.9, 82)
    arrow(ax, 39.6, 82, 41.9, 82, col='#64748b')
    arrow(ax, 80.1, 82, 81.9, 82)

    # Row 2 horizontal chains
    arrow(ax, 19.6, 51, 21.9, 51)
    arrow(ax, 39.6, 51, 41.9, 51)
    arrow(ax, 60.1, 51, 62.4, 51)
    arrow(ax, 80.1, 51, 81.9, 51)

    # Row 3 horizontal chains
    arrow(ax, 39.6, 18, 41.9, 18)
    arrow(ax, 60.1, 18, 62.4, 18)
    arrow(ax, 80.1, 18, 81.9, 18)

    # Clean vertical corridor links (no box crossings!)
    ax.plot([71, 71, 31, 31], [71.8, 67.5, 67.5, 63.3], color='#475569', lw=1.2)
    arrow(ax, 31, 65, 31, 63.2)
    ax.plot([51, 51, 10.5, 10.5], [71.8, 68.8, 68.8, 63.3], color='#475569', lw=1.2)
    ax.plot([71, 71], [38.8, 30.3], color='#475569', lw=1.2)
    arrow(ax, 71, 33, 71, 30.2)
    ax.plot([51, 51], [38.8, 34.5], color='#475569', lw=1.0, ls='--')

    plt.tight_layout()
    plt.savefig(os.path.join(ASSETS_DIR, 'er_diagram.png'), dpi=300, bbox_inches='tight')
    plt.close()


# ==============================================================================
# PART B: PIXEL-ACCURATE HIGH-DPI APPLICATION UI SCREENS (1440 x 900)
# Matching exact app.css & test_pos.png design system!
# ==============================================================================
FONT_DIR = "/usr/share/fonts/truetype/dejavu"
F_REG = lambda sz: ImageFont.truetype(f"{FONT_DIR}/DejaVuSans.ttf", sz)
F_BOLD = lambda sz: ImageFont.truetype(f"{FONT_DIR}/DejaVuSans-Bold.ttf", sz)
F_MONO = lambda sz: ImageFont.truetype(f"{FONT_DIR}/DejaVuSansMono-Bold.ttf", sz)
F_MONO_R = lambda sz: ImageFont.truetype(f"{FONT_DIR}/DejaVuSansMono.ttf", sz)


def draw_ims_shell(org_name, role_label, user_name, nav_items, active_idx, crumbs, page_title, badge=None, subtitle=""):
    W, H = 1440, 900
    img = Image.new("RGB", (W, H), "#F4F6F9")
    d = ImageDraw.Draw(img)

    # Topbar (#0F2744, h=56)
    d.rectangle([0, 0, W, 56], fill="#0F2744")
    # Hamburger
    for ly in [21, 27, 33]:
        d.line([22, ly, 38, ly], fill="#C9D6E8", width=2)
    # IMS Emerald Mark
    d.rounded_rectangle([56, 13, 86, 43], radius=7, fill="#0E8A8A")
    d.rectangle([69, 19, 73, 37], fill="#FFFFFF")
    d.rectangle([62, 26, 80, 30], fill="#FFFFFF")
    d.text((96, 13), "IMS", font=F_BOLD(17), fill="#FFFFFF")
    d.text((96, 32), "INFEARA MEDICAL SYSTEM", font=F_REG(9), fill="#9FB3C8")
    d.line([232, 16, 232, 40], fill="#2B4465", width=1)
    d.text((246, 20), org_name, font=F_REG(14), fill="#D9E2EC")

    # Right user bar
    d.text((1060, 20), role_label, font=F_REG(13), fill="#C9D6E8")
    d.ellipse([1210, 13, 1240, 43], fill="#1E4E8C")
    d.text((1220, 19), user_name[0].upper(), font=F_BOLD(14), fill="#FFFFFF")
    d.text((1248, 20), user_name, font=F_BOLD(13), fill="#FFFFFF")

    # Sidebar (#0F2744, w=248)
    d.rectangle([0, 56, 248, H], fill="#0F2744")
    d.line([0, 56, 248, 56], fill="#1C3554", width=1)
    d.text((20, 78), role_label.upper(), font=F_BOLD(11), fill="#7F93AD")

    ny = 112
    for idx, label in enumerate(nav_items):
        if idx == active_idx:
            d.rounded_rectangle([12, ny, 236, ny + 40], radius=6, fill="#1E4E8C")
            d.ellipse([26, ny + 15, 36, ny + 25], fill="#FFFFFF")
            d.text((46, ny + 11), label, font=F_BOLD(13), fill="#FFFFFF")
        else:
            d.ellipse([26, ny + 15, 36, ny + 25], outline="#8FA4BF", width=2)
            d.text((46, ny + 11), label, font=F_REG(13), fill="#C9D6E8")
        ny += 46

    d.line([16, H - 54, 232, H - 54], fill="#1C3554", width=1)
    d.text((20, H - 38), "IMS v0.1 · Infeara Medical System", font=F_REG(11), fill="#7F93AD")

    # Breadcrumb bar
    d.rectangle([248, 56, W, 96], fill="#FFFFFF")
    d.line([248, 96, W, 96], fill="#D9DEE5", width=1)
    bx = 276
    for i, c in enumerate(crumbs):
        is_last = (i == len(crumbs) - 1)
        col = "#1B2430" if is_last else "#1E4E8C"
        fnt = F_BOLD(13) if is_last else F_REG(13)
        d.text((bx, 68), c, font=fnt, fill=col)
        bx += int(d.textlength(c, font=fnt)) + 10
        if not is_last:
            d.text((bx, 68), "/", font=F_REG(13), fill="#8C96A4")
            bx += 14

    # Page Title
    d.text((276, 116), page_title, font=F_BOLD(23), fill="#1B2430")
    if badge:
        tw = int(d.textlength(page_title, font=F_BOLD(23)))
        bw = int(d.textlength(badge[0], font=F_BOLD(11))) + 20
        d.rounded_rectangle([276 + tw + 14, 120, 276 + tw + 14 + bw, 142], radius=11, fill=badge[1])
        d.text((276 + tw + 24, 124), badge[0], font=F_BOLD(11), fill=badge[2])
    if subtitle:
        d.text((276, 150), subtitle, font=F_REG(13), fill="#5B6573")

    return img, d


def card(d, x, y, w, h, title=None, right_badge=None):
    d.rounded_rectangle([x, y, x + w, y + h], radius=8, fill="#FFFFFF", outline="#D9DEE5", width=1)
    if title:
        d.text((x + 20, y + 16), title, font=F_BOLD(15), fill="#1B2430")
    if right_badge:
        bw = int(d.textlength(right_badge[0], font=F_BOLD(11))) + 18
        d.rounded_rectangle([x + w - bw - 20, y + 15, x + w - 20, y + 37], radius=10, fill=right_badge[1])
        d.text((x + w - bw - 11, y + 19), right_badge[0], font=F_BOLD(11), fill=right_badge[2])


def kpi_row(d, items, y=184):
    x = 276
    w = (1440 - 276 - 32 - (len(items) - 1) * 16) // len(items)
    for label, val, sub, accent in items:
        d.rounded_rectangle([x, y, x + w, y + 104], radius=8, fill="#FFFFFF", outline="#D9DEE5", width=1)
        d.rectangle([x, y + 8, x + 4, y + 96], fill=accent)
        d.text((x + 18, y + 14), label.upper(), font=F_BOLD(11), fill="#5B6573")
        d.text((x + 18, y + 36), val, font=F_BOLD(26), fill="#1B2430")
        d.text((x + 18, y + 76), sub, font=F_REG(12), fill="#5B6573")
        x += w + 16


def draw_table(d, x, y, w, headers, rows, col_x, pill_col=None):
    d.rectangle([x, y, x + w, y + 36], fill="#F8FAFC")
    d.line([x, y + 36, x + w, y + 36], fill="#D9DEE5", width=1)
    for hdr, cx in zip(headers, col_x):
        d.text((x + cx, y + 11), hdr.upper(), font=F_BOLD(11), fill="#5B6573")
    ry = y + 36
    for row in rows:
        for idx, (val, cx) in enumerate(zip(row, col_x)):
            if pill_col is not None and idx == pill_col:
                p_bg, p_fg = ("#E3F4EA", "#1B7F4B") if val in ("ACTIVE", "VERIFIED", "FINAL", "VALID", "COMPLETED", "APPROVED", "INCOME") else (
                    ("#FFF1DB", "#8A5300") if val in ("PENDING", "BOOKED", "CHECKED_IN", "DRAFT", "PARTIAL") else ("#E4EEFB", "#1A5FB4")
                )
                pw = int(d.textlength(val, font=F_BOLD(11))) + 18
                d.rounded_rectangle([x + cx, ry + 10, x + cx + pw, ry + 32], radius=10, fill=p_bg)
                d.text((x + cx + 9, ry + 14), val, font=F_BOLD(11), fill=p_fg)
            else:
                fnt = F_BOLD(12) if idx == 0 else F_REG(12)
                col = "#1E4E8C" if idx == 0 else "#1B2430"
                d.text((x + cx, ry + 13), val, font=fnt, fill=col)
        ry += 42
        d.line([x, ry, x + w, ry], fill="#ECEFF4", width=1)


def btn(d, x, y, label, bg="#1E4E8C", fg="#FFFFFF", w=None):
    bw = w or (int(d.textlength(label, font=F_BOLD(13))) + 32)
    d.rounded_rectangle([x, y, x + bw, y + 38], radius=6, fill=bg, outline="#D9DEE5" if bg == "#FFFFFF" else bg)
    d.text((x + 16, y + 10), label, font=F_BOLD(13), fill=fg)
    return bw


def input_field(d, x, y, w, label, val, mono=False):
    d.text((x, y), label, font=F_BOLD(12), fill="#1B2430")
    d.rounded_rectangle([x, y + 20, x + w, y + 58], radius=6, fill="#FFFFFF", outline="#B8C2CC", width=1)
    d.text((x + 12, y + 31), val, font=F_MONO_R(13) if mono else F_REG(13), fill="#1B2430")


def gen_all_ui_screens():
    # 1. Login & Auth Screen (/login)
    img = Image.new("RGB", (1440, 900), "#0F2744")
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 620, 900], fill="#0F2744")
    d.rectangle([620, 0, 1440, 900], fill="#F4F6F9")
    d.rounded_rectangle([64, 72, 108, 116], radius=9, fill="#0E8A8A")
    d.rectangle([83, 81, 89, 107], fill="#FFFFFF")
    d.rectangle([73, 91, 99, 97], fill="#FFFFFF")
    d.text((124, 74), "IMS · Infeara Medical System", font=F_BOLD(22), fill="#FFFFFF")
    d.text((124, 102), "Unified Multi-Tenant Clinical & Pharmacy ERP", font=F_REG(13), fill="#9FB3C8")
    d.text((64, 190), "Secure Healthcare\nOperations Across\nEvery Care Touchpoint.", font=F_BOLD(34), fill="#FFFFFF")
    bullets = [
        "• 6 Role-Scoped Workspaces with Session & CSRF Hardening",
        "• Staff-Verified Multi-Clinic Patient Identity & Record Merge",
        "• Conflict-Free Doctor Slot Scheduling (UX_Appt_Slot)",
        "• 32-Character Case-Sensitive Digital Prescriptions",
        "• Integrated Pharmacy Point-of-Sale & Automated Finance Ledger",
    ]
    by = 360
    for b in bullets:
        d.text((64, by), b, font=F_REG(14), fill="#C9D6E8")
        by += 38
    d.rounded_rectangle([760, 140, 1300, 760], radius=10, fill="#FFFFFF", outline="#D9DEE5", width=1)
    d.text((810, 185), "Sign in to your workspace", font=F_BOLD(24), fill="#1B2430")
    d.text((810, 222), "Enter your verified IMS email address and password.", font=F_REG(13), fill="#5B6573")
    input_field(d, 810, 275, 440, "Email address", "doctor@riverside.test")
    input_field(d, 810, 360, 440, "Password", "••••••••••••••")
    d.rounded_rectangle([810, 442, 828, 460], radius=4, fill="#1E4E8C")
    d.text((838, 443), "Remember this workstation for 7 days", font=F_REG(13), fill="#1B2430")
    d.text((1118, 443), "Forgot password?", font=F_BOLD(13), fill="#1E4E8C")
    btn(d, 810, 492, "Sign in securely", w=440)
    d.line([810, 566, 1250, 566], fill="#ECEFF4", width=1)
    d.text((810, 590), "New patient without an IMS account?", font=F_REG(13), fill="#5B6573")
    btn(d, 810, 622, "Create Patient Account (/register)", bg="#FFFFFF", fg="#1E4E8C", w=440)
    d.text((810, 695), "Protected by LoginThrottleFilter (5 attempts / 15m) & SHA-256 tokens", font=F_REG(11), fill="#5B6573")
    img.save(os.path.join(ASSETS_DIR, "ui_01_login_auth.png"))

    # 2. Super Admin Dashboard
    nav_admin = ["Dashboard", "Clinic Enrollment", "Pharmacy Enrollment", "Patient Masters", "Reports", "SMTP Settings"]
    img, d = draw_ims_shell("IMS Platform Operations", "Super Admin", "Vignesh Admin", nav_admin, 0,
                            ["Home", "Dashboard"], "Platform Governance Overview", ("LIVE GOVERNANCE", "#E3F4EA", "#1B7F4B"),
                            "Multi-tenant health, enrolled clinics & pharmacies, email outbox telemetry and recent security audit events")
    kpi_row(d, [
        ("Active Clinics", "8", "1 pending activation", "#1E4E8C"),
        ("Active Pharmacies", "6", "All licences verified", "#0E8A8A"),
        ("Verified Patients", "412", "38 multi-clinic linked", "#1B7F4B"),
        ("30-Day e-Prescriptions", "1,284", "99.4% outbox delivery", "#8A5300"),
    ])
    card(d, 276, 308, 1132, 556, "Recent Platform Audit & Governance Stream (dbo.AuditLogs)", ("IMMUTABLE LOG", "#E4EEFB", "#1A5FB4"))
    draw_table(d, 296, 360, 1092,
               ["Timestamp (UTC)", "Actor", "Role", "Action", "Resource", "Outcome"],
               [
                   ["2026-10-30 13:20", "owner@clinicflow.test", "SUPER_ADMIN", "CLINIC_ENROLLED", "Riverside Care Clinic", "ACTIVE"],
                   ["2026-10-30 13:15", "pharmacyadmin@central", "PHARMACY_ADMIN", "DISPENSE_COMPLETED", "SALE-000006 (Rs.12.50)", "COMPLETED"],
                   ["2026-10-30 12:58", "doctor@riverside.test", "DOCTOR", "PRESCRIPTION_ISSUED", "JUEB9AGPAKBT...MXRP", "VALID"],
                   ["2026-10-30 12:40", "clinicadmin@riverside", "CLINIC_ADMIN", "PATIENT_LINK_APPROVED", "PAT-2026-0014", "APPROVED"],
                   ["2026-10-30 12:12", "owner@clinicflow.test", "SUPER_ADMIN", "SMTP_SETTINGS_TESTED", "smtp.srmhealth.in:587", "VERIFIED"],
                   ["2026-10-30 11:50", "unknown@external", "ANONYMOUS", "AUTH_LOGIN_THROTTLED", "IP 103.48.19.44", "PENDING"],
               ],
               [14, 180, 385, 545, 770, 980], pill_col=5)
    img.save(os.path.join(ASSETS_DIR, "ui_02_superadmin_dashboard.png"))

    # 3. Super Admin Clinic Enrollment
    img, d = draw_ims_shell("IMS Platform Operations", "Super Admin", "Vignesh Admin", nav_admin, 1,
                            ["Home", "Clinic Enrollment"], "Enrolled Clinics & Tenant Status", ("8 ACTIVE", "#E3F4EA", "#1B7F4B"),
                            "Enroll healthcare facilities, issue 72-hour administrator invitations, or suspend compromised tenants immediately")
    btn(d, 1218, 118, "+ Enroll New Clinic")
    card(d, 276, 188, 1132, 676, "Registered Healthcare Facilities (dbo.Clinics & dbo.ClinicEnrollments)")
    draw_table(d, 296, 240, 1092,
               ["Clinic Name", "City / Area", "Contact Email", "Doctors", "Enrolled On", "Status"],
               [
                   ["Riverside Multispeciality Clinic", "Chennai · Adyar", "clinicadmin@riverside.test", "6 Doctors", "14 Oct 2026", "ACTIVE"],
                   ["Northside Family Healthcare", "Chennai · Anna Nagar", "admin@northside.in", "4 Doctors", "16 Oct 2026", "ACTIVE"],
                   ["SRM Community Polyclinic", "Kattankulathur", "desk@srmclinic.org", "9 Doctors", "19 Oct 2026", "ACTIVE"],
                   ["Southbay Heart & Diabetes Centre", "Chennai · Velachery", "ops@southbaymed.in", "5 Doctors", "22 Oct 2026", "ACTIVE"],
                   ["Metro Care Outpatient Center", "Tambaram", "invite@metrocare.in", "0 Doctors", "29 Oct 2026", "PENDING"],
               ],
               [14, 310, 520, 760, 875, 995], pill_col=5)
    img.save(os.path.join(ASSETS_DIR, "ui_03_admin_clinic_enrollment.png"))

    # 4. Clinic Admin Appointment Desk
    nav_clinic = ["Dashboard", "Patient Masters", "Link Requests", "Appointments", "Doctors", "Inventory", "Prescriptions"]
    img, d = draw_ims_shell("Riverside Multispeciality Clinic", "Clinic Administration", "Priya Nair", nav_clinic, 3,
                            ["Home", "Appointments"], "Front-Desk Appointment Queue", ("CONFLICT-FREE SLOTS", "#E4EEFB", "#1A5FB4"),
                            "Manage daily arrivals, confirm check-ins, monitor active consultations and handle cancellations")
    kpi_row(d, [
        ("Today's Slots", "28", "4 doctors on duty", "#1E4E8C"),
        ("Checked In", "9", "Waiting for doctor", "#8A5300"),
        ("Completed Today", "14", "12 e-Rx generated", "#1B7F4B"),
        ("Cancelled / No-Show", "2", "Slots auto-released", "#B3261E"),
    ])
    card(d, 276, 308, 1132, 556, "Scheduled Patient Appointments — 30 Oct 2026")
    draw_table(d, 296, 360, 1092,
               ["Appt ID", "Slot Time", "Patient", "Consulting Doctor", "Visit Reason", "Status"],
               [
                   ["APT-2026-00108", "10:00 – 10:15", "Vignesh M (PAT-2026-0014)", "Dr. R. Karthikeyan · Gen Med", "Seasonal fever & cough", "COMPLETED"],
                   ["APT-2026-00109", "10:15 – 10:30", "Ananya Krishnan (PAT-2026-0019)", "Dr. R. Karthikeyan · Gen Med", "BP & thyroid review", "CHECKED_IN"],
                   ["APT-2026-00110", "10:30 – 10:45", "Arjun Subramanian (PAT-2026-0022)", "Dr. Meera Sundaram · Derm", "Allergic rash follow-up", "CHECKED_IN"],
                   ["APT-2026-00111", "11:00 – 11:15", "Lakshmi Narayanan (PAT-2026-0008)", "Dr. S. Raghavan · Cardio", "Lipid profile consult", "BOOKED"],
                   ["APT-2026-00112", "11:15 – 11:30", "Deepak Varma (PAT-2026-0031)", "Dr. R. Karthikeyan · Gen Med", "Annual health check", "BOOKED"],
               ],
               [14, 165, 315, 575, 815, 985], pill_col=5)
    img.save(os.path.join(ASSETS_DIR, "ui_04_clinic_appointments.png"))

    # 5. Clinic Patient Link Verification & Merge
    img, d = draw_ims_shell("Riverside Multispeciality Clinic", "Clinic Administration", "Priya Nair", nav_clinic, 2,
                            ["Home", "Link Requests", "Request #LR-0042"], "Verify Patient Identity & Link Account",
                            ("PENDING VERIFICATION", "#FFF1DB", "#8A5300"),
                            "Staff must verify government ID & DOB before linking or merging onto an existing clinic master record")
    card(d, 276, 188, 548, 670, "Account Requesting Link (Patient-Entered)")
    fields_l = [
        ("Public Patient ID", "PAT-2026-0048"),
        ("Full Name", "Vignesh M"),
        ("Date of Birth (Verified)", "14 Aug 2004"),
        ("Login Email", "vm0920@srmist.edu.in (Verified)"),
        ("Mobile", "+91 98401 55210"),
        ("Blood Group / Allergies", "O+  ·  No known drug allergies"),
    ]
    fy = 242
    for k, v in fields_l:
        d.text((298, fy), k.upper(), font=F_BOLD(11), fill="#5B6573")
        d.text((298, fy + 18), v, font=F_BOLD(14), fill="#1B2430")
        d.line([298, fy + 46, 800, fy + 46], fill="#ECEFF4", width=1)
        fy += 58
    d.rounded_rectangle([298, 608, 800, 690], radius=6, fill="#FFF1DB", outline="#E6B800", width=1)
    d.text((314, 622), "Duplicate Warning — Matching DOB Found in Clinic Masters!", font=F_BOLD(12), fill="#8A5300")
    d.text((314, 646), "Existing walk-in record PAT-2026-0014 (Vignesh M, DOB 14 Aug 2004)\nhas 2 prior appointments and 1 active prescription.", font=F_REG(12), fill="#5B6573")

    card(d, 844, 188, 564, 670, "Staff Resolution & Non-Destructive Record Merge")
    input_field(d, 868, 244, 516, "Target Clinic Patient ID (optional — enter to merge walk-in history)", "PAT-2026-0014", mono=True)
    input_field(d, 868, 330, 516, "Resolution Note (recorded in AuditLogs & shown to patient)", "Verified SRM student ID card at front desk; merged walk-in chart.")
    d.rounded_rectangle([868, 418, 888, 438], radius=4, fill="#1B7F4B")
    d.text((898, 420), "I have verified the patient's photo ID and exact Date of Birth.", font=F_BOLD(12), fill="#1B2430")
    btn(d, 868, 472, "Approve & Merge Patient Record", bg="#1B7F4B", w=290)
    btn(d, 1174, 472, "Reject Request", bg="#B3261E", w=210)
    d.rounded_rectangle([868, 540, 1384, 825], radius=6, fill="#F8FAFC", outline="#D9DEE5", width=1)
    d.text((886, 558), "What happens during Non-Destructive Record Merge:", font=F_BOLD(13), fill="#1B2430")
    merge_notes = [
        "1. Account ownership transfers to PAT-2026-0014.",
        "2. All Appointments, Consultations & Prescriptions stay intact.",
        "3. Old row PAT-2026-0048 sets merged_into_patient_id = 14.",
        "4. AuditLogs writes PATIENT_RECORD_MERGED + PATIENT_LINK_APPROVED.",
    ]
    my = 594
    for mn in merge_notes:
        d.text((886, my), mn, font=F_REG(12), fill="#5B6573")
        my += 34
    img.save(os.path.join(ASSETS_DIR, "ui_05_clinic_link_merge.png"))

    # 6. Clinic Medicine Formulary & CSV Inventory
    img, d = draw_ims_shell("Riverside Multispeciality Clinic", "Clinic Administration", "Priya Nair", nav_clinic, 5,
                            ["Home", "Inventory"], "Clinic Medicine Formulary & Prescribing Defaults",
                            ("CSV IMPORT / EXPORT", "#E3F4EA", "#1B7F4B"),
                            "Maintain standardized drug dosages, frequencies and forms used to auto-complete doctor e-prescriptions")
    btn(d, 980, 118, "Download CSV Template", bg="#FFFFFF", fg="#1E4E8C")
    btn(d, 1210, 118, "Export Formulary CSV")
    card(d, 276, 188, 1132, 676, "Standardized Clinic Formulary (dbo.ClinicMedicines)")
    draw_table(d, 296, 240, 1092,
               ["Medicine Name", "Dosage", "Default Form", "Frequency", "Duration", "Qty"],
               [
                   ["Paracetamol", "650 mg", "Tablet", "1-0-1 (After food)", "5 days", "10"],
                   ["Amoxicillin + Clavulanate", "625 mg", "Tablet", "1-0-1", "5 days", "10"],
                   ["Cetirizine Hydrochloride", "10 mg", "Tablet", "0-0-1 (Night)", "5 days", "5"],
                   ["Pantoprazole Gastro-Resistant", "40 mg", "Capsule", "1-0-0 (Before breakfast)", "14 days", "14"],
                   ["Metformin Sustained Release", "500 mg", "Tablet", "1-0-1 (Chronic/Lifetime)", "30 days", "60"],
                   ["Azithromycin", "500 mg", "Tablet", "0-1-0", "3 days", "3"],
               ],
               [14, 320, 450, 590, 835, 995])
    img.save(os.path.join(ASSETS_DIR, "ui_06_clinic_inventory.png"))

    # 7. Doctor Clinical Consultation & Addenda
    nav_doc = ["Dashboard", "My Appointments", "Prescriptions"]
    img, d = draw_ims_shell("Riverside Multispeciality Clinic", "Doctor Workspace", "Dr. R. Karthikeyan", nav_doc, 1,
                            ["Home", "My Appointments", "APT-2026-00108"], "Clinical Consultation Record",
                            ("FINALISED · IMMUTABLE", "#E3F4EA", "#1B7F4B"),
                            "Patient: Vignesh M (PAT-2026-0014) · Male, 22 yrs · Blood Group O+ · Slot: 30 Oct 2026, 10:00 IST")
    btn(d, 1170, 118, "Issue Digital Prescription ->")
    card(d, 276, 188, 720, 670, "Clinical Diagnosis & Examination Notes (dbo.Consultations)")
    d.text((298, 240), "PRIMARY DIAGNOSIS", font=F_BOLD(11), fill="#5B6573")
    d.rounded_rectangle([298, 260, 972, 315], radius=6, fill="#F8FAFC", outline="#D9DEE5", width=1)
    d.text((312, 278), "Acute Viral Upper Respiratory Tract Infection (J06.9) with mild pharyngitis", font=F_BOLD(13), fill="#1B2430")
    d.text((298, 336), "CLINICAL EXAMINATION & OBSERVATIONS", font=F_BOLD(11), fill="#5B6573")
    d.rounded_rectangle([298, 356, 972, 510], radius=6, fill="#F8FAFC", outline="#D9DEE5", width=1)
    d.text((312, 372), "Temp: 100.4 F | BP: 118/76 mmHg | SpO2: 99% RA | Pulse: 84 bpm.\nThroat mildly congested; bilateral chest clear, no wheeze/crepitations.\nAdvised warm saline gargles, adequate hydration and rest for 3 days.", font=F_REG(13), fill="#1B2430")
    d.text((298, 535), "APPENDED CLINICAL ADDENDA (dbo.ConsultationAddenda — Append-Only)", font=F_BOLD(11), fill="#5B6573")
    d.rounded_rectangle([298, 558, 972, 650], radius=6, fill="#E4EEFB", outline="#93C5FD", width=1)
    d.text((312, 572), "Addendum #1 · 30 Oct 2026, 10:18 UTC · Reason: Lab correlation", font=F_BOLD(12), fill="#1A5FB4")
    d.text((312, 598), "Rapid antigen test negative; no antibiotic indicated unless fever persists >72h.", font=F_REG(12), fill="#1B2430")
    input_field(d, 298, 675, 674, "Add Signed Addendum (Finalised clinical notes can never be overwritten)", "Enter reason & clinical addendum...")
    btn(d, 298, 750, "Append Signed Addendum")

    card(d, 1014, 188, 394, 670, "Patient Clinical Summary")
    p_summary = [
        ("Patient ID", "PAT-2026-0014"),
        ("Blood Group", "O Positive"),
        ("Known Allergies", "None reported"),
        ("Chronic Conditions", "None"),
        ("Follow-up Date", "04 Nov 2026"),
        ("Finalised Timestamp", "30 Oct 2026, 10:12 UTC"),
    ]
    sy = 245
    for k, v in p_summary:
        d.text((1034, sy), k.upper(), font=F_BOLD(11), fill="#5B6573")
        d.text((1034, sy + 18), v, font=F_BOLD(13), fill="#1B2430")
        sy += 62
    img.save(os.path.join(ASSETS_DIR, "ui_07_doctor_consultation.png"))

    # 8. Doctor 32-Character e-Prescription Issuance
    img, d = draw_ims_shell("Riverside Multispeciality Clinic", "Doctor Workspace", "Dr. R. Karthikeyan", nav_doc, 2,
                            ["Home", "Prescriptions", "RX Detail"], "Issued Digital e-Prescription",
                            ("VALID · 32-CHAR CODE", "#E3F4EA", "#1B7F4B"),
                            "Issued 29 Oct 2026 · Expires 28 Nov 2026 · Binary Case-Sensitive Collation (Latin1_General_100_BIN2)")
    card(d, 276, 188, 1132, 130, "Unique 32-Character Prescription Verification Identifier")
    d.rounded_rectangle([298, 236, 1090, 296], radius=8, fill="#0F2744")
    d.text((324, 254), "JUEB9AGPAKBTXGWNX3MQKNCUFA3DMXRP", font=F_MONO(22), fill="#FFFFFF")
    btn(d, 1115, 246, "Email Code to Patient", w=265)

    card(d, 276, 336, 1132, 524, "Prescribed Medication Lines & Real-Time Pharmacy Fulfilment")
    draw_table(d, 296, 390, 1092,
               ["Medicine & Strength", "Dosage Form", "Frequency", "Duration", "Prescribed", "Dispensed", "Mode"],
               [
                   ["Paracetamol 650 mg", "Tablet", "1-0-1 After food", "5 days", "10 Tabs", "10 / 10", "COMPLETED"],
                   ["Cetirizine 10 mg", "Tablet", "0-0-1 At bedtime", "5 days", "5 Tabs", "5 / 5", "COMPLETED"],
                   ["Vitamin C Chewable 500 mg", "Tablet", "0-1-0 Daily", "10 days", "10 Tabs", "0 / 10", "PARTIAL"],
               ],
               [14, 280, 415, 595, 720, 845, 965], pill_col=6)
    img.save(os.path.join(ASSETS_DIR, "ui_08_doctor_eprescription.png"))

    # 9. Patient Portal — Clinic Discovery & Conflict-Free Slot Booking
    nav_pat = ["Dashboard", "My Profile", "Find Nearby Clinics", "Appointments", "My Prescriptions", "My Medical History"]
    img, d = draw_ims_shell("Patient Self-Service", "Patient Portal", "Vignesh M", nav_pat, 2,
                            ["Home", "Find Nearby Clinics", "Book Appointment"], "Book Clinic Appointment Slot",
                            ("REAL-TIME AVAILABILITY", "#E3F4EA", "#1B7F4B"),
                            "Riverside Multispeciality Clinic · Adyar, Chennai · Select doctor, date and real-time 15-minute slot")
    card(d, 276, 188, 1132, 670, "Step-by-Step Atomic Appointment Booking Wizard")
    input_field(d, 304, 246, 510, "Selected Clinic", "Riverside Multispeciality Clinic (Adyar, Chennai)")
    input_field(d, 850, 246, 520, "Consulting Specialist", "Dr. R. Karthikeyan — General Medicine (Fee: Rs. 400)")
    input_field(d, 304, 336, 340, "Appointment Date", "2026-11-02 (Monday)")
    d.text((304, 420), "AVAILABLE 15-MINUTE SLOTS (Generated by SlotService · Booked slots excluded)", font=F_BOLD(12), fill="#1B2430")
    slots = [
        ("09:30", False), ("09:45", False), ("10:00", True), ("10:15", False),
        ("10:30", False), ("10:45", False), ("11:15", False), ("11:30", False)
    ]
    sx = 304
    for sl, sel in slots:
        bg = "#1E4E8C" if sel else "#FFFFFF"
        fg = "#FFFFFF" if sel else "#1B2430"
        d.rounded_rectangle([sx, 448, sx + 118, 492], radius=6, fill=bg, outline="#1E4E8C" if sel else "#B8C2CC", width=2 if sel else 1)
        d.text((sx + 34, 462), sl, font=F_BOLD(14), fill=fg)
        sx += 132
    input_field(d, 304, 522, 1066, "Reason for Visit / Chief Symptoms", "Follow-up health evaluation and blood test review")
    btn(d, 304, 616, "Confirm Appointment Slot (APT-2026-XXXXX)", w=390)
    d.text((304, 678), "Protected by Pessimistic Doctor Lock (findForUpdate) + SQL Filtered Unique Index UX_Appt_Slot.", font=F_REG(12), fill="#5B6573")
    img.save(os.path.join(ASSETS_DIR, "ui_09_patient_booking.png"))

    # 10. Pharmacy Finance Ledger & Receipt Sync
    nav_pharm = ["Dashboard", "POS", "Price List", "Finance", "Users", "Reports"]
    img, d = draw_ims_shell("Vignesh Pharmacy", "Pharmacy Workspace", "Vignesh Pharmacy", nav_pharm, 3,
                            ["Home", "Finance"], "Pharmacy Finance & Automated POS Ledger",
                            ("AUTO-SYNCED WITH POS", "#E3F4EA", "#1B7F4B"),
                            "POS dispense totals are posted automatically as INCOME linked to receipt IDs; expenses are recorded manually")
    kpi_row(d, [
        ("Today's POS Income", "Rs. 75.00", "2 POS sales auto-posted", "#1B7F4B"),
        ("30-Day Gross Income", "Rs. 48,620.00", "146 verified Rx dispenses", "#1E4E8C"),
        ("30-Day Expenses", "Rs. 14,250.00", "Wholesale stock & utilities", "#B3261E"),
        ("30-Day Net Cashflow", "+Rs. 34,370.00", "70.7% net operating margin", "#0E8A8A"),
    ])
    card(d, 276, 308, 1132, 556, "General Cashbook & POS Receipt-Linked Ledger (dbo.PharmacyFinanceEntries)")
    draw_table(d, 296, 360, 1092,
               ["Entry Date", "Description", "Linked POS Receipt", "Amount", "Recorded By", "Type"],
               [
                   ["29 Oct 2026", "POS Sale · Rx JUEB9AGP...MXRP (Cash)", "SALE-000006 ->", "Rs. 12.50", "Auto (POS Engine)", "INCOME"],
                   ["29 Oct 2026", "POS Sale · Rx JUEB9AGP...MXRP (UPI)", "SALE-000005 ->", "Rs. 62.50", "Auto (POS Engine)", "INCOME"],
                   ["28 Oct 2026", "Wholesale Distributor Cold-Chain Invoice #884", "Manual Entry", "Rs. 6,400.00", "Vignesh Pharmacy", "PENDING"],
                   ["28 Oct 2026", "POS Sale · Rx 9KLM4PQR...7WXZ (Card)", "SALE-000004 ->", "Rs. 480.00", "Auto (POS Engine)", "INCOME"],
                   ["27 Oct 2026", "POS Sale · Rx 4HGT8VBN...2QWE (UPI)", "SALE-000003 ->", "Rs. 315.00", "Auto (POS Engine)", "INCOME"],
               ],
               [14, 155, 550, 745, 875, 1005], pill_col=5)
    img.save(os.path.join(ASSETS_DIR, "ui_11_pharmacy_finance.png"))


if __name__ == '__main__':
    gen_system_architecture()
    gen_security_pipeline()
    gen_dfd_level0()
    gen_dfd_level1()
    gen_dfd_level2_pos()
    gen_use_case()
    gen_class_diagram()
    gen_sequence_diagram()
    gen_activity_diagram()
    gen_state_machine()
    gen_er_diagram()
    print("Generated all 11 architectural, DFD, UML, and ER diagrams successfully!")
