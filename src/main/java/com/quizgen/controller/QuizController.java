package com.quizgen.controller;

import com.quizgen.dto.QuizResponse;
import com.quizgen.model.Question;
import com.quizgen.model.Quiz;
import com.quizgen.model.StudyMaterial;
import com.quizgen.repository.QuizRepository;
import com.quizgen.repository.QuestionRepository;
import com.quizgen.repository.StudyMaterialRepository;
import com.quizgen.service.QuizGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private StudyMaterialRepository studyMaterialRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    @Qualifier("quizTaskExecutor")
    private Executor quizTaskExecutor;

    // ── Generate quiz ──────────────────────────────────────────────────────────
    @PostMapping("/generate/{materialId}")
    public CompletableFuture<ResponseEntity<?>> generateQuiz(
            @PathVariable("materialId") Long materialId,
            @RequestParam(name = "questionCount", required = false, defaultValue = "10") int questionCount,
            @RequestParam(name = "quizTitle", required = false) String quizTitle) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Generating quiz from material ID: {}", materialId);

                StudyMaterial material = studyMaterialRepository.findById(materialId)
                        .orElseThrow(() -> new RuntimeException("Study material not found"));

                String text = material.getExtractedText();
                if (text == null || text.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("No text content found in study material"));
                }

                int wordCount = countWords(text);
                if (wordCount > 3000) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse(
                                    "The document is too large (over 3000 words). Please extract or chunk the PDF into smaller sections and upload again."));
                }

                String title = (quizTitle != null && !quizTitle.trim().isEmpty())
                        ? quizTitle.trim()
                        : buildDefaultQuizTitle(material.getFileName());

                if (title.length() > 100) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Quiz title must be 100 characters or fewer"));
                }

                Quiz quiz = quizGenerationService.generateQuiz(title, text, materialId, questionCount);
                List<Question> questions = quizGenerationService.getQuestionsByQuizId(quiz.getId());

                QuizResponse response = new QuizResponse(quiz, questions);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error generating quiz: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to generate quiz: " + e.getMessage()));
            }
        }, quizTaskExecutor);
    }

    // ── Re-generate quiz (replaces questions for existing quiz) ───────────────
    @PostMapping("/{id}/regenerate")
    public CompletableFuture<ResponseEntity<?>> regenerateQuiz(
            @PathVariable("id") Long quizId,
            @RequestParam(name = "questionCount", required = false, defaultValue = "10") int questionCount) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Re-generating quiz ID: {}", quizId);

                Quiz quiz = quizRepository.findById(quizId)
                        .orElseThrow(() -> new RuntimeException("Quiz not found"));

                Long materialId = quiz.getStudyMaterialId();
                if (materialId == null) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Quiz has no linked study material"));
                }

                StudyMaterial material = studyMaterialRepository.findById(materialId)
                        .orElseThrow(() -> new RuntimeException("Study material no longer exists"));

                String text = material.getExtractedText();
                if (text == null || text.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("No text content in study material"));
                }

                // Delete old questions
                List<Question> oldQuestions = questionRepository.findByQuizId(quizId);
                questionRepository.deleteAll(oldQuestions);

                // Re-generate using same service
                Quiz updatedQuiz = quizGenerationService.regenerateQuestions(quiz, text, questionCount);
                List<Question> newQuestions = quizGenerationService.getQuestionsByQuizId(quizId);

                QuizResponse response = new QuizResponse(updatedQuiz, newQuestions);
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                logger.error("Error re-generating quiz: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to re-generate quiz: " + e.getMessage()));
            }
        }, quizTaskExecutor);
    }

    // ── Toggle active/inactive ─────────────────────────────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable("id") Long quizId) {
        try {
            Optional<Quiz> opt = quizRepository.findById(quizId);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Quiz quiz = opt.get();
            quiz.setActive(!quiz.isActive());
            quizRepository.save(quiz);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", quiz.getId());
            resp.put("active", quiz.isActive());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error toggling quiz status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update status"));
        }
    }

    // ── Delete quiz ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable("id") Long quizId) {
        try {
            if (!quizRepository.existsById(quizId)) {
                return ResponseEntity.notFound().build();
            }
            // Delete questions first
            List<Question> questions = questionRepository.findByQuizId(quizId);
            questionRepository.deleteAll(questions);
            quizRepository.deleteById(quizId);

            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Quiz deleted successfully");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("Error deleting quiz: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete quiz"));
        }
    }

    // ── Get single quiz ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuiz(@PathVariable("id") Long id) {
        try {
            Quiz quiz = quizGenerationService.getQuizWithQuestions(id);
            List<Question> questions = quizGenerationService.getQuestionsByQuizId(id);
            QuizResponse response = new QuizResponse(quiz, questions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching quiz: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ── Get all quizzes (teacher — all), filtered by ?activeOnly=true for students
    @GetMapping
    public ResponseEntity<List<Quiz>> getAllQuizzes(
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        List<Quiz> quizzes = quizGenerationService.getAllQuizzes();
        if (activeOnly) {
            quizzes = quizzes.stream().filter(Quiz::isActive).collect(Collectors.toList());
        }
        return ResponseEntity.ok(quizzes);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private String buildDefaultQuizTitle(String fileName) {
        if (fileName == null || fileName.trim().isEmpty())
            return "Untitled Quiz";
        String name = fileName.trim();
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex > 0)
            name = name.substring(0, extensionIndex);
        name = name.replace('_', ' ').trim();
        return name.isEmpty() ? "Untitled Quiz" : name;
    }

    private int countWords(String text) {
        boolean insideWord = false;
        int words = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                insideWord = false;
            } else if (!insideWord) {
                words++;
                insideWord = true;
            }
        }
        return words;
    }
}
