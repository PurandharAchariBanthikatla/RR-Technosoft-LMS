package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateDailyTaskRequest;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.DailyTask;
import com.rrtechnosoft.lms.entity.DailyTaskCompletion;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.DailyTaskCompletionRepository;
import com.rrtechnosoft.lms.repository.DailyTaskRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyTaskServiceTest {

    @Mock private DailyTaskRepository dailyTaskRepository;
    @Mock private DailyTaskCompletionRepository completionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    private DailyTaskService dailyTaskService;

    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dailyTaskService = new DailyTaskService(dailyTaskRepository, completionRepository, courseRepository, userRepository, auditLogService);
    }

    @Test
    void list_mergesCompletionStatusForTheCallingStudent() {
        UUID doneTaskId = UUID.randomUUID();
        UUID pendingTaskId = UUID.randomUUID();
        DailyTask doneTask = DailyTask.builder().id(doneTaskId).title("Solve 2 SQL problems").taskDate(LocalDate.now()).build();
        DailyTask pendingTask = DailyTask.builder().id(pendingTaskId).title("Watch Module 3 video").taskDate(LocalDate.now()).build();
        when(dailyTaskRepository.search(null)).thenReturn(List.of(doneTask, pendingTask));

        DailyTaskCompletion completion = DailyTaskCompletion.builder().task(doneTask).done(true).build();
        when(completionRepository.findByStudentIdAndTaskIdIn(studentId, List.of(doneTaskId, pendingTaskId)))
                .thenReturn(List.of(completion));

        var result = dailyTaskService.list(null, studentId);

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(r -> r.id().equals(doneTaskId)).findFirst().orElseThrow().completed()).isTrue();
        assertThat(result.stream().filter(r -> r.id().equals(pendingTaskId)).findFirst().orElseThrow().completed()).isFalse();
    }

    @Test
    void list_returnsEmptyListWithoutQueryingCompletionsWhenNoTasksExist() {
        when(dailyTaskRepository.search(any())).thenReturn(List.of());

        var result = dailyTaskService.list(LocalDate.now(), studentId);

        assertThat(result).isEmpty();
        verifyNoInteractions(completionRepository);
    }

    @Test
    void create_persistsTaskLinkedToCourseWhenCourseIdProvided() {
        UUID actorId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).title("AWS Fundamentals").build();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(dailyTaskRepository.save(any(DailyTask.class))).thenAnswer(inv -> {
            DailyTask t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        var request = new CreateDailyTaskRequest(courseId, "Read Chapter 4", "Notes on EC2 basics", LocalDate.now());
        var response = dailyTaskService.create(request, actorId);

        assertThat(response.title()).isEqualTo("Read Chapter 4");
        assertThat(response.courseTitle()).isEqualTo("AWS Fundamentals");
        assertThat(response.completed()).isFalse();
        verify(auditLogService).log(actorId, "CREATE_DAILY_TASK", "DailyTask", response.id(), null);
    }

    @Test
    void create_rejectsUnknownCourseId() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        var request = new CreateDailyTaskRequest(courseId, "Task", "Desc", LocalDate.now());

        assertThatThrownBy(() -> dailyTaskService.create(request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void toggle_createsANewCompletionRowWhenNoneExistsYet() {
        UUID taskId = UUID.randomUUID();
        DailyTask task = DailyTask.builder().id(taskId).title("Task").build();
        User student = User.builder().id(studentId).fullName("Asha").build();
        when(dailyTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(completionRepository.findByTaskIdAndStudentId(taskId, studentId)).thenReturn(Optional.empty());
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(completionRepository.save(any(DailyTaskCompletion.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = dailyTaskService.toggle(taskId, true, studentId);

        assertThat(response.completed()).isTrue();
        verify(completionRepository).save(argThat(c -> c.isDone() && c.getDoneAt() != null));
    }

    @Test
    void toggle_updatesExistingCompletionRowInstead() {
        UUID taskId = UUID.randomUUID();
        DailyTask task = DailyTask.builder().id(taskId).title("Task").build();
        DailyTaskCompletion existing = DailyTaskCompletion.builder().id(UUID.randomUUID()).task(task).done(true).build();
        when(dailyTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(completionRepository.findByTaskIdAndStudentId(taskId, studentId)).thenReturn(Optional.of(existing));
        when(completionRepository.save(any(DailyTaskCompletion.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = dailyTaskService.toggle(taskId, false, studentId);

        assertThat(response.completed()).isFalse();
        verifyNoInteractions(userRepository);
        verify(completionRepository).save(argThat(c -> !c.isDone() && c.getDoneAt() == null));
    }
}
