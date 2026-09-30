import assert from 'node:assert/strict'
import process from 'node:process'
import { after, before, test } from 'node:test'
import { Builder, By, Key, until } from 'selenium-webdriver'
import chrome from 'selenium-webdriver/chrome.js'

const baseUrl = (process.env.TEACHQUEST_BASE_URL || 'http://127.0.0.1:5173').replace(/\/$/, '')
const testEmail = process.env.TEACHQUEST_TEST_EMAIL
const testPassword = process.env.TEACHQUEST_TEST_PASSWORD
const viewerPauseMs = 1400
let driver

async function pauseForViewer() {
  await new Promise((resolve) => setTimeout(resolve, viewerPauseMs))
}

async function signIn() {
  assert.ok(testEmail && testPassword, 'Run run_ai_quiz_test.bat to use your saved student credentials.')
  await driver.get(baseUrl)
  await pauseForViewer()
  await driver.findElement(By.id('email')).sendKeys(testEmail)
  await driver.findElement(By.id('password')).sendKeys(testPassword)
  await pauseForViewer()
  await driver.findElement(By.css("button[type='submit']")).click()
  await driver.wait(until.urlIs(`${baseUrl}/dashboard`), 20000, 'Sign-in failed. Check the saved credentials and make sure the backend is running.')
}

before(async () => {
  assert.ok(testEmail && testPassword, 'Run run_ai_quiz_test.bat to use your saved student credentials.')
  const options = new chrome.Options().addArguments('--disable-gpu', '--start-maximized').detachDriver(true)
  options.setChromeBinaryPath(
    process.env.TEACHQUEST_BROWSER_PATH || 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  )
  driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build()
})

after(async () => {
  if (driver) await driver.quit()
})

test('student generates an AI practice quiz and leaves it open for review', async () => {
  await signIn()
  await pauseForViewer()
  await driver.findElement(By.css("nav[aria-label='Main navigation'] a[href='/ai-practice']")).click()
  await driver.wait(until.elementLocated(By.css('.practice-config select')), 15000)
  await driver.wait(async () => (
    await driver.findElements(By.css('.practice-config select:first-child option'))
  ).length > 1, 15000)

  const resumeButtons = await driver.findElements(By.xpath("//button[normalize-space()='Resume quiz']"))
  assert.equal(resumeButtons.length, 0, "Finish the account's existing in-progress quiz before running this demo.")

  const courseSelect = await driver.findElement(By.css('.practice-config select:first-child'))
  await pauseForViewer()
  await courseSelect.click()
  await courseSelect.sendKeys(Key.ARROW_DOWN, Key.ENTER)
  const selectedCourse = await driver.executeScript('return arguments[0].selectedOptions[0].textContent', courseSelect)
  assert.equal(selectedCourse, 'Data Structures and Algorithms')

  await pauseForViewer()
  const topicInput = await driver.findElement(By.css('.practice-config input'))
  await topicInput.sendKeys('Binary search trees')

  await pauseForViewer()
  const questionMix = await driver.findElement(By.xpath("//label[contains(., 'Question mix')]/select"))
  await questionMix.click()
  await questionMix.sendKeys(Key.ARROW_UP, Key.ENTER)
  assert.equal(await driver.executeScript('return arguments[0].value', questionMix), 'SHORT_ANSWER_ONLY')

  const generateButton = await driver.findElement(By.xpath("//button[normalize-space()='Generate quiz']"))
  await driver.wait(until.elementIsEnabled(generateButton), 10000, 'Generate quiz is disabled; check the selected course and topic.')
  await driver.executeScript("arguments[0].scrollIntoView({ block: 'center', behavior: 'instant' });", generateButton)
  await driver.wait(() => driver.executeScript(
    'const rect = arguments[0].getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= window.innerHeight;',
    generateButton,
  ), 5000, 'Generate quiz did not scroll into view.')
  await pauseForViewer()
  await generateButton.click()
  await driver.wait(async () => {
    const session = await driver.findElements(By.css('.practice-session'))
    const alerts = await driver.findElements(By.css('.practice-alert'))
    return session.length > 0 || alerts.length > 0
  }, 300000, 'Quiz generation timed out. Check that Ollama is running with the configured model.')

  const generationErrors = await driver.findElements(By.css('.practice-alert'))
  if (generationErrors.length > 0) {
    throw new Error(await generationErrors[0].getText())
  }

  const questions = await driver.findElements(By.css('.practice-question'))
  assert.equal(questions.length, 8, 'Level 1 should generate eight questions.')
  assert.match(await driver.findElement(By.css('.practice-session-heading')).getText(), /Binary search trees/i)
  assert.match(await driver.findElement(By.css('.practice-session-footer')).getText(), /0 of 8 answered/)
})