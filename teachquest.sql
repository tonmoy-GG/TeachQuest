-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 02, 2026 at 10:00 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `teachquest`
--

-- --------------------------------------------------------

--
-- Table structure for table `job_applications`
--

CREATE TABLE `job_applications` (
  `id` int(11) NOT NULL,
  `job_id` int(11) NOT NULL,
  `tutor_id` int(11) NOT NULL,
  `application_date` datetime DEFAULT current_timestamp(),
  `status` enum('pending','hired','rejected') DEFAULT 'pending',
  `applied_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `timestamp` datetime DEFAULT current_timestamp(),
  `status` enum('sent','received') DEFAULT 'sent',
  `file_path` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `posted_jobs`
--

CREATE TABLE `posted_jobs` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `days` int(11) NOT NULL,
  `requirements` text DEFAULT NULL,
  `address` text DEFAULT NULL,
  `salary` varchar(100) DEFAULT NULL,
  `student_gender` enum('male','female') NOT NULL,
  `tutor_gender` enum('male','female','any') NOT NULL,
  `posted_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `user_id` int(11) DEFAULT NULL,
  `tutor_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `posted_jobs`
--

INSERT INTO `posted_jobs` (`id`, `title`, `subject`, `days`, `requirements`, `address`, `salary`, `student_gender`, `tutor_gender`, `posted_at`, `user_id`, `tutor_id`) VALUES
(29, 'need physics tutor', 'physcis', 3, 'none', 'Mirpur 02', '4000', 'male', 'male', '2026-01-18 13:36:39', NULL, NULL),
(32, 'need physics tutor', 'physcis', 3, 'none', 'mirpur2', '4000', 'male', 'male', '2026-01-18 13:45:38', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `quizzes`
--

CREATE TABLE `quizzes` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `marks` int(11) NOT NULL,
  `student_name` varchar(255) NOT NULL,
  `student_id` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quizzes`
--

INSERT INTO `quizzes` (`id`, `title`, `marks`, `student_name`, `student_id`, `created_at`) VALUES
(7, 'sol', 10, 'Tonmoy', '011213086', '2025-01-27 04:26:36'),
(8, '', 0, '', '', '2025-01-27 08:32:45'),
(9, 'spl', 10, 'Tonmoy', '011213086', '2025-01-27 08:33:07'),
(10, 'Teacher Question Bank', 0, 'SYSTEM_BANK', 'TEACHER_BANK', '2026-01-26 06:32:59');

-- --------------------------------------------------------

--
-- Table structure for table `quiz_options`
--

CREATE TABLE `quiz_options` (
  `id` int(11) NOT NULL,
  `question_id` int(11) NOT NULL,
  `option_label` varchar(1) NOT NULL,
  `option_text` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_options`
--

INSERT INTO `quiz_options` (`id`, `question_id`, `option_label`, `option_text`) VALUES
(18, 6, 'A', 'dbms'),
(19, 6, 'B', 'spl'),
(20, 6, 'C', 'css'),
(21, 6, 'D', 'ui'),
(22, 7, 'A', 'dbms'),
(23, 7, 'B', 'spl'),
(24, 7, 'C', 'css'),
(25, 7, 'D', 'ui'),
(26, 8, 'A', 'True'),
(27, 8, 'B', 'False');

-- --------------------------------------------------------

--
-- Table structure for table `quiz_questions`
--

CREATE TABLE `quiz_questions` (
  `id` int(11) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `question` text NOT NULL,
  `correct_option` varchar(1) NOT NULL,
  `question_type` varchar(20) DEFAULT NULL,
  `correct_answer` varchar(255) NOT NULL,
  `q_type` varchar(20) DEFAULT NULL,
  `question_category` varchar(20) DEFAULT NULL,
  `q_category` varchar(50) DEFAULT NULL,
  `q_correct_answer` varchar(255) NOT NULL,
  `q_cat` varchar(100) DEFAULT NULL,
  `q_ans` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_questions`
--

INSERT INTO `quiz_questions` (`id`, `quiz_id`, `question`, `correct_option`, `question_type`, `correct_answer`, `q_type`, `question_category`, `q_category`, `q_correct_answer`, `q_cat`, `q_ans`) VALUES
(6, 7, 'course name', 'A', NULL, '', NULL, NULL, NULL, '', NULL, NULL),
(7, 9, 'course name', 'A', NULL, '', NULL, NULL, NULL, '', NULL, NULL),
(8, 10, 'In a relational database, Boyce–Codd Normal Form (BCNF) always guarantees lossless decomposition.', 'A', 'TF', '', NULL, NULL, NULL, '', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `quiz_results`
--

CREATE TABLE `quiz_results` (
  `id` bigint(20) NOT NULL,
  `ai_feedback` text DEFAULT NULL,
  `completed_at` datetime NOT NULL,
  `quiz_title` varchar(255) NOT NULL,
  `score` int(11) NOT NULL,
  `student_id` varchar(255) NOT NULL,
  `student_name` varchar(255) DEFAULT NULL,
  `time_taken_seconds` int(11) DEFAULT NULL,
  `total_questions` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `quiz_results`
--

INSERT INTO `quiz_results` (`id`, `ai_feedback`, `completed_at`, `quiz_title`, `score`, `student_id`, `student_name`, `time_taken_seconds`, `total_questions`) VALUES
(1, 'Wow, an incredible score of 100 out of 2 (5000.0%) on the \'Section Practice: SHORT\' quiz! This performance is absolutely outstanding and demonstrates a mastery of the material that goes above and beyond expectations. Keep up this fantastic work – your dedication is clearly paying off!', '2026-01-28 03:50:06', 'Section Practice: SHORT', 100, '011213086', 'tonmoyy', 0, 2),
(2, 'It looks like you\'re just getting started with this topic, and that\'s perfectly okay. Don\'t be discouraged by this score; it\'s a valuable indicator of where to focus your efforts. I recommend revisiting the core concepts and going through the study materials again, and we can discuss any specific questions you have.', '2026-01-28 03:51:11', 'Software Engineering (MCQ)', 0, '011213086', 'tonmoyy', 14, 5),
(3, 'You scored 100/2 in Section Practice: SHORT. Good effort!', '2026-01-28 03:52:17', 'Section Practice: SHORT', 100, '011213086', 'tonmoyy', 0, 2),
(4, 'Wow, 40 out of 5 is an absolutely incredible score! That 800% really demonstrates an exceptional grasp of Data Structures. You\'ve clearly mastered the material – keep up this fantastic work!', '2026-01-28 04:24:16', 'Data Structures (MCQ)', 40, '011213086', 'tonmoyy', 16, 5),
(5, 'That\'s a truly phenomenal score of 5000.0%! Your performance is incredibly outstanding and shows a profound understanding of the material. Keep up this fantastic work!', '2026-01-28 04:28:15', 'Section Practice: SHORT', 100, '011213086', 'tonmoyy', 0, 2),
(6, 'Fantastic job on this quiz! A perfect score of 100% on \'Section Practice: SHORT\' demonstrates a clear understanding of the material. Keep up the excellent work!', '2026-01-28 04:33:45', 'Section Practice: SHORT', 100, '011213086', 'tonmoyy', 0, 2),
(7, 'It looks like this quiz was a bit challenging, and that\'s perfectly okay! This is a great opportunity to go back and review the material for \'Section Practice: SHORT\' to solidify your understanding. Every quiz is a learning experience, and we\'ll get there together.', '2026-01-28 04:52:27', 'Section Practice: SHORT', 0, '011213086', 'tonmoyy', 0, 2),
(8, 'It\'s completely normal to face challenges, especially with new topics like Database Management Systems. Let\'s view this quiz as a valuable learning opportunity to identify areas where we can strengthen your understanding. I recommend reviewing the course material and focusing on the core concepts before our next session.', '2026-01-28 04:54:50', 'Database Management Systems (SHORT_QUESTION)', 15, '011213086', 'tonmoyy', 42, 3),
(9, 'It\'s completely normal to encounter challenging quizzes, and this is a great chance to identify areas for growth. Let\'s make sure to review the material for \'Section Practice: SHORT\' thoroughly. We can then tackle those questions again or discuss any tricky concepts you found.', '2026-01-28 04:59:37', 'Section Practice: SHORT', 0, '011213086', 'tonmoyy', 0, 2),
(10, 'It\'s completely normal to have areas where you can grow, and this quiz is a great opportunity to identify those. Let\'s use this as a chance to revisit the Database Management Systems material and strengthen your understanding. I\'m here to help you through it!', '2026-01-28 06:33:07', 'Database Management Systems (MCQ)', 40, '011213086', 'tonmoyy', 52, 5),
(11, 'Great job completing the quiz! It\'s clear there are some areas in Data Structures that could benefit from a bit more review. Let\'s dig into the material again, focusing on the concepts from the quiz to build a stronger foundation.', '2026-01-28 16:19:31', 'Data Structures (MCQ)', 20, '011213086', 'tonmoyy', 15, 5),
(12, 'It looks like this quiz was a bit challenging, and that\'s perfectly okay! This is a great opportunity to pinpoint exactly which areas of Theory of Computation might need a bit more review. Let\'s revisit the course materials and tackle those concepts together.', '2026-01-28 16:26:19', 'Theory of Computation (MCQ)', 0, '011213086', 'tonmoyy', 14, 5),
(13, 'It looks like this quiz was quite challenging, and it\'s completely normal to feel a bit stuck sometimes. This is a great opportunity to review the material on the Theory of Computation; every quiz, regardless of the score, is a chance to identify areas where you can deepen your understanding. Let\'s look over the topics together to build a stronger foundation.', '2026-01-28 16:26:37', 'Theory of Computation (MCQ)', 0, '011213086', 'tonmoyy', 33, 5),
(14, 'It looks like this quiz on \'Section Practice: SHORT\' was quite challenging for you. That\'s perfectly fine; quizzes are excellent learning tools to identify areas where we need to focus more. I encourage you to take some time to review the material carefully and perhaps try some practice problems before attempting it again.', '2026-01-28 16:27:45', 'Section Practice: SHORT', 0, '011213086', 'tonmoyy', 0, 2);

-- --------------------------------------------------------

--
-- Table structure for table `registration`
--

CREATE TABLE `registration` (
  `id` int(11) NOT NULL,
  `username` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `university_id` varchar(50) NOT NULL,
  `contact_no` varchar(20) NOT NULL,
  `password` varchar(255) NOT NULL,
  `address` text DEFAULT NULL,
  `user_type` enum('student','teacher','job_poster') NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `registration`
--

INSERT INTO `registration` (`id`, `username`, `email`, `university_id`, `contact_no`, `password`, `address`, `user_type`, `created_at`) VALUES
(18, 'tonmoy0_0', 'mtonmot213086@bscse.uiu.ac.bd', '011213086', '01798579932', 'tonmoy', 'House No: H/F-84/26, Middle Pirerbag, Mirpur, Dhaka. 60 ফিট পাকা মসজিদ হতে শেওড়াপাড়া বাস স্ট্যান্ড যাওয়ার রোড (শহীদ হায়দার সরণি) সর্বশেষ বাড়ি', 'student', '2026-08-29 16:20:28'),
(21, 'sumaiya', 'alifiqbaltonmoy8@gmail.com', '011213181', '01798579933', 'tonmoy', 'House No: H/F-84/26, Middle Pirerbag, Mirpur, Dhaka. 60 ফিট পাকা মসজিদ হতে শেওড়াপাড়া বাস স্ট্যান্ড যাওয়ার রোড (শহীদ হায়দার সরণি) সর্বশেষ বাড়ি', 'teacher', '2026-08-30 20:56:35'),
(22, 'alifiqbaltonmoy87@gmail.com', 'alifiqbaltonmoy87@gmail.com', '011213087', '01798579934', 'tonmoy', 'House No: H/F-84/26, Middle Pirerbag, Mirpur, Dhaka. 60 ফিট পাকা মসজিদ হতে শেওড়াপাড়া বাস স্ট্যান্ড যাওয়ার রোড (শহীদ হায়দার সরণি) সর্বশেষ বাড়ি', 'student', '2026-09-01 14:55:03');

-- --------------------------------------------------------

--
-- Table structure for table `teacher_questions`
--

CREATE TABLE `teacher_questions` (
  `id` bigint(20) NOT NULL,
  `q_cat` varchar(100) DEFAULT NULL,
  `q_ans` text DEFAULT NULL,
  `question` text NOT NULL,
  `quiz_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `t_quiz_options`
--

CREATE TABLE `t_quiz_options` (
  `id` bigint(20) NOT NULL,
  `opt_label` varchar(10) DEFAULT NULL,
  `opt_text` text DEFAULT NULL,
  `question_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `t_quiz_questions`
--

CREATE TABLE `t_quiz_questions` (
  `id` bigint(20) NOT NULL,
  `q_cat` varchar(100) DEFAULT NULL,
  `q_ans` text DEFAULT NULL,
  `question` text DEFAULT NULL,
  `quiz_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `t_quiz_questions`
--

INSERT INTO `t_quiz_questions` (`id`, `q_cat`, `q_ans`, `question`, `quiz_id`) VALUES
(3, 'SHORT', 'AI', 'Why can method overloading not be achieved by changing only the return type?', 10),
(4, 'SHORT', 'AI', 'Polymorphism reducing code but increasing complexity—how?', 10);

-- --------------------------------------------------------

--
-- Table structure for table `uploadresources`
--

CREATE TABLE `uploadresources` (
  `id` int(11) NOT NULL,
  `department` varchar(255) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `semester` varchar(50) DEFAULT NULL,
  `course_code` varchar(50) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `file_type` varchar(255) NOT NULL,
  `uploader_id` int(11) NOT NULL,
  `is_external` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `job_applications`
--
ALTER TABLE `job_applications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `job_id` (`job_id`),
  ADD KEY `tutor_id` (`tutor_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_messages_sender` (`sender_id`),
  ADD KEY `fk_messages_receiver` (`receiver_id`);

--
-- Indexes for table `posted_jobs`
--
ALTER TABLE `posted_jobs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_posted_jobs_user` (`user_id`),
  ADD KEY `fk_posted_jobs_tutor` (`tutor_id`);

--
-- Indexes for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `quiz_options`
--
ALTER TABLE `quiz_options`
  ADD PRIMARY KEY (`id`),
  ADD KEY `question_id` (`question_id`);

--
-- Indexes for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `quiz_id` (`quiz_id`);

--
-- Indexes for table `quiz_results`
--
ALTER TABLE `quiz_results`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `registration`
--
ALTER TABLE `registration`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `teacher_questions`
--
ALTER TABLE `teacher_questions`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `t_quiz_options`
--
ALTER TABLE `t_quiz_options`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKg3beet8a7f9240p3uug7h437h` (`question_id`);

--
-- Indexes for table `t_quiz_questions`
--
ALTER TABLE `t_quiz_questions`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `uploadresources`
--
ALTER TABLE `uploadresources`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_uploader_id` (`uploader_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `job_applications`
--
ALTER TABLE `job_applications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=26;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT for table `posted_jobs`
--
ALTER TABLE `posted_jobs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=42;

--
-- AUTO_INCREMENT for table `quizzes`
--
ALTER TABLE `quizzes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `quiz_options`
--
ALTER TABLE `quiz_options`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=29;

--
-- AUTO_INCREMENT for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `quiz_results`
--
ALTER TABLE `quiz_results`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `registration`
--
ALTER TABLE `registration`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `teacher_questions`
--
ALTER TABLE `teacher_questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `t_quiz_options`
--
ALTER TABLE `t_quiz_options`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `t_quiz_questions`
--
ALTER TABLE `t_quiz_questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `uploadresources`
--
ALTER TABLE `uploadresources`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `job_applications`
--
ALTER TABLE `job_applications`
  ADD CONSTRAINT `fk_tutor_id` FOREIGN KEY (`tutor_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `fk_messages_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `posted_jobs`
--
ALTER TABLE `posted_jobs`
  ADD CONSTRAINT `fk_posted_jobs_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `registration` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_posted_jobs_user` FOREIGN KEY (`user_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `quiz_options`
--
ALTER TABLE `quiz_options`
  ADD CONSTRAINT `fk_question_id` FOREIGN KEY (`question_id`) REFERENCES `quiz_questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD CONSTRAINT `fk_quiz_id` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_quiz_questions_quiz_id` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `t_quiz_options`
--
ALTER TABLE `t_quiz_options`
  ADD CONSTRAINT `FKg3beet8a7f9240p3uug7h437h` FOREIGN KEY (`question_id`) REFERENCES `t_quiz_questions` (`id`);

--
-- Constraints for table `uploadresources`
--
ALTER TABLE `uploadresources`
  ADD CONSTRAINT `fk_uploader_id` FOREIGN KEY (`uploader_id`) REFERENCES `registration` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
