package com.teachquest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index"; // Maps to index.html
    }

    @GetMapping("/login")
    public String loginRedirect() {
        return "redirect:/"; // Redirect to selection page so they can pick a role
    }

    @GetMapping("/login/student")
    public String loginStudent() {
        return "login_student"; // maps to login_student.html
    }

    @GetMapping("/login/teacher")
    public String loginTeacher() {
        return "login_teacher"; // maps to login_teacher.html
    }

    @GetMapping("/login/jobposter")
    public String loginJobPoster() {
        return "login_jobposter"; // maps to login_jobposter.html
    }

    @GetMapping("/register")
    public String register() {
        return "registration"; // maps to registration.html
    }

    @GetMapping("/job-portal")
    public String jobPortal() {
        return "job_portal"; // maps to job_portal.html
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat"; // maps to chat.html
    }

    @GetMapping("/upload-resource")
    public String uploadResource() {
        return "upload_resource"; // maps to upload_resource.html
    }

    @GetMapping("/study-resources")
    public String studyResources() {
        return "study_resources"; // maps to study_resources.html
    }

    @GetMapping("/view_files")
    public String viewFiles() {
        return "view_files"; // maps to view_files.html
    }

    @GetMapping("/create-quiz")
    public String createQuiz() {
        return "create_quiz";
    }

    @GetMapping("/take-quiz")
    public String takeQuiz() {
        return "take_quiz";
    }

    @GetMapping("/view-applicants")
    public String viewApplicants() {
        return "view_applicants";
    }

    @GetMapping("/my-applications")
    public String myApplications() {
        return "my_applications";
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard() {
        return "student_dashboard";
    }

    @GetMapping("/teacher-dashboard")
    public String teacherDashboard() {
        return "teacher_dashboard"; // Placeholder
    }

    @GetMapping("/job-board")
    public String jobBoard() {
        return "job_board";
    }

    @GetMapping("/view-applied-jobs")
    public String viewAppliedJobs() {
        return "view_applied_jobs";
    }

    @GetMapping("/manage-applicants")
    public String manageApplicants() {
        return "manage_applicants";
    }

    // --- New Quiz Pages ---

    @GetMapping("/quiz-hub")
    public String quizHub() {
        return "quiz_hub";
    }

    @GetMapping("/quiz-start-random")
    public String quizStartRandom() {
        return "quiz_start_random";
    }

    @GetMapping("/adaptive-quiz")
    public String adaptiveQuiz() {
        return "adaptive_quiz";
    }

    @GetMapping("/section-quiz")
    public String sectionQuiz() {
        return "section_quiz";
    }

    @GetMapping("/case-study-quiz")
    public String caseStudyQuiz() {
        return "case_study_quiz";
    }

    @GetMapping("/quiz-result-ai")
    public String quizResultAi() {
        return "quiz_result_ai";
    }

    @GetMapping("/tutor-analytics")
    public String tutorAnalytics() {
        return "tutor_analytics";
    }

    // --- Teacher Manual Creation Routes ---

    @GetMapping("/question-bank")
    public String questionBank() {
        return "question_bank";
    }

    @GetMapping("/create-question-manual")
    public String createQuestionManual() {
        return "create_question_manual";
    }

    @GetMapping("/create-casestudy-manual")
    public String createCaseStudyManual() {
        return "create_casestudy_manual";
    }

    @GetMapping("/create-quiz-from-bank")
    public String createQuizFromBank() {
        return "create_quiz_from_bank";
    }
}
