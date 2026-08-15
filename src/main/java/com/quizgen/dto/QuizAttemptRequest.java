package com.quizgen.dto;

import java.util.Map;

public class QuizAttemptRequest {
    private String studentName;
    private Map<Long, Integer> answers;

    public QuizAttemptRequest() {}

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Map<Long, Integer> getAnswers() { return answers; }
    public void setAnswers(Map<Long, Integer> answers) { this.answers = answers; }
}
