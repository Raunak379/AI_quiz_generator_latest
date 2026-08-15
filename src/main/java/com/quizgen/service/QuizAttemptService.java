package com.quizgen.service;

import com.quizgen.model.Question;
import com.quizgen.model.QuizAttempt;
import com.quizgen.repository.QuestionRepository;
import com.quizgen.repository.QuizAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class QuizAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(QuizAttemptService.class);

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Transactional
    public QuizAttempt submitQuizAttempt(Long quizId, String studentName, Map<Long, Integer> answers) {
        logger.info("Processing quiz attempt for student: {} on quiz: {}", studentName, quizId);

        List<Question> questions = questionRepository.findByQuizId(quizId);

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found for quiz: " + quizId);
        }

        int correctAnswers = 0;
        for (Question question : questions) {
            Integer studentAnswer = answers.get(question.getId());
            if (studentAnswer != null && studentAnswer.equals(question.getCorrectAnswer())) {
                correctAnswers++;
            }
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setStudentName(studentName);
        attempt.setScore(correctAnswers);
        attempt.setTotalQuestions(questions.size());

        attempt = quizAttemptRepository.save(attempt);

        logger.info("Quiz attempt saved. Score: {}/{} ({}%)",
                correctAnswers, questions.size(), attempt.getPercentage());

        return attempt;
    }

    public List<QuizAttempt> getAttemptsByQuiz(Long quizId) {
        return quizAttemptRepository.findByQuizId(quizId);
    }

    public List<QuizAttempt> getAttemptsByStudent(String studentName) {
        return quizAttemptRepository.findByStudentName(studentName);
    }

    /** Returns every attempt across all quizzes, sorted newest first. */
    public List<QuizAttempt> getAllAttempts() {
        return quizAttemptRepository.findAllByOrderByAttemptDateDesc();
    }
}
