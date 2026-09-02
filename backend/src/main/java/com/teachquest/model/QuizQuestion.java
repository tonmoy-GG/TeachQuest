package com.teachquest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("question")
    @Column(nullable = true, columnDefinition = "TEXT")
    private String question;

    @JsonProperty("qCorrectAnswer")
    @Column(name = "q_ans", nullable = true, columnDefinition = "TEXT")
    private String qCorrectAnswer;

    @JsonProperty("qCategory")
    @Column(name = "q_cat", nullable = true, length = 100)
    private String qCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = true)
    @JsonIgnore
    private Quiz quiz;

    @JsonProperty("options")
    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizOption> options = new ArrayList<>();

    public QuizQuestion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQCorrectAnswer() {
        return qCorrectAnswer;
    }

    public void setQCorrectAnswer(String qCorrectAnswer) {
        this.qCorrectAnswer = qCorrectAnswer;
    }

    public String getQCategory() {
        return qCategory;
    }

    public void setQCategory(String qCategory) {
        this.qCategory = qCategory;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public List<QuizOption> getOptions() {
        return options;
    }

    public void setOptions(List<QuizOption> options) {
        this.options = options;
    }

    public void addOption(QuizOption option) {
        options.add(option);
        option.setQuizQuestion(this);
    }

    public void removeOption(QuizOption option) {
        options.remove(option);
        option.setQuizQuestion(null);
    }
}
