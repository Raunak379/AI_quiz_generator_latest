package com.quizgen.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "study_material_id")
    private Long studyMaterialId;

    @Column(name = "generation_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private GenerationMethod generationMethod;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    public Quiz() {}

    public Quiz(Long id, String title, LocalDateTime createdDate, Long studyMaterialId, GenerationMethod generationMethod) {
        this.id = id;
        this.title = title;
        this.createdDate = createdDate;
        this.studyMaterialId = studyMaterialId;
        this.generationMethod = generationMethod;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Long getStudyMaterialId() { return studyMaterialId; }
    public void setStudyMaterialId(Long studyMaterialId) { this.studyMaterialId = studyMaterialId; }

    public GenerationMethod getGenerationMethod() { return generationMethod; }
    public void setGenerationMethod(GenerationMethod generationMethod) { this.generationMethod = generationMethod; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public enum GenerationMethod {
        AI, RULE_BASED
    }
}
