package com.quizgen.service;

import com.quizgen.model.Question;
import com.quizgen.model.Quiz;
import com.quizgen.repository.QuestionRepository;
import com.quizgen.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(QuizGenerationService.class);

    @Autowired
    private AIQuizGenerationService aiQuizGenerationService;

    @Autowired
    private RuleBasedQuizGenerationService ruleBasedQuizGenerationService;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Value("${app.quiz.default.questions:10}")
    private int defaultQuestionCount;

    @Value("${app.quiz.fallback.enabled:true}")
    private boolean fallbackEnabled;

    @Transactional
    public Quiz generateQuiz(String title, String text, Long studyMaterialId) {
        return generateQuiz(title, text, studyMaterialId, defaultQuestionCount);
    }

    @Transactional
    public Quiz generateQuiz(String title, String text, Long studyMaterialId, int questionCount) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting quiz generation for: {}", title);

        int sanitizedQuestionCount = Math.max(1, Math.min(questionCount, 20));

        List<Question> questions;
        Quiz.GenerationMethod method;

        try {
            logger.info("Attempting AI-based quiz generation...");
            questions = aiQuizGenerationService.generateQuestions(text, sanitizedQuestionCount);
            method = Quiz.GenerationMethod.AI;
        } catch (Exception e) {
            logger.warn("AI generation failed: {}. Falling back to rule-based generation.", e.getMessage());
            if (!fallbackEnabled) {
                throw new RuntimeException("AI generation failed and fallback is disabled", e);
            }
            questions = ruleBasedQuizGenerationService.generateQuestions(text, sanitizedQuestionCount);
            method = Quiz.GenerationMethod.RULE_BASED;
        }

        List<Question> questionsToPersist = new java.util.ArrayList<>(questions.size());
        for (Question question : questions) {
            questionsToPersist.add(copyQuestion(question));
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setStudyMaterialId(studyMaterialId);
        quiz.setGenerationMethod(method);
        quiz.setActive(true);
        quiz = quizRepository.save(quiz);

        final Long quizId = quiz.getId();
        for (Question question : questionsToPersist) {
            question.setQuizId(quizId);
        }
        questionRepository.saveAll(questionsToPersist);

        logger.info("Quiz created with ID: {} using method: {} in {} ms",
                quiz.getId(), method, (System.currentTimeMillis() - startTime));
        return quiz;
    }

    /**
     * Re-generates questions for an existing quiz (keeps same quiz id, title, material).
     * Caller is responsible for deleting old questions before calling this.
     */
    @Transactional
    public Quiz regenerateQuestions(Quiz quiz, String text, int questionCount) {
        long startTime = System.currentTimeMillis();
        int sanitizedCount = Math.max(1, Math.min(questionCount, 20));

        List<Question> questions;
        Quiz.GenerationMethod method;

        try {
            questions = aiQuizGenerationService.generateQuestions(text, sanitizedCount);
            method = Quiz.GenerationMethod.AI;
        } catch (Exception e) {
            logger.warn("AI re-generation failed: {}. Using rule-based fallback.", e.getMessage());
            if (!fallbackEnabled) {
                throw new RuntimeException("AI generation failed and fallback is disabled", e);
            }
            questions = ruleBasedQuizGenerationService.generateQuestions(text, sanitizedCount);
            method = Quiz.GenerationMethod.RULE_BASED;
        }

        quiz.setGenerationMethod(method);
        quizRepository.save(quiz);

        List<Question> questionsToPersist = new java.util.ArrayList<>(questions.size());
        for (Question q : questions) {
            Question copy = copyQuestion(q);
            copy.setQuizId(quiz.getId());
            questionsToPersist.add(copy);
        }
        questionRepository.saveAll(questionsToPersist);

        logger.info("Quiz {} re-generated using {} in {} ms",
                quiz.getId(), method, (System.currentTimeMillis() - startTime));
        return quiz;
    }

    private Question copyQuestion(Question source) {
        Question target = new Question();
        target.setQuestionText(source.getQuestionText());
        target.setOption1(source.getOption1());
        target.setOption2(source.getOption2());
        target.setOption3(source.getOption3());
        target.setOption4(source.getOption4());
        target.setCorrectAnswer(source.getCorrectAnswer());
        return target;
    }

    public Quiz getQuizWithQuestions(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + quizId));
    }

    public List<Question> getQuestionsByQuizId(Long quizId) {
        return questionRepository.findByQuizId(quizId);
    }

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }
}
