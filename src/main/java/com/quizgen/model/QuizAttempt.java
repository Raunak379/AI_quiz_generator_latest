package com.quizgen.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "attempt_date")
    private LocalDateTime attemptDate;

    @PrePersist
    protected void onCreate() {
        attemptDate = LocalDateTime.now();
    }

    public QuizAttempt() {}

    public QuizAttempt(Long id, Long quizId, String studentName, Integer score, Integer totalQuestions, LocalDateTime attemptDate) {
        this.id = id;
        this.quizId = quizId;
        this.studentName = studentName;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.attemptDate = attemptDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public LocalDateTime getAttemptDate() { return attemptDate; }
    public void setAttemptDate(LocalDateTime attemptDate) { this.attemptDate = attemptDate; }

    public double getPercentage() {
        if (totalQuestions == 0) return 0.0;
        return (score * 100.0) / totalQuestions;
    }
}
