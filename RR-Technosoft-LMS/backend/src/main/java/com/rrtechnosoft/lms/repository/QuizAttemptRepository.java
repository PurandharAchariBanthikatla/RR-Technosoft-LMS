package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findByQuizIdAndStudentId(UUID quizId, UUID studentId);

    @Query("select a from QuizAttempt a where a.quiz.id in :quizIds and a.student.id = :studentId")
    List<QuizAttempt> findByQuizIdsAndStudentId(@Param("quizIds") List<UUID> quizIds, @Param("studentId") UUID studentId);
}
