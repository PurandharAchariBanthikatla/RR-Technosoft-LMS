package com.rrtechnosoft.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * `options` holds the raw JSON array (`[{"key":"A","text":"..."}]`) as text —
 * serialized/deserialized via ObjectMapper in QuizService rather than mapped
 * to a Java list at the entity level, since the shape only matters where the
 * question is actually rendered or graded.
 */
@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String options;

    @Column(name = "correct_option", nullable = false, length = 5)
    private String correctOption;

    @Column(nullable = false)
    private Integer position;
}
