import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './App.css'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import JobsPage from './pages/JobsPage'
import QuizPage from './pages/QuizPage'
import ResourcesPage from './pages/ResourcesPage'
import UploadResourcesPage from './pages/UploadResourcesPage'
import ChatPage from './pages/ChatPage'
import PostedJobsPage from './pages/PostedJobsPage'
import TeacherDashboardPage from './pages/TeacherDashboardPage'
import TeacherJobBoardPage from './pages/TeacherJobBoardPage'
import TeacherApplicationsPage from './pages/TeacherApplicationsPage'
import TeacherResourcesPage from './pages/TeacherResourcesPage'
import TeacherUploadResourcesPage from './pages/TeacherUploadResourcesPage'
import TeacherChatPage from './pages/TeacherChatPage'
import QuestionsPage from './pages/QuestionsPage'
import AdminResourceModerationPage from './pages/AdminResourceModerationPage'

function App() {
  return (
    <BrowserRouter>
      <div className="softlab-app">
        <Routes>
          <Route path="/" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/teacher-dashboard" element={<TeacherDashboardPage />} />
          <Route path="/teacher-job-board" element={<TeacherJobBoardPage />} />
          <Route path="/teacher-applications" element={<TeacherApplicationsPage />} />
          <Route path="/posted-jobs" element={<PostedJobsPage />} />
          <Route path="/jobs" element={<JobsPage />} />
          <Route path="/quiz" element={<QuizPage />} />
          <Route path="/resources" element={<ResourcesPage />} />
          <Route path="/upload-resources" element={<UploadResourcesPage />} />
          <Route path="/teacher-resources" element={<TeacherResourcesPage />} />
          <Route path="/teacher-upload-resources" element={<TeacherUploadResourcesPage />} />
          <Route path="/teacher-chat" element={<TeacherChatPage />} />
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/questions" element={<QuestionsPage />} />
          <Route path="/admin/resources" element={<AdminResourceModerationPage />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}

export default App
