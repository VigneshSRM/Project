"""
Script to generate all 7 architectural and UML diagrams for the IMS (Infeara Medical System) Project Report.
Uses matplotlib to produce clean, high-resolution (300 DPI) publication-grade diagrams.
"""
import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches

os.makedirs('final-docs/assets', exist_ok=True)

# Set global styles
plt.rcParams['font.family'] = 'sans-serif'
plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'Helvetica']
plt.rcParams['axes.edgecolor'] = '#cccccc'
plt.rcParams['axes.linewidth'] = 0.8

# ==============================================================================
# 1. ARCHITECTURE DIAGRAM
# ==============================================================================
def generate_architecture_diagram():
    fig, ax = plt.subplots(figsize=(12, 9), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    # Title
    ax.text(50, 96, "Infeara Medical System (IMS) — Modular Monolith Architecture",
            ha='center', va='center', fontsize=15, fontweight='bold', color='#1a365d')

    # Colors
    c_client = '#e0f2fe'  # Light Blue
    c_pres = '#dbeafe'    # Blue
    c_sec = '#fef3c7'     # Amber
    c_biz = '#dcfce7'     # Green
    c_data = '#f3e8ff'    # Purple
    c_db = '#ffedd5'      # Orange
    c_async = '#fee2e2'   # Red/Rose

    # 1. Client Layer
    ax.add_patch(patches.FancyBboxPatch((5, 83), 90, 8, boxstyle="round,pad=1", fc=c_client, ec='#0284c7', lw=1.5))
    ax.text(50, 87, "CLIENT TIER: Web Browser / Modern Responsive UI (Desktop & Tablet)",
            ha='center', va='center', fontsize=11, fontweight='bold', color='#0369a1')
    ax.text(50, 84, "HTML5, CSS3 ERP Theme, Responsive Dashboard, Semantic Layouts, Async XHR / Fetch",
            ha='center', va='center', fontsize=9, color='#0f172a')

    # Arrow 1
    ax.annotate('', xy=(50, 78), xytext=(50, 82),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))

    # 2. Security & Gateway Filter Layer
    ax.add_patch(patches.FancyBboxPatch((5, 70), 90, 7.5, boxstyle="round,pad=1", fc=c_sec, ec='#d97706', lw=1.5))
    ax.text(50, 74.5, "SECURITY & GATEWAY: Spring Security 6.x (DelegatingPasswordEncoder / BCrypt)",
            ha='center', va='center', fontsize=11, fontweight='bold', color='#b45309')
    ax.text(50, 71.5, "Session Management | RBAC (5 Roles) | CSRF Protection | Rate Limiter | Brute-Force Throttling",
            ha='center', va='center', fontsize=9, color='#0f172a')

    # Arrow 2
    ax.annotate('', xy=(50, 65), xytext=(50, 69.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))

    # 3. Presentation Layer (Controllers + Thymeleaf)
    ax.add_patch(patches.FancyBboxPatch((5, 54), 90, 10.5, boxstyle="round,pad=1", fc=c_pres, ec='#2563eb', lw=1.5))
    ax.text(50, 61.5, "PRESENTATION TIER: Spring WebMVC + Thymeleaf Layout Dialect",
            ha='center', va='center', fontsize=11, fontweight='bold', color='#1d4ed8')
    sub_pres = [
        ("Auth & Setup\n(/setup, /login)", 14),
        ("Super Admin\n(/admin/**)", 32),
        ("Clinic Portal\n(/clinic/**)", 50),
        ("Doctor Portal\n(/doctor/**)", 68),
        ("Patient Portal\n(/patient/**)", 86)
    ]
    for name, x in sub_pres:
        ax.add_patch(patches.FancyBboxPatch((x-8, 55), 16, 4.5, boxstyle="round,pad=0.3", fc='#ffffff', ec='#93c5fd', lw=1))
        ax.text(x, 57.2, name, ha='center', va='center', fontsize=7.5, color='#1e293b')

    # Arrow 3
    ax.annotate('', xy=(50, 48.5), xytext=(50, 53.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))

    # 4. Service / Business Logic Layer
    ax.add_patch(patches.FancyBboxPatch((5, 31), 90, 17, boxstyle="round,pad=1", fc=c_biz, ec='#16a34a', lw=1.5))
    ax.text(50, 45, "BUSINESS LOGIC TIER: Modular Service Layer (@Service, @Transactional)",
            ha='center', va='center', fontsize=11, fontweight='bold', color='#15803d')
    
    modules = [
        ("Identity & Token Service\n• SHA-256 Auth Tokens\n• Password Policies\n• Audit Event Logging", 14, 38),
        ("Patient Linking Service\n• Multi-Clinic Linking\n• ID Verification\n• Record Merging", 38, 38),
        ("Clinical Consult & e-Rx\n• Slot Management\n• Consult Addenda\n• 32-char Rx Codes", 62, 38),
        ("Pharmacy POS & Finance\n• Itemized Dispensing\n• Price Catalog\n• Ledger Automation", 86, 38)
    ]
    for title, x, y in modules:
        ax.add_patch(patches.FancyBboxPatch((x-11, y-5), 22, 10, boxstyle="round,pad=0.5", fc='#ffffff', ec='#86efac', lw=1))
        ax.text(x, y, title, ha='center', va='center', fontsize=7.5, color='#0f172a', linespacing=1.3)

    # Arrow 4
    ax.annotate('', xy=(50, 25.5), xytext=(50, 30.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))

    # 5. Data Access Layer
    ax.add_patch(patches.FancyBboxPatch((5, 16), 90, 9, boxstyle="round,pad=1", fc=c_data, ec='#9333ea', lw=1.5))
    ax.text(50, 22, "PERSISTENCE TIER: Spring Data JPA 3.x + Hibernate 6.x",
            ha='center', va='center', fontsize=11, fontweight='bold', color='#7e22ce')
    ax.text(50, 18.5, "27 Spring Data Repositories | Declarative Query Methods | Optimistic Locking (@Version) | Flyway Migrations",
            ha='center', va='center', fontsize=8.5, color='#0f172a')

    # Arrow 5
    ax.annotate('', xy=(35, 10.5), xytext=(35, 15.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))
    ax.annotate('', xy=(75, 10.5), xytext=(75, 15.5),
                arrowprops=dict(facecolor='#475569', edgecolor='#475569', arrowstyle='->', lw=2))

    # 6. Database and Async Worker
    ax.add_patch(patches.FancyBboxPatch((5, 2), 58, 8.5, boxstyle="round,pad=1", fc=c_db, ec='#ea580c', lw=1.5))
    ax.text(34, 7, "DATABASE ENGINE: SQL Server 2022 / H2 File-Persistent",
            ha='center', va='center', fontsize=10, fontweight='bold', color='#c2410c')
    ax.text(34, 4, "24 Relational Tables | Case-Sensitive Rx Collation | Filtered Indexes | V001-V009 Flyway Scripts",
            ha='center', va='center', fontsize=8, color='#0f172a')

    ax.add_patch(patches.FancyBboxPatch((66, 2), 29, 8.5, boxstyle="round,pad=1", fc=c_async, ec='#e11d48', lw=1.5))
    ax.text(80.5, 7, "ASYNC OUTBOX WORKER",
            ha='center', va='center', fontsize=10, fontweight='bold', color='#be123c')
    ax.text(80.5, 4, "@Scheduled Background Delivery\nRetry with Exponential Backoff",
            ha='center', va='center', fontsize=7.5, color='#0f172a', linespacing=1.2)

    plt.tight_layout()
    plt.savefig('final-docs/assets/architecture_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated architecture_diagram.png")

# ==============================================================================
# 2. USE CASE DIAGRAM
# ==============================================================================
def generate_use_case_diagram():
    fig, ax = plt.subplots(figsize=(13, 9), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "IMS (Infeara Medical System) — UML Use Case Diagram",
            ha='center', va='center', fontsize=15, fontweight='bold', color='#1e3a8a')

    # System boundary box
    sys_box = patches.FancyBboxPatch((24, 4), 52, 90, boxstyle="round,pad=1", fc='#f8fafc', ec='#334155', lw=2, linestyle='--')
    ax.add_patch(sys_box)
    ax.text(50, 91.5, "Infeara Medical System Boundary", ha='center', va='center', fontsize=11, fontweight='bold', color='#475569')

    # Draw Actor Helper
    def draw_actor(x, y, label, role_color='#1e293b'):
        # Head
        circle = patches.Circle((x, y+2.5), 1.6, fc='#f1f5f9', ec=role_color, lw=1.5)
        ax.add_patch(circle)
        # Body
        ax.plot([x, x], [y+0.9, y-2.5], color=role_color, lw=1.5)
        # Arms
        ax.plot([x-2.2, x+2.2], [y-0.2, y-0.2], color=role_color, lw=1.5)
        # Legs
        ax.plot([x, x-1.8], [y-2.5, y-5.2], color=role_color, lw=1.5)
        ax.plot([x, x+1.8], [y-2.5, y-5.2], color=role_color, lw=1.5)
        # Label
        ax.text(x, y-7.5, label, ha='center', va='center', fontsize=9, fontweight='bold', color=role_color)

    # Draw Use Case Oval Helper
    def draw_uc(x, y, text, w=19, h=5.2, fill='#e0f2fe', edge='#0284c7'):
        ellipse = patches.Ellipse((x, y), w, h, fc=fill, ec=edge, lw=1.2)
        ax.add_patch(ellipse)
        ax.text(x, y, text, ha='center', va='center', fontsize=7.5, color='#0f172a', linespacing=1.1)

    # Actors on Left
    draw_actor(10, 80, "Super Admin", '#b45309')
    draw_actor(10, 52, "Clinic Admin", '#15803d')
    draw_actor(10, 24, "Doctor", '#0369a1')

    # Actors on Right
    draw_actor(90, 68, "Patient", '#7e22ce')
    draw_actor(90, 28, "Pharmacy Admin /\nPharmacist", '#be123c')

    # Use cases inside system box
    ucs = [
        # Super Admin
        (50, 85, "Bootstrap System & Setup (/setup)", '#fef3c7', '#d97706'),
        (50, 78, "Enroll Clinics & Pharmacies", '#fef3c7', '#d97706'),
        (50, 71, "Platform KPI & SMTP Config", '#fef3c7', '#d97706'),
        
        # Clinic Admin
        (50, 63, "Manage Doctors & Work Schedules", '#dcfce7', '#16a34a'),
        (50, 56, "Verify & Merge Patient Links", '#dcfce7', '#16a34a'),
        (50, 49, "Front-Desk Appointment Booking", '#dcfce7', '#16a34a'),
        
        # Doctor
        (50, 41, "Clinical Consult & Draft/Finalise", '#dbeafe', '#2563eb'),
        (50, 34, "Generate 32-Char e-Prescriptions", '#dbeafe', '#2563eb'),
        
        # Patient
        (50, 26, "Register & Verify Email", '#f3e8ff', '#9333ea'),
        (50, 19, "Request Clinic Link & View Rx", '#f3e8ff', '#9333ea'),
        
        # Pharmacy
        (50, 12, "Validate Prescription & Auto-POS", '#ffe4e6', '#e11d48'),
        (50, 5.5, "Dispense Drugs & Sync Finance Ledger", '#ffe4e6', '#e11d48')
    ]

    for x, y, text, fill, edge in ucs:
        draw_uc(x, y, text, fill=fill, edge=edge)

    # Connection lines
    # Super Admin -> 85, 78, 71
    for uy in [85, 78, 71]:
        ax.plot([14, 40], [80, uy], color='#b45309', lw=1, linestyle='-')

    # Clinic Admin -> 63, 56, 49
    for uy in [63, 56, 49]:
        ax.plot([14, 40], [52, uy], color='#15803d', lw=1, linestyle='-')

    # Doctor -> 41, 34
    for uy in [41, 34]:
        ax.plot([14, 40], [24, uy], color='#0369a1', lw=1, linestyle='-')

    # Patient -> 26, 19, 49 (book appt)
    for uy in [49, 26, 19]:
        ax.plot([86, 60], [68, uy], color='#7e22ce', lw=1, linestyle='-')

    # Pharmacy -> 12, 5.5
    for uy in [12, 5.5]:
        ax.plot([86, 60], [28, uy], color='#be123c', lw=1, linestyle='-')

    plt.tight_layout()
    plt.savefig('final-docs/assets/use_case_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated use_case_diagram.png")

# ==============================================================================
# 3. CLASS DIAGRAM
# ==============================================================================
def generate_class_diagram():
    fig, ax = plt.subplots(figsize=(13, 9.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "IMS Core Domain Entity Class Diagram (Spring Data JPA)",
            ha='center', va='center', fontsize=15, fontweight='bold', color='#1e3a8a')

    def draw_class(x, y, w, h, name, fields, methods):
        # Outer box
        ax.add_patch(patches.FancyBboxPatch((x, y), w, h, boxstyle="square,pad=0", fc='#ffffff', ec='#1e293b', lw=1.2))
        # Header box
        ax.add_patch(patches.Rectangle((x, y+h-3.2), w, 3.2, fc='#e2e8f0', ec='#1e293b', lw=1))
        ax.text(x + w/2, y+h-1.6, f"«Entity»\n{name}", ha='center', va='center', fontsize=7.5, fontweight='bold', color='#0f172a')
        
        # Split line for fields
        f_y = y + h - 4.5
        for f in fields:
            ax.text(x + 1, f_y, f, ha='left', va='center', fontsize=6.2, color='#334155')
            f_y -= 1.6
            
        # Split line for methods
        ax.plot([x, x+w], [f_y+0.6, f_y+0.6], color='#cbd5e1', lw=0.8)
        f_y -= 0.8
        for m in methods:
            ax.text(x + 1, f_y, m, ha='left', va='center', fontsize=6.2, color='#1e40af', fontstyle='italic')
            f_y -= 1.6

    # 1. User
    draw_class(2, 68, 20, 24, "User",
               ["- userId: Long [PK]", "- email: String", "- passwordHash: String", "- role: Role", "- status: UserStatus", "- clinicId: Long [FK]"],
               ["+ verifyEmail()", "+ updatePassword()", "+ isAccountActive()"])

    # 2. Clinic
    draw_class(27, 68, 20, 24, "Clinic",
               ["- clinicId: Long [PK]", "- publicUuid: UUID", "- name: String", "- city, state: String", "- status: ClinicStatus", "- openingHours: String"],
               ["+ activate()", "+ suspend()", "+ updateDetails()"])

    # 3. Doctor
    draw_class(53, 68, 21, 24, "Doctor",
               ["- doctorId: Long [PK]", "- publicUuid: UUID", "- name: String", "- specialization: String", "- status: DoctorStatus"],
               ["+ linkToClinic(fee)", "+ setSchedule()", "+ isActive()"])

    # 4. Patient
    draw_class(78, 68, 20, 24, "Patient",
               ["- patientId: Long [PK]", "- publicPatientId: String", "- userId: Long [FK]", "- name: String", "- dob: LocalDate", "- bloodGroup: String"],
               ["+ mergeInto(targetId)", "+ updateProfile()", "+ getFullName()"])

    # 5. Appointment
    draw_class(2, 36, 21, 25, "Appointment",
               ["- appointmentId: Long [PK]", "- publicApptId: String", "- patientId: Long [FK]", "- clinicId: Long [FK]", "- doctorId: Long [FK]", "- slotStart, slotEnd: Instant", "- status: ApptStatus"],
               ["+ checkIn()", "+ complete()", "+ cancel(reason)"])

    # 6. Consultation
    draw_class(28, 36, 20, 25, "Consultation",
               ["- consultationId: Long [PK]", "- appointmentId: Long [FK]", "- diagnosis: String", "- notes: String", "- status: ConsultStatus", "- finalisedAt: Instant"],
               ["+ addAddendum(text)", "+ finalise()", "+ isFinal()"])

    # 7. Prescription
    draw_class(53, 36, 21, 25, "Prescription",
               ["- prescriptionId: Long [PK]", "- prescriptionCode: String(32)", "- consultationId: Long [FK]", "- patientId: Long [FK]", "- expiryDate: Instant", "- status: RxStatus"],
               ["+ isExpired()", "+ cancel(reason)", "+ canDispense()"])

    # 8. PrescriptionItem
    draw_class(78, 36, 20, 25, "PrescriptionItem",
               ["- itemId: Long [PK]", "- prescriptionId: Long [FK]", "- medicine: String", "- dosage, freq: String", "- quantity: int", "- quantityDispensed: int"],
               ["+ getRemainingQty()", "+ recordDispense(qty)", "+ isFullyDispensed()"])

    # 9. Pharmacy
    draw_class(10, 4, 22, 25, "Pharmacy",
               ["- pharmacyId: Long [PK]", "- publicUuid: UUID", "- name: String", "- licenceNumber: String", "- status: PharmacyStatus"],
               ["+ verifyPrescription(code)", "+ dispense()", "+ enroll()"])

    # 10. PrescriptionDispense
    draw_class(40, 4, 22, 25, "PrescriptionDispense",
               ["- dispenseId: Long [PK]", "- prescriptionId: Long [FK]", "- pharmacyId: Long [FK]", "- subtotal, total: BigDecimal", "- paymentMethod: String", "- status: DispenseStatus"],
               ["+ calculateTotal()", "+ voidDispense()", "+ generateReceipt()"])

    # 11. PharmacyFinanceEntry
    draw_class(70, 4, 22, 25, "PharmacyFinanceEntry",
               ["- entryId: Long [PK]", "- pharmacyId: Long [FK]", "- entryType: EntryType", "- amount: BigDecimal", "- description: String", "- entryDate: LocalDate"],
               ["+ postIncome(dispenseId)", "+ postExpense(amount)", "+ getSummary()"])

    # Connectors
    # User -> Clinic
    ax.annotate('1..*', xy=(22, 80), xytext=(27, 80), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    # Clinic -> Doctor
    ax.annotate('1..*', xy=(47, 80), xytext=(53, 80), arrowprops=dict(arrowstyle='<->', lw=1.2, color='#475569'))
    # Appointment -> Patient / Doctor
    ax.annotate('', xy=(12, 61), xytext=(12, 68), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    ax.annotate('1..*', xy=(23, 48), xytext=(28, 48), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    # Consultation -> Prescription
    ax.annotate('1..1', xy=(48, 48), xytext=(53, 48), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    # Prescription -> PrescriptionItem
    ax.annotate('1..*', xy=(74, 48), xytext=(78, 48), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    # Pharmacy -> PrescriptionDispense -> Finance
    ax.annotate('', xy=(32, 16), xytext=(40, 16), arrowprops=dict(arrowstyle='<-', lw=1.2, color='#475569'))
    ax.annotate('', xy=(62, 16), xytext=(70, 16), arrowprops=dict(arrowstyle='->', lw=1.2, color='#475569'))

    plt.tight_layout()
    plt.savefig('final-docs/assets/class_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated class_diagram.png")

# ==============================================================================
# 4. SEQUENCE DIAGRAM
# ==============================================================================
def generate_sequence_diagram():
    fig, ax = plt.subplots(figsize=(13, 9), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "IMS Sequence Flow: Patient Consult, e-Rx Issuance, POS Dispense & Finance Sync",
            ha='center', va='center', fontsize=13, fontweight='bold', color='#1e3a8a')

    lifelines = [
        ("Patient", 10, '#f3e8ff', '#9333ea'),
        ("Clinic / Doctor", 30, '#dbeafe', '#2563eb'),
        ("IMS Core (Spring Boot)", 52, '#dcfce7', '#16a34a'),
        ("SQL Server DB", 74, '#ffedd5', '#ea580c'),
        ("Pharmacy POS", 92, '#ffe4e6', '#e11d48')
    ]

    for name, x, fc, ec in lifelines:
        ax.add_patch(patches.FancyBboxPatch((x-8, 90), 16, 4.5, boxstyle="round,pad=0.2", fc=fc, ec=ec, lw=1.2))
        ax.text(x, 92.2, name, ha='center', va='center', fontsize=8, fontweight='bold', color='#0f172a')
        ax.plot([x, x], [4, 90], color='#94a3b8', linestyle='--', lw=1)

    steps = [
        # y, sender, receiver, label, is_return
        (85, 10, 52, "1. Book Appointment / Check-in", False),
        (80, 52, 74, "2. INSERT Appointment (Status: CHECKED_IN)", False),
        (75, 30, 52, "3. Start Consultation & Record Diagnosis", False),
        (70, 52, 74, "4. Save Consultation (DRAFT -> FINAL)", False),
        (65, 30, 52, "5. Prescribe Drugs & Click 'Finalise Rx'", False),
        (60, 52, 74, "6. Generate 32-Char Rx Code & Hash Token", False),
        (55, 52, 10, "7. Send Digital Prescription (SMS/Email)", True),
        (48, 10, 92, "8. Present 32-Char Code at Pharmacy", False),
        (42, 92, 52, "9. Scan Code: GET /pharmacy/pos/lookup", False),
        (36, 52, 74, "10. Query Rx Items, Verify Expiry & Qty Dispensed", False),
        (30, 52, 92, "11. Auto-fill POS Cart with Catalog Prices", True),
        (24, 92, 52, "12. Submit Payment & Complete Dispense", False),
        (18, 52, 74, "13. UPDATE RxItems, INSERT Dispense & Receipt", False),
        (12, 52, 74, "14. Auto-INSERT Income into PharmacyFinanceEntries", False),
        (6, 52, 92, "15. Print Dispense Receipt & Sync Cashflow KPI", True)
    ]

    for y, x1, x2, label, is_ret in steps:
        ls = '--' if is_ret else '-'
        col = '#0284c7' if not is_ret else '#059669'
        ax.annotate('', xy=(x2, y), xytext=(x1, y),
                    arrowprops=dict(facecolor=col, edgecolor=col, arrowstyle='->', lw=1.3, linestyle=ls))
        mid = (x1 + x2) / 2
        ax.text(mid, y + 1.2, label, ha='center', va='bottom', fontsize=7.2, fontweight='bold', color='#1e293b')

    plt.tight_layout()
    plt.savefig('final-docs/assets/sequence_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated sequence_diagram.png")

# ==============================================================================
# 5. ACTIVITY DIAGRAM
# ==============================================================================
def generate_activity_diagram():
    fig, ax = plt.subplots(figsize=(11, 10), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97.5, "IMS End-to-End Clinical & Dispensing Activity Workflow",
            ha='center', va='center', fontsize=14, fontweight='bold', color='#1e3a8a')

    # Initial node
    ax.add_patch(patches.Circle((50, 93), 1.8, fc='#0f172a', ec='#0f172a'))
    ax.annotate('', xy=(50, 87), xytext=(50, 91.2), arrowprops=dict(facecolor='#0f172a', arrowstyle='->', lw=1.5))

    def draw_act(x, y, w, h, text, fc='#e0f2fe', ec='#0284c7'):
        ax.add_patch(patches.FancyBboxPatch((x-w/2, y-h/2), w, h, boxstyle="round,pad=0.3", fc=fc, ec=ec, lw=1.2))
        ax.text(x, y, text, ha='center', va='center', fontsize=8, color='#0f172a', linespacing=1.2)

    def draw_dec(x, y, text):
        # Diamond
        dia = patches.Polygon([[x, y+2.5], [x+6, y], [x, y-2.5], [x-6, y]], fc='#fef3c7', ec='#d97706', lw=1.2)
        ax.add_patch(dia)
        ax.text(x, y, text, ha='center', va='center', fontsize=6.8, fontweight='bold', color='#92400e')

    # 1. Patient Registration / Link Request
    draw_act(50, 84, 46, 5, "Patient Registers & Requests Clinic Link (My Profile)")
    ax.annotate('', xy=(50, 78.5), xytext=(50, 81.5), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 2. Clinic Approval & Identity Check
    draw_dec(50, 75, "Identity\nVerified?")
    # Branch No
    ax.plot([44, 18, 18], [75, 75, 68], color='#e11d48', lw=1.2)
    ax.annotate('', xy=(18, 64), xytext=(18, 68), arrowprops=dict(facecolor='#e11d48', arrowstyle='->', lw=1.2))
    ax.text(30, 76, "No", fontsize=7.5, color='#e11d48', fontweight='bold')
    draw_act(18, 61, 24, 4.5, "Reject Link Request\n(Log Audit Entry)", '#ffe4e6', '#e11d48')

    # Branch Yes
    ax.plot([56, 75, 75], [75, 75, 68], color='#16a34a', lw=1.2)
    ax.annotate('', xy=(75, 64), xytext=(75, 68), arrowprops=dict(facecolor='#16a34a', arrowstyle='->', lw=1.2))
    ax.text(64, 76, "Yes", fontsize=7.5, color='#16a34a', fontweight='bold')
    draw_act(75, 61, 32, 4.5, "Approve Link or Merge into\nExisting Clinic Record", '#dcfce7', '#16a34a')

    # Rejoin at Appointment Booking
    ax.plot([75, 75, 50], [58.5, 54, 54], color='#475569', lw=1.2)
    ax.annotate('', xy=(50, 50.5), xytext=(50, 54), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 3. Appointment Desk
    draw_act(50, 48, 48, 4.5, "Book Appointment with Scheduled Doctor Slot")
    ax.annotate('', xy=(50, 43), xytext=(50, 45.7), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 4. Clinical Consult
    draw_act(50, 40, 52, 4.8, "Doctor Consult: Exam, Diagnosis, Addenda & e-Prescription")
    ax.annotate('', xy=(50, 35), xytext=(50, 37.6), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 5. Rx Generation
    draw_act(50, 32, 54, 4.5, "Issue 32-Character Unique Cryptographic Prescription Code", '#ede9fe', '#7c3aed')
    ax.annotate('', xy=(50, 27), xytext=(50, 29.7), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 6. Pharmacy POS
    draw_act(50, 24, 56, 4.5, "Pharmacy POS: Barcode Scan, Auto-Pricing & Quantity Dispense", '#ffe4e6', '#e11d48')
    ax.annotate('', xy=(50, 19), xytext=(50, 21.7), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # 7. Auto Finance Sync
    draw_act(50, 16, 52, 4.5, "Auto-Post Income to Ledger & Print Detailed Invoice Receipt", '#dcfce7', '#16a34a')
    ax.annotate('', xy=(50, 11), xytext=(50, 13.7), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.5))

    # Final Node (Bullseye)
    ax.add_patch(patches.Circle((50, 8.5), 2.2, fc='#ffffff', ec='#0f172a', lw=1.5))
    ax.add_patch(patches.Circle((50, 8.5), 1.3, fc='#0f172a', ec='#0f172a'))

    plt.tight_layout()
    plt.savefig('final-docs/assets/activity_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated activity_diagram.png")

# ==============================================================================
# 6. DFD LEVEL 0 & LEVEL 1
# ==============================================================================
def generate_dfd_diagram():
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 11), dpi=300)
    
    # ---------------- DFD Level 0 (Context) ----------------
    ax1.set_xlim(0, 100)
    ax1.set_ylim(0, 100)
    ax1.axis('off')
    ax1.text(50, 94, "Data Flow Diagram — Level 0 (Context Diagram)",
             ha='center', va='center', fontsize=12, fontweight='bold', color='#1e3a8a')

    # Center Process
    ax1.add_patch(patches.Circle((50, 50), 16, fc='#dbeafe', ec='#1d4ed8', lw=2))
    ax1.text(50, 52, "0.0\nInfeara Medical\nSystem (IMS)", ha='center', va='center', fontsize=10, fontweight='bold', color='#1e3a8a')

    # External Entities
    def draw_entity(ax, x, y, label):
        ax.add_patch(patches.Rectangle((x-10, y-6), 20, 12, fc='#f1f5f9', ec='#334155', lw=1.5))
        ax.text(x, y, label, ha='center', va='center', fontsize=8.5, fontweight='bold', color='#0f172a')

    draw_entity(ax1, 14, 80, "Super Admin")
    draw_entity(ax1, 14, 20, "Clinic Admin")
    draw_entity(ax1, 86, 80, "Doctor")
    draw_entity(ax1, 86, 20, "Pharmacy")
    draw_entity(ax1, 50, 8, "Patient")

    # Arrows for DFD Level 0
    ax1.annotate('Enrollment & Config', xy=(37, 58), xytext=(24, 76),
                 arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2), fontsize=7)
    ax1.annotate('Link Approvals & Slots', xy=(36, 42), xytext=(24, 24),
                 arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2), fontsize=7)
    ax1.annotate('Diagnosis & e-Rx', xy=(63, 58), xytext=(76, 76),
                 arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2), fontsize=7)
    ax1.annotate('Dispense & POS Bills', xy=(63, 42), xytext=(76, 24),
                 arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2), fontsize=7)
    ax1.annotate('Registration & Booking', xy=(50, 34), xytext=(50, 14),
                 arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2), fontsize=7, ha='center')

    # ---------------- DFD Level 1 (Decomposition) ----------------
    ax2.set_xlim(0, 100)
    ax2.set_ylim(0, 100)
    ax2.axis('off')
    ax2.text(50, 95, "Data Flow Diagram — Level 1 (Subsystem Decomposition)",
             ha='center', va='center', fontsize=12, fontweight='bold', color='#1e3a8a')

    procs = [
        ("1.0 Auth & Identity\nFederation", 14, 70),
        ("2.0 Appointment &\nSlot Management", 38, 70),
        ("3.0 Clinical Consult\n& Diagnosis", 62, 70),
        ("4.0 Electronic\nPrescriptions", 86, 70),
        ("5.0 Pharmacy POS\n& Dispense", 38, 25),
        ("6.0 Financial\nLedger Accounting", 74, 25)
    ]
    for text, x, y in procs:
        ax2.add_patch(patches.Circle((x, y), 10, fc='#e0f2fe', ec='#0284c7', lw=1.5))
        ax2.text(x, y, text, ha='center', va='center', fontsize=7.2, fontweight='bold', color='#0f172a')

    # Data Stores
    def draw_store(ax, x, y, name):
        ax.plot([x-10, x+10], [y+3, y+3], color='#334155', lw=1.5)
        ax.plot([x-10, x+10], [y-3, y-3], color='#334155', lw=1.5)
        ax.fill_between([x-10, x+10], [y-3, y-3], [y+3, y+3], color='#fef3c7')
        ax.text(x, y, name, ha='center', va='center', fontsize=7.5, fontweight='bold', color='#b45309')

    draw_store(ax2, 14, 45, "D1: Users & Profiles")
    draw_store(ax2, 50, 45, "D2: Appointments & Slots")
    draw_store(ax2, 86, 45, "D3: Consultations & e-Rx")
    draw_store(ax2, 56, 5, "D4: Pharmacy Ledger & POS")

    # Connectors
    ax2.annotate('', xy=(14, 50), xytext=(14, 60), arrowprops=dict(facecolor='#475569', arrowstyle='<->', lw=1.2))
    ax2.annotate('', xy=(24, 70), xytext=(28, 70), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2))
    ax2.annotate('', xy=(48, 70), xytext=(52, 70), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2))
    ax2.annotate('', xy=(72, 70), xytext=(76, 70), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2))
    ax2.annotate('', xy=(86, 60), xytext=(86, 50), arrowprops=dict(facecolor='#475569', arrowstyle='<->', lw=1.2))
    ax2.annotate('', xy=(38, 35), xytext=(80, 43), arrowprops=dict(facecolor='#475569', arrowstyle='<-', lw=1.2))
    ax2.annotate('', xy=(48, 25), xytext=(64, 25), arrowprops=dict(facecolor='#475569', arrowstyle='->', lw=1.2))
    ax2.annotate('', xy=(66, 10), xytext=(74, 18), arrowprops=dict(facecolor='#475569', arrowstyle='<->', lw=1.2))

    plt.tight_layout()
    plt.savefig('final-docs/assets/dfd_level0_level1.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated dfd_level0_level1.png")

# ==============================================================================
# 7. ER DIAGRAM
# ==============================================================================
def generate_er_diagram():
    fig, ax = plt.subplots(figsize=(13, 9.5), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis('off')

    ax.text(50, 97, "IMS Database Entity-Relationship (ER) Architecture (24 Tables)",
            ha='center', va='center', fontsize=14, fontweight='bold', color='#1e3a8a')

    def draw_entity_table(x, y, w, h, name, pks, fks, attrs):
        ax.add_patch(patches.FancyBboxPatch((x, y), w, h, boxstyle="square,pad=0", fc='#ffffff', ec='#1e293b', lw=1.2))
        ax.add_patch(patches.Rectangle((x, y+h-3), w, 3, fc='#e0f2fe', ec='#1e293b', lw=1))
        ax.text(x+w/2, y+h-1.5, name, ha='center', va='center', fontsize=7.5, fontweight='bold', color='#0369a1')
        
        curr_y = y + h - 4.5
        for pk in pks:
            ax.text(x+1, curr_y, f"PK {pk}", ha='left', va='center', fontsize=6.2, fontweight='bold', color='#b45309')
            curr_y -= 1.6
        for fk in fks:
            ax.text(x+1, curr_y, f"FK {fk}", ha='left', va='center', fontsize=6.2, color='#15803d')
            curr_y -= 1.6
        for att in attrs:
            ax.text(x+1, curr_y, att, ha='left', va='center', fontsize=6.0, color='#475569')
            curr_y -= 1.5

    # Entities
    draw_entity_table(2, 68, 18, 24, "Users", ["user_id"], ["clinic_id"], ["email", "password_hash", "role", "status", "email_verified_at"])
    draw_entity_table(24, 68, 18, 24, "Clinics", ["clinic_id"], [], ["name", "address_line1", "city, state", "postal_code", "status"])
    draw_entity_table(46, 68, 18, 24, "Doctors", ["doctor_id"], [], ["name", "specialization", "status", "public_uuid"])
    draw_entity_table(68, 68, 18, 24, "Patients", ["patient_id"], ["user_id", "merged_into_id"], ["public_patient_id", "name", "dob", "gender", "blood_group"])
    draw_entity_table(88, 68, 10.5, 24, "AuditLogs", ["audit_id"], ["actor_id", "patient_id"], ["action", "res_type", "outcome", "ip_addr"])

    draw_entity_table(2, 36, 18, 25, "Appointments", ["appointment_id"], ["patient_id", "clinic_id", "doctor_id"], ["slot_start", "slot_end", "reason", "status (UQ_Slot)"])
    draw_entity_table(24, 36, 18, 25, "Consultations", ["consultation_id"], ["appointment_id"], ["diagnosis", "notes", "status (DRAFT/FINAL)", "finalised_at"])
    draw_entity_table(46, 36, 18, 25, "Prescriptions", ["prescription_id"], ["consultation_id", "patient_id", "clinic_id"], ["rx_code (32 CHAR)", "issue_date", "expiry_date", "status"])
    draw_entity_table(68, 36, 18, 25, "PrescriptionItems", ["item_id"], ["prescription_id"], ["medicine", "dosage, freq", "duration, qty", "qty_dispensed", "lifetime (BIT)"])

    draw_entity_table(14, 4, 18, 24, "Pharmacies", ["pharmacy_id"], [], ["name", "licence_number", "address, city", "status"])
    draw_entity_table(38, 4, 22, 24, "PrescriptionDispenses", ["dispense_id"], ["prescription_id", "pharmacy_id", "verification_id"], ["subtotal, discount", "tax_rate, total", "payment_method", "dispensed_at"])
    draw_entity_table(66, 4, 20, 24, "PharmacyFinanceEntries", ["entry_id"], ["pharmacy_id"], ["entry_type", "amount", "description", "entry_date"])

    # Relationships
    ax.plot([11, 11, 24], [68, 55, 55], color='#64748b', lw=1)
    ax.annotate('', xy=(24, 55), xytext=(11, 55), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    ax.plot([20, 24], [48, 48], color='#64748b', lw=1)
    ax.annotate('', xy=(24, 48), xytext=(20, 48), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    ax.plot([42, 46], [48, 48], color='#64748b', lw=1)
    ax.annotate('', xy=(46, 48), xytext=(42, 48), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    ax.plot([64, 68], [48, 48], color='#64748b', lw=1)
    ax.annotate('', xy=(68, 48), xytext=(64, 48), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    ax.plot([55, 55, 60], [36, 30, 28], color='#64748b', lw=1)
    ax.annotate('', xy=(60, 24), xytext=(55, 30), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    ax.plot([60, 66], [16, 16], color='#64748b', lw=1)
    ax.annotate('', xy=(66, 16), xytext=(60, 16), arrowprops=dict(facecolor='#64748b', arrowstyle='->', lw=1))

    plt.tight_layout()
    plt.savefig('final-docs/assets/er_diagram.png', dpi=300, bbox_inches='tight')
    plt.close()
    print("Generated er_diagram.png")

if __name__ == '__main__':
    generate_architecture_diagram()
    generate_use_case_diagram()
    generate_class_diagram()
    generate_sequence_diagram()
    generate_activity_diagram()
    generate_dfd_diagram()
    generate_er_diagram()
    print("All diagrams generated successfully!")
