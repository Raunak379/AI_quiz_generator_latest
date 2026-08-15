package com.quizgen.dto;

import com.quizgen.model.Question;
import com.quizgen.model.Quiz;
import java.util.List;

public class QuizResponse {
    private Quiz quiz;
    private List<Question> questions;

    public QuizResponse() {}

    public QuizResponse(Quiz quiz, List<Question> questions) {
        this.quiz = quiz;
        this.questions = questions;
    }

    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
