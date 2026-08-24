package com.rrtechnosoft.lms.repository;

import com.rrtechnosoft.lms.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findByQuizIdOrderByPositionAsc(UUID quizId);

    long countByQuizId(UUID quizId);

    @Query("select q.quiz.id as id, count(q) as cnt from QuizQuestion q where q.quiz.id in :quizIds group by q.quiz.id")
    List<IdCountProjection> countByQuizIds(@Param("quizIds") List<UUID> quizIds);
}
