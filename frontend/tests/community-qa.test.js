import assert from 'node:assert/strict'
import process from 'node:process'
import { after, before, test } from 'node:test'
import { Builder, By, until } from 'selenium-webdriver'
import chrome from 'selenium-webdriver/chrome.js'

const baseUrl = (process.env.TEACHQUEST_BASE_URL || 'http://localhost:5173').replace(/\/$/, '')
const testEmail = process.env.TEACHQUEST_TEST_EMAIL
const testPassword = process.env.TEACHQUEST_TEST_PASSWORD
let driver

async function pauseForViewer() {
  await new Promise((resolve) => setTimeout(resolve, 2000))
}

async function clickCentered(locator) {
  const element = await driver.findElement(locator)
  await driver.executeScript("arguments[0].scrollIntoView({ block: 'center', behavior: 'instant' });", element)
  await driver.wait(() => driver.executeScript(
    'const rect = arguments[0].getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= window.innerHeight;',
    element,
  ), 5000)
  await pauseForViewer()
  await element.click()
  await pauseForViewer()
}

async function signIn() {
  assert.ok(testEmail && testPassword, 'Run run_community_qa_test.bat to use your saved student credentials.')
  await driver.get(baseUrl)
  await pauseForViewer()
  const emailInput = await driver.findElement(By.id('email'))
  await emailInput.sendKeys(testEmail)
  await pauseForViewer()
  const passwordInput = await driver.findElement(By.id('password'))
  await passwordInput.sendKeys(testPassword)
  await pauseForViewer()
  const signInButton = await driver.findElement(By.css("button[type='submit']"))
  await signInButton.click()
  await pauseForViewer()
  await driver.wait(until.urlIs(`${baseUrl}/dashboard`), 20000, 'Sign-in failed. Check the credentials and make sure the backend is running.')
  await pauseForViewer()
}

before(async () => {
  assert.ok(testEmail && testPassword, 'Run run_community_qa_test.bat to use your saved student credentials.')
  const options = new chrome.Options().addArguments('--disable-gpu', '--start-maximized').detachDriver(true)
  options.setChromeBinaryPath(
    process.env.TEACHQUEST_BROWSER_PATH || 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  )
  driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build()
})

after(async () => {
  if (driver) await driver.quit()
})

test('student posts a community question and answers it', async () => {
  const questionTitle = `Selenium community question ${Date.now()}`
  const questionBody = `Community Q&A browser test context ${Date.now()}.`
  const answerBody = `Selenium answer verification ${Date.now()}.`

  await signIn()
  await driver.get(`${baseUrl}/questions`)
  await pauseForViewer()
  await driver.wait(until.elementLocated(By.css('.question-composer input[name="title"]')), 10000)
  await pauseForViewer()

  await driver.findElement(By.css('.question-composer input[name="title"]')).sendKeys(questionTitle)
  await pauseForViewer()
  await driver.findElement(By.css('.question-composer textarea[name="body"]')).sendKeys(questionBody)
  await pauseForViewer()
  await driver.findElement(By.css('.question-composer select[name="category"] option')).click()
  await pauseForViewer()
  await clickCentered(By.css('.question-composer button[type="submit"]'))

  const questionCard = By.xpath(`//button[contains(@class, 'question-card')][.//h3[normalize-space()='${questionTitle}']]`)
  await driver.wait(until.elementLocated(questionCard), 15000, 'Posted question did not appear in the community feed.')
  await pauseForViewer()
  assert.equal(await driver.findElement(By.css('.question-detail h2')).getText(), questionTitle)

  await driver.findElement(By.css('.answer-form textarea')).sendKeys(answerBody)
  await pauseForViewer()
  await clickCentered(By.css('.answer-form button[type="submit"]'))
  const answer = By.xpath(`//article[contains(@class, 'answer-item')]//p[normalize-space()='${answerBody}']`)
  await driver.wait(until.elementLocated(answer), 15000, 'Posted answer did not appear in the question conversation.')
  await pauseForViewer()
  assert.equal(await driver.findElement(answer).getText(), answerBody)
})