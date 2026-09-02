package com.teachquest.model;

import javax.persistence.*;

@Entity
@Table(name = "posted_jobs")
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    private String salary;

    @Column(name = "student_gender", nullable = false, length = 20)
    private String studentGender;

    @Column(name = "tutor_gender", nullable = false, length = 20)
    private String tutorGender;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tutor_id")
    private Long hiredTutorId;

    public JobPost() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getStudentGender() {
        return studentGender;
    }

    public void setStudentGender(String studentGender) {
        this.studentGender = studentGender;
    }

    public String getTutorGender() {
        return tutorGender;
    }

    public void setTutorGender(String tutorGender) {
        this.tutorGender = tutorGender;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getHiredTutorId() {
        return hiredTutorId;
    }

    public void setHiredTutorId(Long hiredTutorId) {
        this.hiredTutorId = hiredTutorId;
    }
}
