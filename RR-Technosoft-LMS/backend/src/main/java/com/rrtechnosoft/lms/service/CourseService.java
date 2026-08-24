package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateCourseModuleRequest;
import com.rrtechnosoft.lms.dto.request.CreateCourseRequest;
import com.rrtechnosoft.lms.dto.request.CreateLessonRequest;
import com.rrtechnosoft.lms.dto.request.UpdateCourseRequest;
import com.rrtechnosoft.lms.dto.response.CourseModuleResponse;
import com.rrtechnosoft.lms.dto.response.CourseResponse;
import com.rrtechnosoft.lms.dto.response.LessonResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.CourseModule;
import com.rrtechnosoft.lms.entity.Lesson;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseModuleRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.IdCountProjection;
import com.rrtechnosoft.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final LessonRepository lessonRepository;
    private final AuditLogService auditLogService;

    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");

    // ---------------------------------------------------------------- Courses

    @Transactional(readOnly = true)
    public Page<CourseResponse> listCourses(String search, CourseStatus status, String category,
                                             boolean requesterIsStudent, Pageable pageable) {
        // Students only ever see the published catalog, regardless of what
        // status filter they pass — draft/archived courses aren't theirs to see.
        CourseStatus effectiveStatus = requesterIsStudent ? CourseStatus.PUBLISHED : status;

        Page<Course> page = courseRepository.search(blankToNull(search), effectiveStatus, blankToNull(category), pageable);
        if (page.isEmpty()) {
            return page.map(c -> CourseResponse.from(c, 0, 0));
        }

        List<UUID> courseIds = page.getContent().stream().map(Course::getId).toList();
        Map<UUID, Long> moduleCounts = toCountMap(courseRepository.countModulesByCourseIds(courseIds));
        Map<UUID, Long> enrollmentCounts = toCountMap(courseRepository.countEnrollmentsByCourseIds(courseIds));

        return page.map(c -> CourseResponse.from(
                c,
                moduleCounts.getOrDefault(c.getId(), 0L),
                enrollmentCounts.getOrDefault(c.getId(), 0L)
        ));
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID id, boolean requesterIsStudent) {
        Course course = findCourseOrThrow(id);
        if (requesterIsStudent && course.getStatus() != CourseStatus.PUBLISHED) {
            // Don't leak existence of unpublished courses to students as a 403 —
            // a plain 404 is the correct signal here.
            throw ApiException.notFound("Course not found");
        }
        long moduleCount = courseRepository.countModules(id);
        long studentsEnrolled = courseRepository.countEnrollments(id);
        return CourseResponse.from(course, moduleCount, studentsEnrolled);
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request, UUID actorId) {
        String slug = uniqueSlug(request.title(), null);
        Course course = Course.builder()
                .title(request.title())
                .slug(slug)
                .description(request.description())
                .category(request.category())
                .level(request.level())
                .durationWeeks(request.durationWeeks())
                .price(request.price())
                .instructorName(request.instructorName())
                .thumbnailUrl(request.thumbnailUrl())
                .status(CourseStatus.DRAFT)
                .createdBy(actorId)
                .build();
        course = courseRepository.save(course);
        auditLogService.log(actorId, "CREATE_COURSE", "Course", course.getId(), null);
        return CourseResponse.from(course, 0, 0);
    }

    @Transactional
    public CourseResponse updateCourse(UUID id, UpdateCourseRequest request, UUID actorId) {
        Course course = findCourseOrThrow(id);
        if (!course.getTitle().equals(request.title())) {
            course.setSlug(uniqueSlug(request.title(), id));
        }
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(request.category());
        course.setLevel(request.level());
        course.setDurationWeeks(request.durationWeeks());
        course.setPrice(request.price());
        course.setInstructorName(request.instructorName());
        course.setThumbnailUrl(request.thumbnailUrl());
        course = courseRepository.save(course);
        auditLogService.log(actorId, "UPDATE_COURSE", "Course", id, null);

        long moduleCount = courseRepository.countModules(id);
        long studentsEnrolled = courseRepository.countEnrollments(id);
        return CourseResponse.from(course, moduleCount, studentsEnrolled);
    }

    @Transactional
    public CourseResponse setCourseStatus(UUID id, CourseStatus status, UUID actorId) {
        Course course = findCourseOrThrow(id);
        course.setStatus(status);
        course = courseRepository.save(course);
        auditLogService.log(actorId, "SET_COURSE_STATUS_" + status, "Course", id, null);
        long moduleCount = courseRepository.countModules(id);
        long studentsEnrolled = courseRepository.countEnrollments(id);
        return CourseResponse.from(course, moduleCount, studentsEnrolled);
    }

    @Transactional
    public void deleteCourse(UUID id, UUID actorId) {
        Course course = findCourseOrThrow(id);
        courseRepository.delete(course); // cascades to modules/lessons at the DB level (ON DELETE CASCADE)
        auditLogService.log(actorId, "DELETE_COURSE", "Course", id, null);
    }

    // ---------------------------------------------------------------- Modules

    @Transactional(readOnly = true)
    public List<CourseModuleResponse> listModules(UUID courseId, boolean requesterIsStudent) {
        Course course = findCourseOrThrow(courseId);
        if (requesterIsStudent && course.getStatus() != CourseStatus.PUBLISHED) {
            throw ApiException.notFound("Course not found");
        }
        List<CourseModule> modules = courseModuleRepository.findByCourseIdOrderByPositionAsc(courseId);
        if (modules.isEmpty()) return List.of();

        List<UUID> moduleIds = modules.stream().map(CourseModule::getId).toList();
        Map<UUID, Long> lessonCounts = toCountMap(courseModuleRepository.countLessonsByModuleIds(moduleIds));

        return modules.stream()
                .map(m -> CourseModuleResponse.from(m, lessonCounts.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CourseModuleResponse createModule(UUID courseId, CreateCourseModuleRequest request, UUID actorId) {
        Course course = findCourseOrThrow(courseId);
        int nextPosition = courseModuleRepository.findMaxPosition(courseId) + 1;
        CourseModule module = CourseModule.builder()
                .course(course)
                .title(request.title())
                .position(nextPosition)
                .build();
        module = courseModuleRepository.save(module);
        auditLogService.log(actorId, "CREATE_COURSE_MODULE", "CourseModule", module.getId(), null);
        return CourseModuleResponse.from(module, 0);
    }

    // ---------------------------------------------------------------- Lessons

    @Transactional(readOnly = true)
    public List<LessonResponse> listLessons(UUID moduleId, boolean requesterIsStudent) {
        CourseModule module = findModuleOrThrow(moduleId);
        if (requesterIsStudent && module.getCourse().getStatus() != CourseStatus.PUBLISHED) {
            throw ApiException.notFound("Module not found");
        }
        return lessonRepository.findByModuleIdOrderByPositionAsc(moduleId).stream()
                .map(LessonResponse::from)
                .toList();
    }

    @Transactional
    public LessonResponse createLesson(UUID moduleId, CreateLessonRequest request, UUID actorId) {
        CourseModule module = findModuleOrThrow(moduleId);
        int nextPosition = lessonRepository.findMaxPosition(moduleId) + 1;

        var contentType = LessonResponse.toContentType(request.type());
        Lesson.LessonBuilder builder = Lesson.builder()
                .module(module)
                .title(request.title())
                .contentType(contentType)
                .durationMinutes(request.durationMinutes())
                .position(nextPosition)
                .createdBy(actorId);

        switch (contentType) {
            case VIDEO -> builder.videoUrl(request.contentUrl());
            case PDF -> builder.pdfUrl(request.contentUrl());
            case ARTICLE -> builder.body(request.contentUrl());
            default -> { /* not reachable from this endpoint */ }
        }

        Lesson lesson = lessonRepository.save(builder.build());
        auditLogService.log(actorId, "CREATE_LESSON", "Lesson", lesson.getId(), null);
        return LessonResponse.from(lesson);
    }

    // ---------------------------------------------------------------- Helpers

    private Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id).orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    private CourseModule findModuleOrThrow(UUID id) {
        return courseModuleRepository.findById(id).orElseThrow(() -> ApiException.notFound("Module not found"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Map<UUID, Long> toCountMap(List<IdCountProjection> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (IdCountProjection row : rows) {
            map.put(row.getId(), row.getCnt());
        }
        return map;
    }

    private String uniqueSlug(String title, UUID excludeCourseId) {
        String base = NON_SLUG_CHARS.matcher(
                Normalizer.normalize(title, Normalizer.Form.NFKD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase()
        ).replaceAll("-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "course";

        String candidate = base;
        int suffix = 2;
        while (excludeCourseId == null
                ? courseRepository.existsBySlug(candidate)
                : courseRepository.existsBySlugAndIdNot(candidate, excludeCourseId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
