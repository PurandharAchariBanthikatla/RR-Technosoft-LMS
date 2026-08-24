package com.rrtechnosoft.lms.dto.shared;

/**
 * One option in a quiz question, mirroring the `{"key":"A","text":"..."}`
 * shape stored in quiz_questions.options (jsonb). `key` is what
 * quiz_questions.correct_option references; array position = the index
 * the frontend's QuizQuestion.options / SubmitQuizRequest optionIndex use.
 */
public record QuestionOption(String key, String text) {}
