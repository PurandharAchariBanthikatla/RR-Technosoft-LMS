package com.rrtechnosoft.lms.config;

import com.rrtechnosoft.lms.entity.*;
import com.rrtechnosoft.lms.entity.enums.*;
import com.rrtechnosoft.lms.repository.*;
import com.rrtechnosoft.lms.service.ChatbotService;
import com.rrtechnosoft.lms.service.StudentIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Populates realistic-looking demo data — an admin, a handful of students,
 * courses with modules/lessons, enrollments, daily tasks, practice problems,
 * fee records and a payment, a couple of notifications, and one sample
 * chatbot conversation — so a freshly `docker/run.sh`'d stack (or a fresh
 * `mvn spring-boot:run` against an empty DB) is immediately browsable and
 * every screen has something real to show, instead of being blank until
 * an admin manually clicks through the whole app once.
 *
 * Separate from DataSeeder (which only ever creates the one SUPER_ADMIN
 * bootstrap account and is meant to run in every environment including
 * production) — this one is opt-in via app.seed.demo-data, defaults to
 * false, and is meant for local dev, CI/E2E runs, and demos only. Runs
 * after DataSeeder (@Order 2 vs 1) so the Super Admin row always exists
 * first, and is fully idempotent: it no-ops if any course already exists,
 * so restarting the app never creates duplicates.
 *
 * All the login credentials below are printed to the log on startup —
 * nothing here is meant to be secret; it exists specifically so a fresh
 * environment can be logged into immediately.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Demo@12345";

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final DailyTaskCompletionRepository dailyTaskCompletionRepository;
    private final PracticeProblemRepository practiceProblemRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final NotificationRepository notificationRepository;
    private final ChatbotService chatbotService;
    private final StudentIdGenerator studentIdGenerator;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-data:false}")
    private boolean demoDataEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoDataEnabled) return;
        if (courseRepository.count() > 0) {
            log.info("Demo data already present — skipping DemoDataSeeder.");
            return;
        }

        log.warn("Seeding demo data (app.seed.demo-data=true) — do not enable this in production.");

        User admin = seedAdmin();
        List<User> students = seedStudents();
        List<Course> courses = seedCourses(admin.getId());
        seedModulesAndLessons(courses, admin.getId());
        List<Enrollment> enrollments = seedEnrollments(students, courses);
        seedDailyTasks(courses, students, admin.getId());
        seedPracticeProblems(students, admin.getId());
        seedFinance(students, courses, admin.getId());
        seedNotifications(students);
        seedChatbotConversation(students.get(0));

        log.warn("Demo data seeded: 1 admin + {} students, {} courses, {} enrollments. "
                        + "Admin login: admin.demo@rrtechnosoft.com / {}  |  Student logins: {} / {} (same password for all demo students)",
                students.size(), courses.size(), enrollments.size(), DEMO_PASSWORD,
                students.get(0).getStudentId(), DEMO_PASSWORD);
    }

    // --- users -------------------------------------------------------------

    private User seedAdmin() {
        User admin = userRepository.save(User.builder()
                .role(UserRole.ADMIN)
                .email("admin.demo@rrtechnosoft.com")
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .fullName("Priya Sharma")
                .phone("+91-9800000001")
                .status(AccountStatus.ACTIVE)
                .build());
        adminProfileRepository.save(AdminProfile.builder()
                .userId(admin.getId())
                .user(admin)
                .department("Academics")
                .designation("Program Coordinator")
                .build());
        return admin;
    }

    private List<User> seedStudents() {
        String[][] roster = {
                // fullName, batch, branch, college, graduationYear
                {"Asha Rao", "2026-A", "Computer Science", "Rajeev Gandhi Memorial College of Engineering", "2026"},
                {"Vikram Nair", "2026-A", "Information Technology", "JNTU Anantapur", "2026"},
                {"Sneha Iyer", "2026-B", "Electronics & Communication", "Andhra University", "2026"},
                {"Rahul Verma", "2026-B", "Computer Science", "Osmania University", "2027"},
                {"Divya Patel", "2025-C", "Computer Science", "VNR Vignana Jyothi Institute", "2025"},
                {"Karthik Reddy", "2025-C", "Information Technology", "CBIT Hyderabad", "2025"},
        };

        List<User> students = new ArrayList<>();
        for (String[] row : roster) {
            String studentId = studentIdGenerator.next();
            User student = userRepository.save(User.builder()
                    .role(UserRole.STUDENT)
                    .studentId(studentId)
                    .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                    .fullName(row[0])
                    .phone("+91-98000000" + String.format("%02d", students.size() + 10))
                    .status(AccountStatus.ACTIVE)
                    .build());
            studentProfileRepository.save(StudentProfile.builder()
                    .userId(student.getId())
                    .user(student)
                    .batch(row[1])
                    .branch(row[2])
                    .college(row[3])
                    .graduationYear(Integer.valueOf(row[4]))
                    .githubUrl("https://github.com/" + row[0].toLowerCase().replace(" ", "-"))
                    .build());
            students.add(student);
        }
        return students;
    }

    // --- courses / modules / lessons --------------------------------------

    private List<Course> seedCourses(UUID adminId) {
        record Def(String title, String slug, String category, CourseLevel level, int weeks, String instructor,
                   BigDecimal price, String description) {}
        List<Def> defs = List.of(
                new Def("AWS Cloud & DevOps Bootcamp", "aws-cloud-devops-bootcamp", "Cloud & DevOps",
                        CourseLevel.INTERMEDIATE, 12, "Anil Kumar", new BigDecimal("45000.00"),
                        "Hands-on AWS, Docker, Kubernetes, Terraform, and CI/CD with Jenkins — from EC2 basics to production deployments."),
                new Def("Full Stack Java with Spring Boot", "full-stack-java-spring-boot", "Full Stack Development",
                        CourseLevel.BEGINNER, 16, "Meera Krishnan", new BigDecimal("38000.00"),
                        "Java, Spring Boot, REST APIs, PostgreSQL, and a React frontend — build and ship a complete web application."),
                new Def("Data Analytics with Python & SQL", "data-analytics-python-sql", "Data Analytics",
                        CourseLevel.BEGINNER, 10, "Suresh Babu", new BigDecimal("28000.00"),
                        "Python, Pandas, SQL, and Power BI/Tableau for real-world data analysis and dashboarding.")
        );

        List<Course> courses = new ArrayList<>();
        for (Def d : defs) {
            courses.add(courseRepository.save(Course.builder()
                    .title(d.title())
                    .slug(d.slug())
                    .category(d.category())
                    .description(d.description())
                    .level(d.level())
                    .status(CourseStatus.PUBLISHED)
                    .durationWeeks(d.weeks())
                    .instructorName(d.instructor())
                    .price(d.price())
                    .createdBy(adminId)
                    .build()));
        }
        return courses;
    }

    private void seedModulesAndLessons(List<Course> courses, UUID adminId) {
        String[][] moduleTitles = {
                {"Getting Started", "Core Concepts"},
                {"Getting Started", "Core Concepts"},
                {"Getting Started", "Core Concepts"},
        };
        for (int c = 0; c < courses.size(); c++) {
            Course course = courses.get(c);
            for (int m = 0; m < moduleTitles[c].length; m++) {
                CourseModule module = courseModuleRepository.save(CourseModule.builder()
                        .course(course)
                        .title(moduleTitles[c][m])
                        .position(m)
                        .build());
                for (int l = 0; l < 2; l++) {
                    lessonRepository.save(Lesson.builder()
                            .module(module)
                            .title(moduleTitles[c][m] + " — Lesson " + (l + 1))
                            .contentType(l == 0 ? ContentType.VIDEO : ContentType.ARTICLE)
                            .videoUrl(l == 0 ? "https://example.com/videos/" + course.getSlug() + "-m" + m + "l" + l : null)
                            .body(l == 1 ? "Reading material and notes for this lesson." : null)
                            .durationMinutes(20 + l * 10)
                            .position(l)
                            .freePreview(m == 0 && l == 0)
                            .createdBy(adminId)
                            .build());
                }
            }
        }
    }

    // --- enrollments ---------------------------------------------------------

    private List<Enrollment> seedEnrollments(List<User> students, List<Course> courses) {
        List<Enrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Course course = courses.get(i % courses.size());
            BigDecimal progress = new BigDecimal(List.of("15.00", "40.00", "65.00", "90.00").get(i % 4));
            enrollments.add(enrollmentRepository.save(Enrollment.builder()
                    .student(students.get(i))
                    .course(course)
                    .status(EnrollmentStatus.ACTIVE)
                    .progressPct(progress)
                    .build()));
            // A couple of students enrolled in a second course too, so the
            // "My Courses" list isn't a 1:1 mapping for every student.
            if (i % 3 == 0) {
                Course secondCourse = courses.get((i + 1) % courses.size());
                enrollments.add(enrollmentRepository.save(Enrollment.builder()
                        .student(students.get(i))
                        .course(secondCourse)
                        .status(EnrollmentStatus.ACTIVE)
                        .progressPct(new BigDecimal("5.00"))
                        .build()));
            }
        }
        return enrollments;
    }

    // --- daily tasks -----------------------------------------------------------

    private void seedDailyTasks(List<Course> courses, List<User> students, UUID adminId) {
        String[][] taskDefs = {
                {"Set up your AWS Free Tier account", "Create and verify an AWS account, enable MFA on the root user."},
                {"Write a Dockerfile for a sample app", "Containerize any small app you have locally and get `docker run` working."},
                {"Practice 3 SQL JOIN queries", "Use the sample schema from today's class and write INNER/LEFT/self joins."},
                {"Push today's code to GitHub", "Commit your in-class exercise with a clear commit message."},
                {"Review yesterday's lecture notes", "Spend 15 minutes summarizing yesterday's session in your own words."},
        };
        List<DailyTask> tasks = new ArrayList<>();
        for (int i = 0; i < taskDefs.length; i++) {
            tasks.add(dailyTaskRepository.save(DailyTask.builder()
                    .course(courses.get(i % courses.size()))
                    .title(taskDefs[i][0])
                    .description(taskDefs[i][1])
                    .taskDate(LocalDate.now().minusDays(taskDefs.length - 1 - i))
                    .createdBy(adminId)
                    .build()));
        }
        // Mark the older tasks done for a subset of students, leaving today's task open —
        // gives the daily-tasks screen a realistic mix of done/pending state.
        for (int s = 0; s < students.size(); s++) {
            for (int t = 0; t < tasks.size() - 1; t++) {
                if ((s + t) % 2 == 0) {
                    dailyTaskCompletionRepository.save(DailyTaskCompletion.builder()
                            .task(tasks.get(t))
                            .student(students.get(s))
                            .done(true)
                            .doneAt(OffsetDateTime.now().minusDays(tasks.size() - 1 - t))
                            .build());
                }
            }
        }
    }

    // --- practice portal ------------------------------------------------------

    private void seedPracticeProblems(List<User> students, UUID adminId) {
        record Def(String title, PracticeTrack track, DifficultyLevel difficulty, String statement, int points) {}
        List<Def> defs = List.of(
                new Def("Two Sum", PracticeTrack.DSA, DifficultyLevel.BEGINNER,
                        "Given an array of integers and a target, return indices of the two numbers that add up to the target.", 10),
                new Def("Reverse a Linked List", PracticeTrack.DSA, DifficultyLevel.INTERMEDIATE,
                        "Reverse a singly linked list iteratively and recursively.", 20),
                new Def("Nth Highest Salary", PracticeTrack.SQL, DifficultyLevel.INTERMEDIATE,
                        "Write a query to find the Nth highest salary from an Employee table.", 15),
                new Def("List vs Tuple", PracticeTrack.PYTHON, DifficultyLevel.BEGINNER,
                        "Write a function that demonstrates the mutability difference between a Python list and a tuple.", 5),
                new Def("Write a Terraform S3 bucket module", PracticeTrack.DEVOPS, DifficultyLevel.INTERMEDIATE,
                        "Write a reusable Terraform module that provisions a versioned, encrypted S3 bucket.", 20),
                new Def("EC2 Auto Scaling basics", PracticeTrack.AWS, DifficultyLevel.BEGINNER,
                        "Explain and configure a basic EC2 Auto Scaling Group with a target-tracking policy.", 15)
        );

        List<PracticeProblem> problems = new ArrayList<>();
        for (Def d : defs) {
            problems.add(practiceProblemRepository.save(PracticeProblem.builder()
                    .title(d.title())
                    .track(d.track())
                    .difficulty(d.difficulty())
                    .statement(d.statement())
                    .points(d.points())
                    .createdBy(adminId)
                    .build()));
        }

        // A couple of students have already submitted attempts on the first two problems.
        for (int i = 0; i < Math.min(3, students.size()); i++) {
            practiceSubmissionRepository.save(PracticeSubmission.builder()
                    .problem(problems.get(0))
                    .student(students.get(i))
                    .code("def two_sum(nums, target):\n    seen = {}\n    for i, n in enumerate(nums):\n        if target - n in seen:\n            return [seen[target - n], i]\n        seen[n] = i\n")
                    .language("python")
                    .build());
        }
    }

    // --- finance ---------------------------------------------------------------

    private void seedFinance(List<User> students, List<Course> courses, UUID adminId) {
        // One fee structure per course, matching the course price.
        List<FeeStructure> feeStructures = new ArrayList<>();
        for (Course course : courses) {
            feeStructures.add(feeStructureRepository.save(FeeStructure.builder()
                    .course(course)
                    .name(course.getTitle() + " — Standard Fee")
                    .description("Full program fee, payable in up to 2 installments.")
                    .totalAmount(course.getPrice())
                    .installmentCount(2)
                    .createdBy(adminId)
                    .build()));
        }

        // Assign a StudentFee per student for their first enrollment's course,
        // with a spread of statuses so the Finance dashboard has real variety.
        FeeStatus[] statuses = {FeeStatus.PAID, FeeStatus.PARTIAL, FeeStatus.PENDING, FeeStatus.OVERDUE};
        for (int i = 0; i < students.size(); i++) {
            Course course = courses.get(i % courses.size());
            FeeStructure structure = feeStructures.get(i % feeStructures.size());
            BigDecimal total = course.getPrice();
            FeeStatus status = statuses[i % statuses.length];
            BigDecimal amountPaid = switch (status) {
                case PAID -> total;
                case PARTIAL -> total.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
                default -> BigDecimal.ZERO;
            };

            StudentFee fee = studentFeeRepository.save(StudentFee.builder()
                    .student(students.get(i))
                    .course(course)
                    .feeStructure(structure)
                    .totalAmount(total)
                    .netPayable(total)
                    .amountPaid(amountPaid)
                    .status(status)
                    .assignedBy(adminId)
                    .build());

            if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                Payment payment = paymentRepository.save(Payment.builder()
                        .studentFee(fee)
                        .student(students.get(i))
                        .amount(amountPaid)
                        .method(PaymentMethod.UPI)
                        .gatewayProvider(PaymentGatewayProvider.MANUAL)
                        .status(PaymentStatus.SUCCESS)
                        .recordedBy(adminId)
                        .paidAt(OffsetDateTime.now().minusDays(3))
                        .build());
                receiptRepository.save(Receipt.builder()
                        .payment(payment)
                        .studentFee(fee)
                        .receiptNumber("RRT-RCPT-DEMO-" + String.format("%04d", i + 1))
                        .amount(amountPaid)
                        .generatedBy(adminId)
                        .build());
            }
        }
    }

    // --- notifications -----------------------------------------------------

    private void seedNotifications(List<User> students) {
        for (User student : students) {
            notificationRepository.save(Notification.builder()
                    .user(student)
                    .type(NotificationType.ANNOUNCEMENT)
                    .title("Welcome to RR Technosoft LMS")
                    .body("Your account is set up. Check your Daily Tasks and enrolled courses to get started.")
                    .build());
        }
        notificationRepository.save(Notification.builder()
                .user(students.get(0))
                .type(NotificationType.TASK)
                .title("New daily task posted")
                .body("A new daily task has been added for your course.")
                .link("/student/daily-tasks")
                .build());
    }

    // --- chatbot ------------------------------------------------------------

    private void seedChatbotConversation(User student) {
        // Goes through ChatbotService (not direct repository saves) specifically
        // so it exercises the real service path — SimulatedLlmClient generates
        // an actual topic-aware reply, the same as if the student had typed it.
        chatbotService.sendMessage(
                new com.rrtechnosoft.lms.dto.request.SendChatMessageRequest(
                        null, "Can you explain how Docker works?"),
                student.getId());
    }
}
