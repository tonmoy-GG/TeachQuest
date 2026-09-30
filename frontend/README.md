# React + Vite

## Selenium browser tests

The default Selenium test opens Chrome, signs in, posts a job, reloads and edits it, then deletes it. The backend must be running for sign-in.

Requirements: Node.js, Google Chrome, and the npm dependencies installed. Selenium Manager downloads the matching ChromeDriver when needed. Start the app with `run_teachquest.bat`, then run `run_selenium_tests.bat` from the repository root. On first run, enter the student email and password; the password is encrypted for your Windows account outside the repository. Later runs reuse it automatically. Run `run_selenium_tests.bat -ResetCredentials` to replace the saved account.

Pass `-TestScript test:resource-upload` or `-TestScript test:ai-quiz` to run another Selenium suite.

You can also run `npm run test:e2e` from `frontend` after setting `TEACHQUEST_TEST_EMAIL` and `TEACHQUEST_TEST_PASSWORD` in that terminal. Set `TEACHQUEST_BASE_URL` if the frontend is running at a different URL, or `TEACHQUEST_BROWSER_PATH` if Chrome is installed outside its standard location.

The AI quiz feature has its own test and launcher: run `run_ai_quiz_test.bat` from the repository root. It signs in, generates a Level 1 short-answer quiz, verifies the eight questions, then leaves the visible Chrome window open for review. The generated attempt remains in progress and prevents another quiz attempt on that account until it is submitted or expires. Quiz generation requires Ollama to be running with the configured model (`qwen2.5:1.5b` by default).

The resource upload feature has its own test and launcher: run `run_resource_upload_test.bat` from the repository root. It signs in, uploads a generated text file, and verifies that the file appears in the resource library. The uploaded resource remains in the account's library after the test.

The Community Q&A feature has its own test and launcher: run `run_community_qa_test.bat` from the repository root. It signs in, posts a uniquely named question, submits an answer, and leaves Chrome open on the conversation. The question and answer remain in the database because the feature has no delete endpoint.


This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
