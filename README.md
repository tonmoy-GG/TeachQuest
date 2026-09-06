# TeachQuest

TeachQuest is a React/Vite frontend for student and teacher peer-learning workflows.

## Current project

- `frontend/` contains the active React application.
- `run_teachquest.bat` starts the frontend and the compatible Spring Boot API.
- `DESIGN_SYSTEM.md` documents the shared visual language.

## Run locally

Requirements: Node.js, Java 17, and MySQL if backend persistence is enabled.

```powershell
cd frontend
npm install
npm run dev
```

To start both applications on Windows, run `run_teachquest.bat` from the repository root.

## Legacy project

The previous server-rendered HTML/CSS/JavaScript/Spring Boot application is maintained separately in `TeachQuest(AOOP)/` and is excluded from this repository's GitHub upload. It has its own README and Maven wrapper.