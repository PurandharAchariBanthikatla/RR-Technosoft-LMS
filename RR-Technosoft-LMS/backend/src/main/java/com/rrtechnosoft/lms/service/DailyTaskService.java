package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateDailyTaskRequest;
import com.rrtechnosoft.lms.dto.response.DailyTaskResponse;
import com.rrtechnosoft.lms.entity.Course;
import com.rrtechnosoft.lms.entity.DailyTask;
import com.rrtechnosoft.lms.entity.DailyTaskCompletion;
import com.rrtechnosoft.lms.entity.User;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.DailyTaskCompletionRepository;
import com.rrtechnosoft.lms.repository.DailyTaskRepository;
import com.rrtechnosoft.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DailyTaskService {

    private final DailyTaskRepository dailyTaskRepository;
    private final DailyTaskCompletionRepository completionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<DailyTaskResponse> list(LocalDate date, UUID studentId) {
        List<DailyTask> tasks = dailyTaskRepository.search(date);
        if (tasks.isEmpty()) return List.of();

        List<UUID> taskIds = tasks.stream().map(DailyTask::getId).toList();
        Map<UUID, Boolean> doneByTask = new HashMap<>();
        for (DailyTaskCompletion c : completionRepository.findByStudentIdAndTaskIdIn(studentId, taskIds)) {
            doneByTask.put(c.getTask().getId(), c.isDone());
        }
        return tasks.stream()
                .map(t -> DailyTaskResponse.from(t, doneByTask.getOrDefault(t.getId(), false)))
                .toList();
    }

    @Transactional
    public DailyTaskResponse create(CreateDailyTaskRequest request, UUID actorId) {
        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                    .orElseThrow(() -> ApiException.notFound("Course not found"));
        }
        DailyTask task = DailyTask.builder()
                .course(course)
                .title(request.title())
                .description(request.description())
                .taskDate(request.date())
                .createdBy(actorId)
                .build();
        task = dailyTaskRepository.save(task);
        auditLogService.log(actorId, "CREATE_DAILY_TASK", "DailyTask", task.getId(), null);
        return DailyTaskResponse.from(task, false);
    }

    @Transactional
    public DailyTaskResponse toggle(UUID taskId, boolean completed, UUID studentId) {
        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        DailyTaskCompletion completion = completionRepository.findByTaskIdAndStudentId(taskId, studentId)
                .orElseGet(() -> {
                    User student = userRepository.findById(studentId)
                            .orElseThrow(() -> ApiException.notFound("Student not found"));
                    return DailyTaskCompletion.builder().task(task).student(student).build();
                });
        completion.setDone(completed);
        completion.setDoneAt(completed ? OffsetDateTime.now() : null);
        completionRepository.save(completion);

        auditLogService.log(studentId, "TOGGLE_DAILY_TASK", "DailyTask", taskId, null);
        return DailyTaskResponse.from(task, completed);
    }
}
