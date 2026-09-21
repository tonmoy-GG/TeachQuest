package com.teachquest.model;

import javax.persistence.*;

@Entity
@Table(name = "quiz_attempt_answers")
public class QuizAttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "attempt_id", nullable = false) private Long attemptId;
    @Column(name = "question_id", nullable = false) private Long questionId;
    @Column(columnDefinition = "TEXT") private String answer;
    @Column(name = "awarded_score", nullable = false) private int awardedScore;
    @Column(nullable = false) private boolean reviewed;
    @Column(columnDefinition = "TEXT") private String feedback;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public int getAwardedScore() { return awardedScore; }
    public void setAwardedScore(int awardedScore) { this.awardedScore = awardedScore; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
