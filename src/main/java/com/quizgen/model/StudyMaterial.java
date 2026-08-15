package com.quizgen.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_materials")
public class StudyMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        if (uploadedBy == null) {
            uploadedBy = "teacher";
        }
    }

    public StudyMaterial() {}

    public StudyMaterial(Long id, String fileName, String fileType, LocalDateTime uploadDate, String extractedText, String uploadedBy) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadDate = uploadDate;
        this.extractedText = extractedText;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
