package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateCourseModuleRequest;
import com.rrtechnosoft.lms.dto.request.CreateCourseRequest;
import com.rrtechnosoft.lms.dto.request.CreateLessonRequest;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.CourseModule;
import com.rrtechnosoft.lms.entity.enums.CourseLevel;
import com.rrtechnosoft.lms.entity.enums.CourseStatus;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseModuleRepository;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private CourseService courseService;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // no-op, kept for future shared fixtures
    }

    // --------------------------------------------------------- createCourse

    @Test
    void createCourse_defaultsToDraftAndGeneratesSlug() {
        CreateCourseRequest request = new CreateCourseRequest(
                "Full Stack Web Development", "A description that is definitely long enough.",
                "Web Development", CourseLevel.BEGINNER, 8, new BigDecimal("4999"), "Jane Doe", null
        );
        when(courseRepository.existsBySlug("full-stack-web-development")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(java.time.OffsetDateTime.now());
            return c;
        });

        var response = courseService.createCourse(request, actorId);

        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.slug()).isEqualTo("full-stack-web-development");
        assertThat(response.moduleCount()).isZero();
        assertThat(response.studentsEnrolled()).isZero();
        verify(auditLogService).log(actorId, "CREATE_COURSE", "Course", response.id(), null);
    }

    @Test
    void createCourse_dedupesSlugOnCollision() {
        CreateCourseRequest request = new CreateCourseRequest(
                "AWS Basics", "A description that is definitely long enough.",
                "Cloud", CourseLevel.BEGINNER, 4, BigDecimal.ZERO, "Jane Doe", null
        );
        when(courseRepository.existsBySlug("aws-basics")).thenReturn(true);
        when(courseRepository.existsBySlug("aws-basics-2")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = courseService.createCourse(request, actorId);

        assertThat(response.slug()).isEqualTo("aws-basics-2");
    }

    // ----------------------------------------------------------- getCourse

    @Test
    void getCourse_studentCanSeePublishedCourse() {
        UUID id = UUID.randomUUID();
        Course course = publishedCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseRepository.countModules(id)).thenReturn(3L);
        when(courseRepository.countEnrollments(id)).thenReturn(10L);

        var response = courseService.getCourse(id, true);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.moduleCount()).isEqualTo(3L);
        assertThat(response.studentsEnrolled()).isEqualTo(10L);
    }

    @Test
    void getCourse_studentCannotSeeDraftCourse_returns404NotForbidden() {
        UUID id = UUID.randomUUID();
        Course course = draftCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.getCourse(id, true))
                .isInstanceOf(ApiException.class)
                .hasMessage("Course not found");
    }

    @Test
    void getCourse_adminCanSeeDraftCourse() {
        UUID id = UUID.randomUUID();
        Course course = draftCourse(id);
        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseRepository.countModules(id)).thenReturn(0L);
        when(courseRepository.countEnrollments(id)).thenReturn(0L);

        var response = courseService.getCourse(id, false);

        assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void getCourse_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(id, false))
                .isInstanceOf(ApiException.class)
                .hasMessage("Course not found");
    }

    // ------------------------------------------------------------ listing

    @Test
    void listCourses_forcesPublishedStatusForStudentsRegardlessOfRequestedFilter() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        org.springframework.data.domain.Page<Course> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());
        when(courseRepository.search(isNull(), eq(CourseStatus.PUBLISHED), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        // student explicitly asks for DRAFT courses — should be silently overridden to PUBLISHED
        courseService.listCourses(null, CourseStatus.DRAFT, null, true, pageable);

        verify(courseRepository).search(isNull(), eq(CourseStatus.PUBLISHED), isNull(), eq(pageable));
    }

    // ----------------------------------------------------------- modules

    @Test
    void createModule_assignsNextSequentialPosition() {
        UUID courseId = UUID.randomUUID();
        Course course = draftCourse(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseModuleRepository.findMaxPosition(courseId)).thenReturn(1); // two modules already, positions 0 and 1
        when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(inv -> {
            CourseModule m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        var response = courseService.createModule(courseId, new CreateCourseModuleRequest("New module"), actorId);

        assertThat(response.order()).isEqualTo(2);
        assertThat(response.lessonCount()).isZero();
    }

    // ----------------------------------------------------------- lessons

    @Test
    void createLesson_resourceTypeMapsToPdfContentType() {
        UUID moduleId = UUID.randomUUID();
        CourseModule module = CourseModule.builder().id(moduleId).course(draftCourse(UUID.randomUUID())).title("M1").position(0).build();
        when(courseModuleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(lessonRepository.findMaxPosition(moduleId)).thenReturn(-1);
        when(lessonRepository.save(any())).thenAnswer(inv -> {
            var lesson = inv.getArgument(0, com.rrtechnosoft.lms.entity.Lesson.class);
            lesson.setId(UUID.randomUUID());
            return lesson;
        });

        var response = courseService.createLesson(
                moduleId,
                new CreateLessonRequest("Cheat sheet", "RESOURCE", 5, "https://cdn.example.com/sheet.pdf"),
                actorId
        );

        assertThat(response.type()).isEqualTo("RESOURCE");
        assertThat(response.contentUrl()).isEqualTo("https://cdn.example.com/sheet.pdf");
        assertThat(response.order()).isZero();
    }

    @Test
    void createLesson_unknownModule_throwsNotFound() {
        UUID moduleId = UUID.randomUUID();
        when(courseModuleRepository.findById(moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createLesson(
                moduleId, new CreateLessonRequest("X", "VIDEO", 1, "https://x.test/v.mp4"), actorId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Module not found");
    }

    // ------------------------------------------------------------ helpers

    private Course publishedCourse(UUID id) {
        return Course.builder().id(id).title("T").slug("t").category("C").status(CourseStatus.PUBLISHED)
                .level(CourseLevel.BEGINNER).price(BigDecimal.ZERO).createdBy(actorId).build();
    }

    private Course draftCourse(UUID id) {
        return Course.builder().id(id).title("T").slug("t").category("C").status(CourseStatus.DRAFT)
                .level(CourseLevel.BEGINNER).price(BigDecimal.ZERO).createdBy(actorId).build();
    }
}
