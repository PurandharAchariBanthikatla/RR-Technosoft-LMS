#!/usr/bin/env python3
"""
seed_data.py — RR Technosoft LMS demo-data seeder.

Connects directly to the EXISTING PostgreSQL database used by the
Java / Spring Boot backend and INSERTs realistic Indian demo data
(students, faculty, courses, modules/lessons, videos, enrollments,
attendance, finance records, certificates, notifications) that matches
the schema created by the backend's own Flyway migrations
(backend/src/main/resources/db/migration/V1..V20).

This script does NOT:
  - create or alter any table / column / enum type
  - touch Java code, Spring Boot, or the frontend
  - delete, truncate, or drop anything
  - create any other file

It IS safe to run repeatedly: every insert is guarded by the same
unique constraints / indexes the schema already enforces (or, where no
such constraint exists, by an explicit existence check), so a second
run only reports "skipped" rather than creating duplicates.

Usage:
    pip install psycopg[binary] Faker python-dotenv bcrypt
    export DATABASE_URL=postgresql://lms_user:secret@localhost:5432/rr_lms
    python seed_data.py

Connection env vars (either form works):
    DATABASE_URL=postgresql://user:pass@host:port/dbname
  or
    DB_HOST (default localhost)
    DB_PORT (default 5432)
    DB_NAME  / DB_USERNAME / DB_PASSWORD   (names used by the app's own .env)
"""

from __future__ import annotations

import os
import random
import sys
from dataclasses import dataclass, field
from datetime import date, timedelta, datetime, timezone
from decimal import Decimal
from typing import Optional

try:
    import psycopg
    from psycopg.rows import dict_row
except ImportError:
    sys.exit("Missing dependency 'psycopg'. Install with: pip install psycopg[binary]")

try:
    import bcrypt
except ImportError:
    sys.exit("Missing dependency 'bcrypt'. Install with: pip install bcrypt")

try:
    from faker import Faker
except ImportError:
    sys.exit("Missing dependency 'Faker'. Install with: pip install Faker")

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # python-dotenv is a convenience only; env vars still work without it


# ============================================================================
# CONFIG
# ============================================================================

# Same demo password for every seeded account (students + faculty), mirroring
# the convention already used by the backend's own DemoDataSeeder.java.
DEMO_PASSWORD = "Demo@12345"
BCRYPT_ROUNDS = 12  # matches new BCryptPasswordEncoder(12) in SecurityConfig.java

TOTAL_STUDENTS = 40  # >= 30 as required; a few extra so every course gets enrollments

# Deterministic randomness: reruns compute the same assignments, so the
# existence checks below always resolve to "already exists" rather than
# silently drifting into new records on every run.
random.seed(20260101)
fake = Faker("en_IN")
Faker.seed(20260101)


def env(*names: str, default: Optional[str] = None) -> Optional[str]:
    for n in names:
        v = os.environ.get(n)
        if v:
            return v
    return default


def get_dsn() -> str:
    """Build a psycopg connection string from whatever env vars are present."""
    database_url = env("DATABASE_URL")
    if database_url:
        # Accept a Java-style jdbc:postgresql://... URL too, just in case.
        if database_url.startswith("jdbc:"):
            database_url = database_url[len("jdbc:"):]
        return database_url

    host = env("DB_HOST", default="localhost")
    port = env("DB_PORT", default="5432")
    name = env("DB_NAME", default="rr_lms")
    user = env("DB_USERNAME", "DB_USER", default="lms_user")
    password = env("DB_PASSWORD", default="")

    return f"postgresql://{user}:{password}@{host}:{port}/{name}"


def hash_password(plain: str) -> str:
    return bcrypt.hashpw(plain.encode("utf-8"), bcrypt.gensalt(rounds=BCRYPT_ROUNDS)).decode("utf-8")


def slugify(text: str) -> str:
    out = []
    prev_dash = False
    for ch in text.lower():
        if ch.isalnum():
            out.append(ch)
            prev_dash = False
        elif not prev_dash:
            out.append("-")
            prev_dash = True
    return "".join(out).strip("-")


# ============================================================================
# COUNTERS (for the final summary — only counts records this run actually inserted)
# ============================================================================

@dataclass
class Counters:
    students: int = 0
    students_skipped: int = 0
    faculty_devops: int = 0
    faculty_data: int = 0
    faculty_skipped: int = 0
    courses: int = 0
    courses_skipped: int = 0
    modules: int = 0
    modules_skipped: int = 0
    lessons: int = 0
    lessons_skipped: int = 0
    videos: int = 0
    videos_skipped: int = 0
    enrollments: int = 0
    enrollments_skipped: int = 0
    attendance: int = 0
    attendance_skipped: int = 0
    payments: int = 0
    fee_records: int = 0
    fee_records_skipped: int = 0
    certificates: int = 0
    certificates_skipped: int = 0
    notifications: int = 0
    notifications_skipped: int = 0
    companies: int = 0
    companies_skipped: int = 0
    placements: int = 0
    placements_skipped: int = 0
    placement_applications: int = 0
    placement_applications_skipped: int = 0
    quizzes: int = 0
    quizzes_skipped: int = 0
    quiz_questions: int = 0
    quiz_questions_skipped: int = 0
    quiz_attempts: int = 0
    quiz_attempts_skipped: int = 0
    errors: list = field(default_factory=list)


C = Counters()


# ============================================================================
# SMALL DB HELPERS
# ============================================================================

def fetch_one(cur, sql: str, params=()) -> Optional[dict]:
    cur.execute(sql, params)
    row = cur.fetchone()
    return row


def get_or_create_super_admin_id(cur) -> str:
    """
    Every table that records "who created this" (courses, lessons, fee
    structures, certificates, ...) has a NOT NULL FK to users(id). We never
    create a SUPER_ADMIN ourselves (the schema allows only one — see
    uq_single_super_admin — and the app's own DataSeeder already creates
    it), so we borrow whichever admin-level user already exists, falling
    back to the first demo faculty account we create in this run.
    """
    row = fetch_one(
        cur,
        "SELECT id FROM users WHERE role IN ('SUPER_ADMIN','ADMIN') ORDER BY role, created_at LIMIT 1",
    )
    return str(row["id"]) if row else None


# ============================================================================
# 1. FACULTY  (modeled as role=ADMIN users + admin_profiles; there is no
#    separate Faculty table in the existing schema. Courses reference an
#    instructor via courses.instructor_name / courses.instructor_id, both
#    of which are nullable columns reserved for exactly this purpose.)
# ============================================================================

DEVOPS_FACULTY = [
    ("Rajesh Kumar", "DevOps"),
    ("Suresh Reddy", "AWS & Cloud"),
    ("Naveen Sharma", "Kubernetes & Docker"),
    ("Vikram Rao", "Jenkins & CI/CD"),
    ("Anil Kumar", "Terraform & Infrastructure"),
    ("Prakash Reddy", "Linux & Cloud"),
]

DATA_ANALYTICS_FACULTY = [
    ("Priya Sharma", "Data Analytics"),
    ("Divya Reddy", "Power BI & SQL"),
]


def demo_email(full_name: str, domain: str, existing: set) -> str:
    base = slugify(full_name).replace("-", ".")
    candidate = f"{base}@{domain}"
    n = 1
    while candidate in existing:
        n += 1
        candidate = f"{base}{n}@{domain}"
    existing.add(candidate)
    return candidate


def seed_faculty(cur) -> dict:
    """Returns {full_name: {"id": ..., "specialization": ..., "department": ...}}"""
    faculty = {}
    used_emails = set()
    phone_seq = 1

    def upsert_faculty(full_name: str, specialization: str, department: str, is_devops: bool):
        nonlocal phone_seq
        email = demo_email(full_name, "faculty.rrtechnosoft.demo", used_emails)

        row = fetch_one(cur, "SELECT id FROM users WHERE email = %s", (email,))
        if row:
            faculty[full_name] = {"id": str(row["id"]), "specialization": specialization, "department": department}
            C.faculty_skipped += 1
            return

        cur.execute(
            """
            INSERT INTO users (role, email, password_hash, full_name, phone, status)
            VALUES ('ADMIN'::user_role, %s, %s, %s, %s, 'ACTIVE'::account_status)
            ON CONFLICT (email) DO NOTHING
            RETURNING id
            """,
            (email, hash_password(DEMO_PASSWORD), full_name, f"+91-90000{phone_seq:05d}"),
        )
        phone_seq += 1
        row = cur.fetchone()
        if row is None:
            row = fetch_one(cur, "SELECT id FROM users WHERE email = %s", (email,))
            faculty[full_name] = {"id": str(row["id"]), "specialization": specialization, "department": department}
            C.faculty_skipped += 1
            return

        user_id = row["id"]
        cur.execute(
            """
            INSERT INTO admin_profiles (user_id, department, designation)
            VALUES (%s, %s, %s)
            ON CONFLICT (user_id) DO NOTHING
            """,
            (user_id, department, f"Faculty — {specialization}"),
        )
        faculty[full_name] = {"id": str(user_id), "specialization": specialization, "department": department}
        if is_devops:
            C.faculty_devops += 1
        else:
            C.faculty_data += 1

    for name, spec in DEVOPS_FACULTY:
        upsert_faculty(name, spec, "DevOps & Cloud", is_devops=True)
    for name, spec in DATA_ANALYTICS_FACULTY:
        upsert_faculty(name, spec, "Data Analytics", is_devops=False)

    return faculty


# ============================================================================
# 2. STUDENTS
# ============================================================================

SEED_STUDENT_NAMES = [
    "Aarav Sharma", "Aditya Reddy", "Ananya Reddy", "Arjun Kumar", "Bhavya Rao",
    "Charan Teja", "Deepika Reddy", "Harsha Vardhan", "Karthik Varma", "Keerthana Rao",
    "Manoj Kumar", "Nandini Sharma", "Nikhil Reddy", "Pooja Verma", "Pranav Kumar",
    "Rahul Reddy", "Sandeep Kumar", "Sneha Reddy", "Srinivas Rao", "Varun Teja",
]

BRANCHES = [
    "Computer Science", "Information Technology", "Electronics & Communication",
    "Electrical & Electronics", "Mechanical Engineering",
]

COLLEGES = [
    "JNTU Hyderabad", "Osmania University", "CBIT Hyderabad", "VNR Vignana Jyothi Institute",
    "Andhra University", "JNTU Anantapur", "Vasavi College of Engineering",
    "Chaitanya Bharathi Institute of Technology", "Gokaraju Rangaraju Institute of Engineering",
    "Sreenidhi Institute of Science and Technology",
]

BATCHES = ["2025-A", "2025-B", "2026-A", "2026-B", "2026-C"]


def next_student_id(cur) -> str:
    """
    Mirrors StudentIdGenerator.java exactly: RRT<year>S<seq>, scoped to the
    current calendar year, scanning forward past any collision.
    """
    year = date.today().year
    prefix = f"RRT{year}S"
    row = fetch_one(cur, "SELECT count(*) AS n FROM users WHERE student_id LIKE %s", (prefix + "%",))
    seq = row["n"] + 1
    while True:
        candidate = f"{prefix}{seq:04d}"
        exists = fetch_one(cur, "SELECT 1 FROM users WHERE student_id = %s", (candidate,))
        if not exists:
            return candidate
        seq += 1


def build_student_roster(total: int) -> list:
    """Curated Indian names first, padded with Faker(en_IN)-generated names to reach `total`."""
    roster = list(SEED_STUDENT_NAMES)
    seen = set(roster)
    while len(roster) < total:
        name = f"{fake.first_name()} {fake.last_name()}"
        if name not in seen:
            seen.add(name)
            roster.append(name)
    return roster[:total]


def seed_students(cur, total: int) -> list:
    students = []
    used_emails = set()
    roster = build_student_roster(total)

    for i, full_name in enumerate(roster):
        email = demo_email(full_name, "student.demo", used_emails)

        existing = fetch_one(cur, "SELECT id, student_id FROM users WHERE email = %s", (email,))
        if existing:
            students.append({"id": str(existing["id"]), "student_id": existing["student_id"], "full_name": full_name})
            C.students_skipped += 1
            continue

        student_id = next_student_id(cur)
        phone = f"+91-98{(10000000 + i):08d}"[:14]

        cur.execute(
            """
            INSERT INTO users (role, student_id, email, password_hash, full_name, phone, status)
            VALUES ('STUDENT'::user_role, %s, %s, %s, %s, %s, 'ACTIVE'::account_status)
            ON CONFLICT (student_id) DO NOTHING
            RETURNING id
            """,
            (student_id, email, hash_password(DEMO_PASSWORD), full_name, phone),
        )
        row = cur.fetchone()
        if row is None:
            row = fetch_one(cur, "SELECT id FROM users WHERE student_id = %s", (student_id,))
            students.append({"id": str(row["id"]), "student_id": student_id, "full_name": full_name})
            C.students_skipped += 1
            continue

        user_id = row["id"]
        branch = BRANCHES[i % len(BRANCHES)]
        college = COLLEGES[i % len(COLLEGES)]
        batch = BATCHES[i % len(BATCHES)]
        grad_year = int(batch.split("-")[0])

        cur.execute(
            """
            INSERT INTO student_profiles (user_id, batch, branch, college, graduation_year, github_url)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (user_id) DO NOTHING
            """,
            (user_id, batch, branch, college, grad_year, f"https://github.com/{slugify(full_name)}"),
        )

        students.append({"id": str(user_id), "student_id": student_id, "full_name": full_name})
        C.students += 1

    return students


# ============================================================================
# 3. COURSES
# ============================================================================

# (title, category, level, weeks, price)
DEVOPS_COURSES = [
    ("Linux Administration", "DevOps", "BEGINNER", 6, "12000.00"),
    ("Git & GitHub", "DevOps", "BEGINNER", 3, "6000.00"),
    ("Docker", "DevOps", "INTERMEDIATE", 5, "15000.00"),
    ("Kubernetes", "DevOps", "INTERMEDIATE", 6, "18000.00"),
    ("Jenkins", "DevOps", "INTERMEDIATE", 4, "14000.00"),
    ("CI/CD", "DevOps", "INTERMEDIATE", 4, "14000.00"),
    ("Terraform", "DevOps", "INTERMEDIATE", 5, "16000.00"),
    ("AWS DevOps", "DevOps", "ADVANCED", 8, "28000.00"),
    ("DevSecOps", "DevOps", "ADVANCED", 6, "22000.00"),
    ("Monitoring & Observability", "DevOps", "ADVANCED", 4, "16000.00"),
]

CLOUD_COURSES = [
    ("AWS Fundamentals", "Cloud", "BEGINNER", 4, "10000.00"),
    ("Amazon EC2", "Cloud", "BEGINNER", 3, "9000.00"),
    ("Amazon VPC", "Cloud", "INTERMEDIATE", 3, "9500.00"),
    ("AWS IAM", "Cloud", "BEGINNER", 2, "7000.00"),
    ("Amazon S3", "Cloud", "BEGINNER", 2, "7000.00"),
    ("Amazon RDS", "Cloud", "INTERMEDIATE", 3, "9500.00"),
    ("Elastic Load Balancing", "Cloud", "INTERMEDIATE", 2, "8000.00"),
    ("Auto Scaling", "Cloud", "INTERMEDIATE", 2, "8000.00"),
    ("CloudFront", "Cloud", "INTERMEDIATE", 2, "7500.00"),
    ("Route 53", "Cloud", "INTERMEDIATE", 2, "7000.00"),
    ("CloudWatch", "Cloud", "INTERMEDIATE", 2, "7500.00"),
    ("Amazon ECR", "Cloud", "ADVANCED", 2, "8500.00"),
    ("Amazon ECS", "Cloud", "ADVANCED", 4, "16000.00"),
    ("Amazon EKS", "Cloud", "ADVANCED", 5, "20000.00"),
]

DATA_ANALYTICS_COURSES = [
    ("SQL", "Data Analytics", "BEGINNER", 4, "9000.00"),
    ("Excel", "Data Analytics", "BEGINNER", 3, "6000.00"),
    ("Python for Data Analytics", "Data Analytics", "BEGINNER", 6, "14000.00"),
    ("Pandas", "Data Analytics", "INTERMEDIATE", 4, "11000.00"),
    ("NumPy", "Data Analytics", "INTERMEDIATE", 3, "9000.00"),
    ("Power BI", "Data Analytics", "INTERMEDIATE", 5, "15000.00"),
    ("Data Visualization", "Data Analytics", "INTERMEDIATE", 4, "12000.00"),
    ("Statistics", "Data Analytics", "INTERMEDIATE", 5, "13000.00"),
    ("Data Cleaning", "Data Analytics", "BEGINNER", 2, "7000.00"),
    ("Dashboard Development", "Data Analytics", "ADVANCED", 4, "14000.00"),
]

JAVA_COURSES = [
    ("Core Java", "Programming", "BEGINNER", 6, "10000.00"),
    ("Object-Oriented Programming", "Programming", "BEGINNER", 4, "8000.00"),
    ("Java Collections", "Programming", "INTERMEDIATE", 3, "7000.00"),
    ("Exception Handling", "Programming", "BEGINNER", 2, "5000.00"),
    ("Multithreading", "Programming", "ADVANCED", 4, "10000.00"),
    ("JDBC", "Programming", "INTERMEDIATE", 3, "7000.00"),
    ("REST API Concepts", "Programming", "INTERMEDIATE", 3, "8000.00"),
    ("Spring Boot Concepts", "Programming", "ADVANCED", 6, "16000.00"),
    ("Java Full Stack", "Programming", "ADVANCED", 12, "32000.00"),
]

COURSE_DESCRIPTIONS = {
    "DevOps": "Hands-on {title} training as part of the DevOps track — practical labs, real-world workflows, and production-style exercises.",
    "Cloud": "A focused, hands-on course on {title}, part of the AWS Cloud curriculum.",
    "Data Analytics": "Practical, project-based {title} training for the Data Analytics track.",
    "Programming": "{title} as part of the Java educational track — core concepts explained with hands-on examples.",
}


def assign_instructor(category: str, index: int, faculty: dict):
    if category == "DevOps":
        pool = [n for n, _ in DEVOPS_FACULTY]
    elif category == "Cloud":
        # Cloud-leaning specialists first, falling back across the DevOps roster.
        pool = ["Suresh Reddy", "Prakash Reddy"] + [n for n, _ in DEVOPS_FACULTY]
    elif category == "Data Analytics":
        pool = [n for n, _ in DATA_ANALYTICS_FACULTY]
    else:
        return None, None  # Java educational content: no dedicated faculty seeded for it
    name = pool[index % len(pool)]
    return name, faculty[name]["id"]


def seed_courses(cur, faculty: dict) -> list:
    all_defs = (
        [(t, c, l, w, p) for (t, c, l, w, p) in DEVOPS_COURSES]
        + [(t, c, l, w, p) for (t, c, l, w, p) in CLOUD_COURSES]
        + [(t, c, l, w, p) for (t, c, l, w, p) in DATA_ANALYTICS_COURSES]
        + [(t, c, l, w, p) for (t, c, l, w, p) in JAVA_COURSES]
    )

    creator_id = get_or_create_super_admin_id(cur)
    courses = []
    per_category_index = {}

    for title, category, level, weeks, price in all_defs:
        slug = slugify(title)
        idx = per_category_index.get(category, 0)
        per_category_index[category] = idx + 1

        instructor_name, instructor_id = assign_instructor(category, idx, faculty)
        if creator_id is None:
            creator_id = instructor_id  # first faculty ever created stands in as the creator

        existing = fetch_one(cur, "SELECT id, title, category FROM courses WHERE slug = %s", (slug,))
        if existing:
            courses.append({
                "id": str(existing["id"]), "title": title, "slug": slug, "category": category,
            })
            C.courses_skipped += 1
            continue

        description = COURSE_DESCRIPTIONS[category].format(title=title)

        cur.execute(
            """
            INSERT INTO courses (title, slug, category, description, level, status,
                                  duration_weeks, instructor_name, instructor_id, price, created_by)
            VALUES (%s, %s, %s, %s, %s::course_level, 'PUBLISHED'::course_status,
                    %s, %s, %s, %s, %s)
            ON CONFLICT (slug) DO NOTHING
            RETURNING id
            """,
            (title, slug, category, description, level, weeks, instructor_name, instructor_id,
             Decimal(price), creator_id),
        )
        row = cur.fetchone()
        if row is None:
            row = fetch_one(cur, "SELECT id FROM courses WHERE slug = %s", (slug,))
            courses.append({"id": str(row["id"]), "title": title, "slug": slug, "category": category})
            C.courses_skipped += 1
            continue

        courses.append({"id": str(row["id"]), "title": title, "slug": slug, "category": category})
        C.courses += 1

    return courses, creator_id


# ============================================================================
# 4. MODULES / LESSONS
# ============================================================================

MODULE_TEMPLATE = ["Introduction & Setup", "Core Concepts", "Hands-on Practice", "Assessment & Wrap-up"]


def seed_modules_and_lessons(cur, courses: list, creator_id: str):
    for course in courses:
        for m_pos, module_title in enumerate(MODULE_TEMPLATE):
            existing_module = fetch_one(
                cur, "SELECT id FROM course_modules WHERE course_id = %s AND position = %s",
                (course["id"], m_pos),
            )
            if existing_module:
                module_id = existing_module["id"]
                C.modules_skipped += 1
            else:
                cur.execute(
                    """
                    INSERT INTO course_modules (course_id, title, position)
                    VALUES (%s, %s, %s)
                    ON CONFLICT (course_id, position) DO NOTHING
                    RETURNING id
                    """,
                    (course["id"], module_title, m_pos),
                )
                row = cur.fetchone()
                if row is None:
                    row = fetch_one(
                        cur, "SELECT id FROM course_modules WHERE course_id = %s AND position = %s",
                        (course["id"], m_pos),
                    )
                    C.modules_skipped += 1
                else:
                    C.modules += 1
                module_id = row["id"]

            for l_pos in range(2):
                existing_lesson = fetch_one(
                    cur, "SELECT id FROM lessons WHERE module_id = %s AND position = %s",
                    (module_id, l_pos),
                )
                if existing_lesson:
                    C.lessons_skipped += 1
                    continue

                is_video = (l_pos == 0)
                lesson_title = f"{module_title} — {course['title']} (Lesson {l_pos + 1})"
                cur.execute(
                    """
                    INSERT INTO lessons (module_id, title, content_type, video_url, body,
                                          duration_minutes, position, is_free_preview, created_by)
                    VALUES (%s, %s, %s::content_type, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (module_id, position) DO NOTHING
                    """,
                    (
                        module_id, lesson_title, "VIDEO" if is_video else "ARTICLE",
                        f"https://cdn.rrtechnosoft.demo/videos/{course['slug']}-m{m_pos}-l{l_pos}.mp4" if is_video else None,
                        None if is_video else "Reading material and notes for this lesson. (Demo content.)",
                        20 + l_pos * 10, l_pos, (m_pos == 0 and l_pos == 0), creator_id,
                    ),
                )
                if cur.rowcount:
                    C.lessons += 1
                else:
                    C.lessons_skipped += 1


# ============================================================================
# 5. VIDEOS (video_resources — no unique constraint in the schema beyond id,
#    so we dedupe on title, which is unique within this demo set)
# ============================================================================

VIDEO_DEFS = [
    ("Linux Commands for DevOps", "DEVOPS"),
    ("Docker Containers Explained", "DEVOPS"),
    ("Kubernetes Pods and Deployments", "DEVOPS"),
    ("Jenkins CI/CD Pipeline", "DEVOPS"),
    ("Terraform Infrastructure as Code", "DEVOPS"),
    ("AWS EC2 Complete Guide", "CLOUD"),
    ("AWS VPC Explained", "CLOUD"),
    ("AWS IAM Fundamentals", "CLOUD"),
    ("SQL for Data Analytics", "DATA_ANALYTICS"),
    ("Power BI Dashboard Tutorial", "DATA_ANALYTICS"),
    ("Python Pandas Introduction", "DATA_ANALYTICS"),
    ("Core Java OOP", "JAVA"),
    ("Java Collections", "JAVA"),
    ("Java Exception Handling", "JAVA"),
]

CATEGORY_TO_COURSE_CATEGORY = {
    "DEVOPS": "DevOps", "CLOUD": "Cloud", "DATA_ANALYTICS": "Data Analytics", "JAVA": "Programming",
}


def seed_videos(cur, courses: list, uploader_id: str):
    by_category = {}
    for c in courses:
        by_category.setdefault(c["category"], []).append(c)

    for title, video_category in VIDEO_DEFS:
        existing = fetch_one(cur, "SELECT 1 FROM video_resources WHERE title = %s", (title,))
        if existing:
            C.videos_skipped += 1
            continue

        course_category = CATEGORY_TO_COURSE_CATEGORY[video_category]
        matching_courses = by_category.get(course_category, [])
        course_id = matching_courses[0]["id"] if matching_courses else None
        video_key = slugify(title)

        cur.execute(
            """
            INSERT INTO video_resources (title, description, category, course_id, source,
                                          video_url, video_key, duration_seconds, is_published, uploaded_by)
            VALUES (%s, %s, %s, %s, 'UPLOAD'::video_source, %s, %s, %s, true, %s)
            """,
            (
                title, f"Demo educational video: {title}.", video_category, course_id,
                f"https://cdn.rrtechnosoft.demo/library/{video_key}.mp4", video_key,
                random.randint(300, 1800), uploader_id,
            ),
        )
        C.videos += 1


# ============================================================================
# 6. ENROLLMENTS
# ============================================================================

def seed_enrollments(cur, students: list, courses: list) -> list:
    """
    Deterministic (not every student in every course): each student is
    enrolled in one primary course by index, and roughly every third
    student picks up a second course — matching the existing DemoDataSeeder's
    pattern of a realistic, uneven course load.
    """
    enrollments = []  # (student_id, course_id, is_completed)
    n = len(courses)
    if n == 0:
        return enrollments

    for i, student in enumerate(students):
        picks = [courses[i % n]]
        if i % 3 == 0:
            picks.append(courses[(i * 7 + 3) % n])

        for j, course in enumerate(picks):
            # Deterministic "how far along" so certificates/attendance have something to work with.
            completed = (i + j) % 7 == 0
            progress = Decimal("100.00") if completed else Decimal(str(random.choice([15, 30, 45, 60, 75, 90])))
            status = "COMPLETED" if completed else "ACTIVE"

            existing = fetch_one(
                cur, "SELECT id FROM enrollments WHERE student_id = %s AND course_id = %s",
                (student["id"], course["id"]),
            )
            if existing:
                C.enrollments_skipped += 1
                enrollments.append((student, course, completed))
                continue

            cur.execute(
                """
                INSERT INTO enrollments (student_id, course_id, status, progress_pct, completed_at)
                VALUES (%s, %s, %s::enrollment_status, %s, %s)
                ON CONFLICT (student_id, course_id) DO NOTHING
                """,
                (student["id"], course["id"], status, progress,
                 (datetime.now(timezone.utc) - timedelta(days=5)) if completed else None),
            )
            if cur.rowcount:
                C.enrollments += 1
            else:
                C.enrollments_skipped += 1
            enrollments.append((student, course, completed))

    return enrollments


# ============================================================================
# 7. ATTENDANCE
# ============================================================================

def recent_weekdays(n: int, end: date) -> list:
    days = []
    d = end
    while len(days) < n:
        if d.weekday() < 5:  # Mon-Fri
            days.append(d)
        d -= timedelta(days=1)
    return days


def seed_attendance(cur, enrollments: list, marker_id: str):
    session_days = recent_weekdays(15, date.today() - timedelta(days=1))

    for student, course, _completed in enrollments:
        for d in session_days:
            existing = fetch_one(
                cur,
                "SELECT 1 FROM attendance WHERE course_id = %s AND student_id = %s AND attendance_date = %s",
                (course["id"], student["id"], d),
            )
            if existing:
                C.attendance_skipped += 1
                continue

            roll = random.random()
            if roll < 0.87:
                status = "PRESENT"
            elif roll < 0.97:
                status = "ABSENT"
            else:
                status = "LATE"

            cur.execute(
                """
                INSERT INTO attendance (course_id, student_id, attendance_date, status, marked_by)
                VALUES (%s, %s, %s, %s::attendance_status, %s)
                ON CONFLICT (course_id, student_id, attendance_date) DO NOTHING
                """,
                (course["id"], student["id"], d, status, marker_id),
            )
            if cur.rowcount:
                C.attendance += 1
            else:
                C.attendance_skipped += 1


# ============================================================================
# 8. FINANCE (fee_structures, student_fees, payments, receipts)
# ============================================================================

def get_or_create_fee_structure(cur, course: dict, price: Decimal, creator_id: str) -> str:
    existing = fetch_one(cur, "SELECT id FROM fee_structures WHERE course_id = %s", (course["id"],))
    if existing:
        return str(existing["id"])

    cur.execute(
        """
        INSERT INTO fee_structures (course_id, name, description, total_amount, installment_count, created_by)
        VALUES (%s, %s, %s, %s, 2, %s)
        RETURNING id
        """,
        (course["id"], f"{course['title']} — Standard Fee", "Full program fee, payable in up to 2 installments.",
         price, creator_id),
    )
    return str(cur.fetchone()["id"])


def next_receipt_number(cur) -> str:
    row = fetch_one(cur, "SELECT count(*) AS n FROM receipts WHERE receipt_number LIKE %s", ("RRT-RCPT-DEMO-%",))
    return f"RRT-RCPT-DEMO-{row['n'] + 1:04d}"


STATUS_CYCLE = ["PAID", "PARTIAL", "PENDING", "OVERDUE"]


def seed_finance(cur, enrollments: list, course_price_by_id: dict, creator_id: str):
    for i, (student, course, completed) in enumerate(enrollments):
        total = course_price_by_id[course["id"]]
        fee_structure_id = get_or_create_fee_structure(cur, course, total, creator_id)

        status = "PAID" if completed else STATUS_CYCLE[i % len(STATUS_CYCLE)]
        if status == "PAID":
            amount_paid = total
        elif status == "PARTIAL":
            amount_paid = (total / 2).quantize(Decimal("0.01"))
        else:
            amount_paid = Decimal("0.00")

        cur.execute(
            """
            INSERT INTO student_fees (student_id, course_id, fee_structure_id, total_amount,
                                       net_payable, amount_paid, status, assigned_by)
            VALUES (%s, %s, %s, %s, %s, %s, %s::fee_status, %s)
            ON CONFLICT (student_id, course_id) WHERE course_id IS NOT NULL DO NOTHING
            RETURNING id
            """,
            (student["id"], course["id"], fee_structure_id, total, total, amount_paid, status, creator_id),
        )
        row = cur.fetchone()
        if row is None:
            C.fee_records_skipped += 1
            continue  # already existed from a previous run — never re-create its payment/receipt

        C.fee_records += 1
        student_fee_id = row["id"]

        if amount_paid > 0:
            cur.execute(
                """
                INSERT INTO payments (student_fee_id, student_id, amount, method, gateway_provider,
                                       status, recorded_by, paid_at)
                VALUES (%s, %s, %s, 'UPI'::payment_method, 'MANUAL'::payment_gateway_provider,
                        'SUCCESS'::payment_status, %s, %s)
                RETURNING id
                """,
                (student_fee_id, student["id"], amount_paid, creator_id,
                 datetime.now(timezone.utc) - timedelta(days=3)),
            )
            payment_id = cur.fetchone()["id"]
            C.payments += 1

            cur.execute(
                """
                INSERT INTO receipts (payment_id, student_fee_id, receipt_number, amount, generated_by)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (payment_id, student_fee_id, next_receipt_number(cur), amount_paid, creator_id),
            )


# ============================================================================
# 9. CERTIFICATES
# ============================================================================

def next_certificate_no(cur) -> str:
    row = fetch_one(cur, "SELECT count(*) AS n FROM certificates WHERE certificate_no LIKE %s", ("RRT-CERT-2026-%",))
    return f"RRT-CERT-2026-{row['n'] + 1:06d}"


def seed_certificates(cur, enrollments: list, issuer_id: str):
    for student, course, completed in enrollments:
        if not completed:
            continue

        existing = fetch_one(
            cur, "SELECT 1 FROM certificates WHERE student_id = %s AND course_id = %s",
            (student["id"], course["id"]),
        )
        if existing:
            C.certificates_skipped += 1
            continue

        cert_no = next_certificate_no(cur)
        cur.execute(
            """
            INSERT INTO certificates (student_id, course_id, certificate_no, issued_by)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (student_id, course_id) DO NOTHING
            """,
            (student["id"], course["id"], cert_no, issuer_id),
        )
        if cur.rowcount:
            C.certificates += 1
        else:
            C.certificates_skipped += 1


# ============================================================================
# 10. NOTIFICATIONS
# ============================================================================

NOTIFICATION_TEMPLATES = [
    ("ANNOUNCEMENT", "New course assigned", "You've been enrolled in a new course. Check My Courses to get started."),
    ("SYSTEM", "Attendance updated", "Your attendance record has been updated by your instructor."),
    ("ASSIGNMENT", "Assignment deadline approaching", "An assignment deadline is coming up soon — don't miss it."),
    ("SYSTEM", "Payment reminder", "A fee installment is due soon. Visit the Finance section for details."),
    ("CERTIFICATE", "Certificate available", "A new certificate is available for download on your profile."),
    ("ANNOUNCEMENT", "New video uploaded", "A new video has been added to the library for your track."),
]


def seed_notifications(cur, students: list):
    for student in students:
        for ntype, title, body in NOTIFICATION_TEMPLATES:
            existing = fetch_one(
                cur, "SELECT 1 FROM notifications WHERE user_id = %s AND title = %s",
                (student["id"], title),
            )
            if existing:
                C.notifications_skipped += 1
                continue

            cur.execute(
                """
                INSERT INTO notifications (user_id, type, title, body)
                VALUES (%s, %s::notification_type, %s, %s)
                """,
                (student["id"], ntype, title, body),
            )
            C.notifications += 1


# ============================================================================
# 11. COMPANIES  (real Hyderabad tech employers — used to give the Placement
#    module's Company Directory something real to browse. Public names /
#    industries / general campus locations only; no invented private contact
#    details for the real organizations — contact fields point at a clearly
#    fictitious RR Technosoft placement-cell inbox, and application links
#    point at a demo application portal rather than any real corporate URL.)
# ============================================================================

# (name, industry, description, hyderabad location, public careers site)
HYDERABAD_COMPANIES = [
    ("Amazon Development Center India", "E-commerce & Cloud Computing",
     "Major India engineering campus contributing to AWS and Amazon retail systems.",
     "Financial District, Hyderabad, Telangana", "https://www.amazon.jobs"),
    ("Microsoft India Development Center", "Software & Cloud Services",
     "One of Microsoft's largest engineering hubs outside the US, working on Azure and enterprise cloud products.",
     "HITEC City, Hyderabad, Telangana", "https://careers.microsoft.com"),
    ("Google Hyderabad", "Internet & Cloud Services",
     "Google's largest campus outside the United States, spanning search, cloud, and infrastructure engineering.",
     "Kondapur, Hyderabad, Telangana", "https://careers.google.com"),
    ("Deloitte USI", "IT Consulting & Professional Services",
     "Deloitte's US-India delivery centers, including cloud, DevOps, and digital transformation practices.",
     "Gachibowli, Hyderabad, Telangana", "https://www2.deloitte.com/us/en/careers.html"),
    ("Accenture", "IT Services & Consulting",
     "Global professional services company with a large Hyderabad delivery center for cloud and DevOps engagements.",
     "Financial District, Hyderabad, Telangana", "https://www.accenture.com/in-en/careers"),
    ("Cognizant", "IT Services & Consulting",
     "IT services company with a major Hyderabad delivery center supporting cloud infrastructure and DevOps practices.",
     "Gachibowli, Hyderabad, Telangana", "https://careers.cognizant.com"),
    ("Capgemini", "IT Consulting & Technology Services",
     "Global technology consulting firm with a large Hyderabad presence across cloud and infrastructure services.",
     "Financial District, Hyderabad, Telangana", "https://www.capgemini.com/careers/"),
    ("Salesforce Hyderabad", "Cloud Software",
     "CRM and cloud platform company with an India engineering hub in Hyderabad.",
     "Raidurg, Hyderabad, Telangana", "https://www.salesforce.com/company/careers/"),
    ("ServiceNow India", "Cloud Software",
     "Enterprise cloud platform company with an India development center in Hyderabad.",
     "Knowledge City, Hyderabad, Telangana", "https://careers.servicenow.com"),
    ("Qualcomm India", "Semiconductors & Technology",
     "Qualcomm's India R&D center in Hyderabad, working across connectivity and cloud-integrated platforms.",
     "Mindspace, Hyderabad, Telangana", "https://www.qualcomm.com/company/careers"),
    ("Tech Mahindra", "IT Services & Consulting",
     "IT services and consulting company with a large delivery center in Hyderabad.",
     "Hitech City, Hyderabad, Telangana", "https://careers.techmahindra.com"),
    ("Cyient", "Engineering & IT Services",
     "Hyderabad-headquartered engineering and digital services company.",
     "Gachibowli, Hyderabad, Telangana", "https://www.cyient.com/careers"),
]


def seed_companies(cur, creator_id: str) -> dict:
    """Returns {name: company_id}"""
    companies = {}
    for name, industry, description, address, website in HYDERABAD_COMPANIES:
        existing = fetch_one(cur, "SELECT id FROM companies WHERE lower(name) = lower(%s)", (name,))
        if existing:
            companies[name] = str(existing["id"])
            C.companies_skipped += 1
            continue

        slug = slugify(name)
        cur.execute(
            """
            INSERT INTO companies (name, website, industry, description, contact_person_name,
                                    contact_email, contact_phone, address, is_active, created_by)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, true, %s)
            ON CONFLICT (lower(name)) DO NOTHING
            RETURNING id
            """,
            (
                name, website, industry, description,
                "RR Technosoft Placement Cell",
                f"campus.hiring@{slug}.rrtechnosoft-demo.local",
                "+91-90000-00000",
                address, creator_id,
            ),
        )
        row = cur.fetchone()
        if row is None:
            row = fetch_one(cur, "SELECT id FROM companies WHERE lower(name) = lower(%s)", (name,))
            companies[name] = str(row["id"])
            C.companies_skipped += 1
            continue

        companies[name] = str(row["id"])
        C.companies += 1

    return companies


# ============================================================================
# 12. PLACEMENTS (Job Drives) — DevOps openings at the companies above
# ============================================================================

# (role_title, skills_required, salary_min, salary_max)
DEVOPS_OPENINGS = [
    ("DevOps Engineer", ["Linux", "Docker", "Kubernetes", "CI/CD", "AWS"], "600000.00", "1200000.00"),
    ("Cloud Infrastructure Engineer", ["AWS", "Terraform", "Networking", "Linux"], "700000.00", "1400000.00"),
    ("Site Reliability Engineer", ["Kubernetes", "Monitoring", "Linux", "Python"], "900000.00", "1800000.00"),
    ("AWS DevOps Engineer", ["AWS", "Jenkins", "Terraform", "Docker"], "650000.00", "1300000.00"),
    ("Platform Engineer — Kubernetes", ["Kubernetes", "Docker", "Helm", "CI/CD"], "800000.00", "1600000.00"),
    ("CI/CD Engineer", ["Jenkins", "Git", "Docker", "Linux"], "550000.00", "1100000.00"),
    ("Cloud DevOps Consultant", ["AWS", "Azure", "Terraform", "DevSecOps"], "750000.00", "1500000.00"),
    ("Infrastructure Automation Engineer", ["Terraform", "Ansible", "AWS", "Linux"], "700000.00", "1350000.00"),
]

ALLOWED_BRANCHES_DEVOPS = ["Computer Science", "Information Technology", "Electronics & Communication"]


def seed_placements(cur, companies: dict, poster_id: str) -> list:
    placements = []
    company_names = list(companies.keys())

    for i, company_name in enumerate(company_names):
        role_title, skills, salary_min, salary_max = DEVOPS_OPENINGS[i % len(DEVOPS_OPENINGS)]

        existing = fetch_one(
            cur, "SELECT id, status FROM placements WHERE company_name = %s AND role_title = %s",
            (company_name, role_title),
        )
        if existing:
            placements.append({"id": str(existing["id"]), "company_name": company_name, "role_title": role_title})
            C.placements_skipped += 1
            continue

        last_date = date.today() + timedelta(days=21 + (i * 3) % 30)
        drive_date = last_date + timedelta(days=7)
        status = "OPEN" if i % 5 != 4 else "CLOSED"
        slug = slugify(f"{company_name}-{role_title}")

        cur.execute(
            """
            INSERT INTO placements (company_id, company_name, role_title, description, eligibility,
                                     skills_required, allowed_branches, salary_min, salary_max, min_cgpa,
                                     location, job_type, drive_date, last_date_to_apply, application_link,
                                     status, posted_by)
            VALUES (%s, %s, %s, %s, %s, %s::jsonb, %s::jsonb, %s, %s, %s,
                    %s, 'FULL_TIME'::job_type, %s, %s, %s, %s::placement_status, %s)
            RETURNING id
            """,
            (
                companies[company_name], company_name, role_title,
                f"{company_name} is hiring for the {role_title} role as part of an on-campus DevOps recruitment drive.",
                "Final-year / recent graduates from the DevOps or Cloud & DevOps track. Sound Linux, "
                "scripting, and cloud fundamentals expected.",
                psycopg.types.json.Json(skills), psycopg.types.json.Json(ALLOWED_BRANCHES_DEVOPS),
                Decimal(salary_min), Decimal(salary_max), Decimal("6.50"),
                "Hyderabad, Telangana", drive_date, last_date,
                f"https://careers.rrtechnosoft.demo/apply/{slug}",
                status, poster_id,
            ),
        )
        row = cur.fetchone()
        placements.append({"id": str(row["id"]), "company_name": company_name, "role_title": role_title})
        C.placements += 1

    return placements


def seed_placement_applications(cur, students: list, placements: list):
    """A modest, deterministic set of student applications so the placement
    dashboard has real application-status variety, not just open listings."""
    if not placements:
        return
    statuses = ["APPLIED", "SHORTLISTED", "INTERVIEW_SCHEDULED", "SELECTED", "REJECTED"]

    applicants = students[: min(len(students), 15)]
    for i, student in enumerate(applicants):
        placement = placements[i % len(placements)]
        status = statuses[i % len(statuses)]

        existing = fetch_one(
            cur, "SELECT 1 FROM placement_applications WHERE placement_id = %s AND student_id = %s",
            (placement["id"], student["id"]),
        )
        if existing:
            C.placement_applications_skipped += 1
            continue

        cur.execute(
            """
            INSERT INTO placement_applications (placement_id, student_id, status)
            VALUES (%s, %s, %s::application_status)
            ON CONFLICT (placement_id, student_id) DO NOTHING
            """,
            (placement["id"], student["id"], status),
        )
        if cur.rowcount:
            C.placement_applications += 1
        else:
            C.placement_applications_skipped += 1


# ============================================================================
# 13. QUIZZES
# ============================================================================

# Each quiz is attached to the "Core Concepts" module (position 1, from
# MODULE_TEMPLATE above) of the named course slug.
QUIZ_DEFS = [
    {
        "course_slug": "docker",
        "title": "Docker Fundamentals Quiz",
        "questions": [
            ("Which command builds a Docker image from a Dockerfile?",
             [("A", "docker build"), ("B", "docker run"), ("C", "docker pull"), ("D", "docker exec")], "A"),
            ("What does the Docker command 'docker ps' show?",
             [("A", "All images"), ("B", "Running containers"), ("C", "Docker volumes"), ("D", "Network list")], "B"),
            ("Which file defines the steps to build a Docker image?",
             [("A", "docker-compose.yml"), ("B", "Dockerfile"), ("C", "manifest.json"), ("D", "image.yaml")], "B"),
            ("What is the purpose of a Docker volume?",
             [("A", "To persist data outside a container's lifecycle"), ("B", "To build images faster"),
              ("C", "To expose ports"), ("D", "To tag images")], "A"),
            ("Which command is used to run a container in detached mode?",
             [("A", "docker run -d"), ("B", "docker run -it"), ("C", "docker start -f"), ("D", "docker run -v")], "A"),
        ],
    },
    {
        "course_slug": "kubernetes",
        "title": "Kubernetes Basics Quiz",
        "questions": [
            ("What is the smallest deployable unit in Kubernetes?",
             [("A", "Node"), ("B", "Pod"), ("C", "Service"), ("D", "Container")], "B"),
            ("Which Kubernetes object ensures a specified number of pod replicas are running?",
             [("A", "ConfigMap"), ("B", "Deployment"), ("C", "Secret"), ("D", "Namespace")], "B"),
            ("What does 'kubectl get nodes' list?",
             [("A", "Running pods"), ("B", "Cluster nodes"), ("C", "Services"), ("D", "Persistent volumes")], "B"),
            ("Which component schedules pods onto nodes?",
             [("A", "kube-scheduler"), ("B", "etcd"), ("C", "kubelet"), ("D", "kube-proxy")], "A"),
            ("What is used to expose a set of pods as a network service?",
             [("A", "Ingress only"), ("B", "Service"), ("C", "ReplicaSet"), ("D", "DaemonSet")], "B"),
        ],
    },
    {
        "course_slug": "linux-administration",
        "title": "Linux Administration Basics Quiz",
        "questions": [
            ("Which command changes file permissions in Linux?",
             [("A", "chmod"), ("B", "chown"), ("C", "chgrp"), ("D", "chroot")], "A"),
            ("Which command shows currently running processes?",
             [("A", "ls"), ("B", "ps"), ("C", "df"), ("D", "cat")], "B"),
            ("What does the 'grep' command do?",
             [("A", "Compresses files"), ("B", "Searches text using patterns"),
              ("C", "Changes ownership"), ("D", "Schedules jobs")], "B"),
            ("Which directory typically holds system configuration files?",
             [("A", "/etc"), ("B", "/tmp"), ("C", "/dev"), ("D", "/proc")], "A"),
            ("Which command is used to view disk space usage?",
             [("A", "df"), ("B", "top"), ("C", "free"), ("D", "uptime")], "A"),
        ],
    },
    {
        "course_slug": "aws-fundamentals",
        "title": "AWS Cloud Basics Quiz",
        "questions": [
            ("Which AWS service provides scalable object storage?",
             [("A", "Amazon S3"), ("B", "Amazon EC2"), ("C", "Amazon RDS"), ("D", "Amazon VPC")], "A"),
            ("What does IAM stand for in AWS?",
             [("A", "Internet Access Management"), ("B", "Identity and Access Management"),
              ("C", "Infrastructure Automation Module"), ("D", "Instance Allocation Manager")], "B"),
            ("Which AWS service is used to run virtual servers in the cloud?",
             [("A", "Amazon EC2"), ("B", "Amazon S3"), ("C", "AWS Lambda"), ("D", "Amazon Route 53")], "A"),
            ("Which AWS service provides managed relational databases?",
             [("A", "Amazon RDS"), ("B", "Amazon DynamoDB"), ("C", "Amazon Redshift"), ("D", "Amazon Aurora Serverless")], "A"),
            ("What is the primary purpose of Amazon VPC?",
             [("A", "Object storage"), ("B", "Isolated virtual networking for AWS resources"),
              ("C", "Managed Kubernetes"), ("D", "Content delivery")], "B"),
        ],
    },
    {
        "course_slug": "ci-cd",
        "title": "CI/CD Concepts Quiz",
        "questions": [
            ("What does CI stand for in a CI/CD pipeline?",
             [("A", "Continuous Integration"), ("B", "Code Inspection"), ("C", "Container Instance"), ("D", "Change Implementation")], "A"),
            ("What is the main goal of Continuous Deployment?",
             [("A", "Manual code reviews only"), ("B", "Automatically releasing every change that passes tests to production"),
              ("C", "Writing documentation"), ("D", "Manually building artifacts")], "B"),
            ("Which tool is commonly used to automate CI/CD pipelines?",
             [("A", "Jenkins"), ("B", "Photoshop"), ("C", "Excel"), ("D", "Figma")], "A"),
            ("What is a 'build artifact' in a CI/CD pipeline?",
             [("A", "A test report only"), ("B", "The compiled/packaged output of a build step"),
              ("C", "A Git commit message"), ("D", "A deployment schedule")], "B"),
            ("Why are automated tests important in a CI/CD pipeline?",
             [("A", "They replace the need for version control"),
              ("B", "They catch regressions before code reaches production"),
              ("C", "They are only needed for UI changes"), ("D", "They slow down releases unnecessarily")], "B"),
        ],
    },
    {
        "course_slug": "sql",
        "title": "SQL for Data Analytics Quiz",
        "questions": [
            ("Which SQL clause is used to filter rows before grouping?",
             [("A", "HAVING"), ("B", "WHERE"), ("C", "GROUP BY"), ("D", "ORDER BY")], "B"),
            ("Which SQL keyword removes duplicate rows from a result set?",
             [("A", "DISTINCT"), ("B", "UNIQUE"), ("C", "FILTER"), ("D", "ONLY")], "A"),
            ("Which JOIN returns only matching rows from both tables?",
             [("A", "LEFT JOIN"), ("B", "INNER JOIN"), ("C", "FULL OUTER JOIN"), ("D", "CROSS JOIN")], "B"),
            ("Which aggregate function returns the number of rows?",
             [("A", "SUM()"), ("B", "COUNT()"), ("C", "AVG()"), ("D", "MAX()")], "B"),
            ("Which clause filters groups after a GROUP BY?",
             [("A", "WHERE"), ("B", "HAVING"), ("C", "LIMIT"), ("D", "ORDER BY")], "B"),
        ],
    },
    {
        "course_slug": "power-bi",
        "title": "Power BI Basics Quiz",
        "questions": [
            ("What language is used to write measures in Power BI?",
             [("A", "DAX"), ("B", "SQL"), ("C", "Python"), ("D", "M"), ], "A"),
            ("Which Power BI component is used to shape and transform data?",
             [("A", "Power Query Editor"), ("B", "Report view"), ("C", "Bookmark pane"), ("D", "Q&A visual")], "A"),
            ("What is a 'measure' in Power BI generally used for?",
             [("A", "Storing raw text"), ("B", "Performing calculations over aggregated data"),
              ("C", "Importing images"), ("D", "Setting file permissions")], "B"),
            ("Which file extension does a Power BI Desktop report use?",
             [("A", ".pbix"), ("B", ".pptx"), ("C", ".xlsx"), ("D", ".pbir")], "A"),
            ("What does a slicer visual do on a Power BI report page?",
             [("A", "Deletes a dataset"), ("B", "Lets users filter the report interactively"),
              ("C", "Publishes the report"), ("D", "Encrypts the data model")], "B"),
        ],
    },
    {
        "course_slug": "core-java",
        "title": "Core Java OOP Basics Quiz",
        "questions": [
            ("Which keyword is used to inherit a class in Java?",
             [("A", "implements"), ("B", "extends"), ("C", "inherits"), ("D", "super")], "B"),
            ("What is encapsulation in OOP?",
             [("A", "Hiding internal state behind methods"), ("B", "Creating multiple classes"),
              ("C", "Running code in parallel"), ("D", "Compiling code faster")], "A"),
            ("Which keyword is used to create an object in Java?",
             [("A", "new"), ("B", "class"), ("C", "this"), ("D", "static")], "A"),
            ("What does polymorphism allow in Java?",
             [("A", "A method to behave differently based on the object calling it"),
              ("B", "Multiple inheritance of classes"), ("C", "Faster garbage collection"),
              ("D", "Direct memory access")], "A"),
            ("Which access modifier restricts a member to its own class only?",
             [("A", "public"), ("B", "protected"), ("C", "private"), ("D", "default")], "C"),
        ],
    },
]


def seed_quizzes(cur, courses_by_slug: dict, creator_id: str) -> list:
    """Returns a list of {id, title} for quizzes newly created or already present, for use by attempts."""
    quizzes = []

    for qdef in QUIZ_DEFS:
        course = courses_by_slug.get(qdef["course_slug"])
        if course is None:
            continue  # course wasn't seeded (shouldn't happen, but keep this defensive)

        module = fetch_one(
            cur, "SELECT id FROM course_modules WHERE course_id = %s AND position = 1", (course["id"],),
        )
        module_id = module["id"] if module else None

        existing = fetch_one(cur, "SELECT id FROM quizzes WHERE title = %s", (qdef["title"],))
        if existing:
            quizzes.append({"id": str(existing["id"]), "title": qdef["title"]})
            C.quizzes_skipped += 1
            quiz_id = existing["id"]
        else:
            now = datetime.now(timezone.utc)
            cur.execute(
                """
                INSERT INTO quizzes (module_id, title, time_limit_minutes, pass_score_pct,
                                      available_from, available_to, created_by)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                RETURNING id
                """,
                (module_id, qdef["title"], 20, Decimal("60.00"),
                 now - timedelta(days=5), now + timedelta(days=60), creator_id),
            )
            quiz_id = cur.fetchone()["id"]
            quizzes.append({"id": str(quiz_id), "title": qdef["title"]})
            C.quizzes += 1

        for pos, (question_text, options, correct_key) in enumerate(qdef["questions"]):
            existing_q = fetch_one(
                cur, "SELECT 1 FROM quiz_questions WHERE quiz_id = %s AND position = %s", (quiz_id, pos),
            )
            if existing_q:
                C.quiz_questions_skipped += 1
                continue

            options_json = [{"key": k, "text": t} for k, t in options]
            cur.execute(
                """
                INSERT INTO quiz_questions (quiz_id, question, options, correct_option, position)
                VALUES (%s, %s, %s::jsonb, %s, %s)
                """,
                (quiz_id, question_text, psycopg.types.json.Json(options_json), correct_key, pos),
            )
            C.quiz_questions += 1

    return quizzes


def seed_quiz_attempts(cur, quizzes: list, students: list):
    """A handful of deterministic attempts per quiz so the assessment
    dashboard shows real scores, not just empty quizzes."""
    if not students:
        return

    for qi, quiz in enumerate(quizzes):
        questions = fetch_one_all(
            cur, "SELECT id, correct_option FROM quiz_questions WHERE quiz_id = %s ORDER BY position", (quiz["id"],),
        )
        if not questions:
            continue

        attempt_students = students[qi % len(students): qi % len(students) + 5] or students[:5]
        for si, student in enumerate(attempt_students):
            existing = fetch_one(
                cur, "SELECT 1 FROM quiz_attempts WHERE quiz_id = %s AND student_id = %s",
                (quiz["id"], student["id"]),
            )
            if existing:
                C.quiz_attempts_skipped += 1
                continue

            # Deterministic mix of correct/incorrect answers per student.
            answers = {}
            correct_count = 0
            for j, q in enumerate(questions):
                get_it_right = (si + j) % 3 != 0  # ~2/3 correct, varies per student/question
                if get_it_right:
                    answers[str(q["id"])] = q["correct_option"]
                    correct_count += 1
                else:
                    wrong = "A" if q["correct_option"] != "A" else "B"
                    answers[str(q["id"])] = wrong
            score_pct = Decimal(correct_count * 100) / Decimal(len(questions))
            score_pct = score_pct.quantize(Decimal("0.01"))

            cur.execute(
                """
                INSERT INTO quiz_attempts (quiz_id, student_id, answers, score_pct, submitted_at)
                VALUES (%s, %s, %s::jsonb, %s, %s)
                ON CONFLICT (quiz_id, student_id) DO NOTHING
                """,
                (quiz["id"], student["id"], psycopg.types.json.Json(answers), score_pct,
                 datetime.now(timezone.utc) - timedelta(days=2)),
            )
            if cur.rowcount:
                C.quiz_attempts += 1
            else:
                C.quiz_attempts_skipped += 1


def fetch_one_all(cur, sql: str, params=()) -> list:
    cur.execute(sql, params)
    return cur.fetchall()


# ============================================================================
# MAIN
# ============================================================================

def main():
    dsn = get_dsn()
    try:
        conn = psycopg.connect(dsn, row_factory=dict_row)
    except Exception as exc:
        sys.exit(f"Could not connect to the database.\n  DSN: {dsn}\n  Error: {exc}")

    try:
        with conn:
            with conn.cursor() as cur:
                print("Seeding faculty (DevOps + Data Analytics)...")
                faculty = seed_faculty(cur)

                print(f"Seeding {TOTAL_STUDENTS} Indian demo students...")
                students = seed_students(cur, TOTAL_STUDENTS)

                print("Seeding courses (DevOps, Cloud, Data Analytics, Java)...")
                courses, creator_id = seed_courses(cur, faculty)
                if creator_id is None:
                    raise RuntimeError(
                        "No admin/super-admin user exists to attribute created_by to, "
                        "and no faculty could be created either — aborting."
                    )
                course_price_by_id = {}
                all_defs = DEVOPS_COURSES + CLOUD_COURSES + DATA_ANALYTICS_COURSES + JAVA_COURSES
                price_by_slug = {slugify(t): Decimal(p) for (t, _c, _l, _w, p) in all_defs}
                for c in courses:
                    course_price_by_id[c["id"]] = price_by_slug[c["slug"]]

                print("Seeding modules & lessons...")
                seed_modules_and_lessons(cur, courses, creator_id)

                print("Seeding video library...")
                seed_videos(cur, courses, creator_id)

                print("Seeding enrollments...")
                enrollments = seed_enrollments(cur, students, courses)

                print("Seeding attendance history...")
                seed_attendance(cur, enrollments, creator_id)

                print("Seeding finance records (fees, payments, receipts)...")
                seed_finance(cur, enrollments, course_price_by_id, creator_id)

                print("Seeding certificates...")
                seed_certificates(cur, enrollments, creator_id)

                print("Seeding notifications...")
                seed_notifications(cur, students)

                print("Seeding Hyderabad companies (placement directory)...")
                companies = seed_companies(cur, creator_id)

                print("Seeding DevOps placement openings (job drives)...")
                placements = seed_placements(cur, companies, creator_id)
                seed_placement_applications(cur, students, placements)

                print("Seeding quizzes...")
                courses_by_slug = {c["slug"]: c for c in courses}
                quizzes = seed_quizzes(cur, courses_by_slug, creator_id)
                seed_quiz_attempts(cur, quizzes, students)

        # transaction committed by `with conn:` on clean exit
    except Exception as exc:
        conn.rollback()
        print(f"\nERROR — transaction rolled back. Nothing was left half-seeded.\n  Reason: {exc}", file=sys.stderr)
        raise
    finally:
        conn.close()

    print()
    print("=" * 60)
    print("RR TECHNOSOFT LMS — DEMO DATA SEED")
    print("=" * 60)
    print(f"Students created        : {C.students}  (skipped existing: {C.students_skipped})")
    print(f"DevOps faculty created   : {C.faculty_devops}")
    print(f"Data Analytics faculty   : {C.faculty_data}  (faculty skipped existing: {C.faculty_skipped})")
    print(f"Courses created          : {C.courses}  (skipped existing: {C.courses_skipped})")
    print(f"Modules created          : {C.modules}  (skipped existing: {C.modules_skipped})")
    print(f"Lessons created          : {C.lessons}  (skipped existing: {C.lessons_skipped})")
    print(f"Videos created           : {C.videos}  (skipped existing: {C.videos_skipped})")
    print(f"Enrollments created      : {C.enrollments}  (skipped existing: {C.enrollments_skipped})")
    print(f"Attendance created       : {C.attendance}  (skipped existing: {C.attendance_skipped})")
    print(f"Fee records created      : {C.fee_records}  (skipped existing: {C.fee_records_skipped})")
    print(f"Payments created         : {C.payments}")
    print(f"Certificates created     : {C.certificates}  (skipped existing: {C.certificates_skipped})")
    print(f"Notifications created    : {C.notifications}  (skipped existing: {C.notifications_skipped})")
    print(f"Companies created        : {C.companies}  (skipped existing: {C.companies_skipped})")
    print(f"Placements created       : {C.placements}  (skipped existing: {C.placements_skipped})")
    print(f"Placement applications   : {C.placement_applications}  (skipped existing: {C.placement_applications_skipped})")
    print(f"Quizzes created          : {C.quizzes}  (skipped existing: {C.quizzes_skipped})")
    print(f"Quiz questions created   : {C.quiz_questions}  (skipped existing: {C.quiz_questions_skipped})")
    print(f"Quiz attempts created    : {C.quiz_attempts}  (skipped existing: {C.quiz_attempts_skipped})")
    print("=" * 60)
    print("SEED COMPLETED SUCCESSFULLY")
    print("=" * 60)
    print(f"\nAll demo accounts use the password: {DEMO_PASSWORD}")


if __name__ == "__main__":
    main()
