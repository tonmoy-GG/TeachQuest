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
  await new Promise((resolve) => setTimeout(resolve, 1400))
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
}

async function signIn() {
  assert.ok(testEmail && testPassword, 'Run run_selenium_tests.bat to securely enter your student credentials.')
  await driver.get(baseUrl)
  await pauseForViewer()
  await driver.findElement(By.id('email')).sendKeys(testEmail)
  await driver.findElement(By.id('password')).sendKeys(testPassword)
  await pauseForViewer()
  await driver.findElement(By.css("button[type='submit']")).click()
  await driver.wait(until.urlIs(`${baseUrl}/dashboard`), 20000, 'Sign-in failed. Check the credentials and make sure the backend is running.')
  await driver.wait(until.elementLocated(By.xpath("//h3[normalize-space()='Quick Actions']")), 10000)
}

before(async () => {
  assert.ok(testEmail && testPassword, 'Run run_selenium_tests.bat to securely enter your student credentials.')
  const options = new chrome.Options().addArguments('--disable-gpu', '--start-maximized')
  options.setChromeBinaryPath(
    process.env.TEACHQUEST_BROWSER_PATH || 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  )
  driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build()
})

after(async () => {
  if (driver) await driver.quit()
})

test('student signs in and completes the posted-job workflow', async () => {
  await signIn()
  await pauseForViewer()
  assert.equal(await driver.findElement(By.css('.welcome-section h2')).isDisplayed(), true)

  const jobTitle = `Selenium tutor request ${Date.now()}`
  await pauseForViewer()
  await driver.findElement(By.xpath("//a[.//h4[normalize-space()='Hire Tutor']]" )).click()
  await pauseForViewer()
  await driver.findElement(By.id('job-title')).sendKeys(jobTitle)
  await driver.findElement(By.id('subject')).sendKeys('Calculus II')
  await driver.findElement(By.id('days-per-week')).sendKeys('3')
  await driver.findElement(By.id('requirements')).sendKeys('Help with integration techniques and exam practice.')
  await driver.findElement(By.id('location')).sendKeys('Online')
  await driver.findElement(By.id('salary')).sendKeys('5000 tk')
  await pauseForViewer()
  await clickCentered(By.xpath("//button[.//span[normalize-space()='Post Job']]"))

  await driver.wait(until.urlIs(`${baseUrl}/posted-jobs`), 10000)
  await driver.wait(until.elementLocated(By.xpath(`//h3[normalize-space()='${jobTitle}']`)), 10000)
  assert.equal(await driver.findElement(By.xpath("//strong[normalize-space()='Calculus II']")).getText(), 'Calculus II')
  await pauseForViewer()
  await driver.navigate().refresh()
  await driver.wait(until.elementLocated(By.xpath(`//h3[normalize-space()='${jobTitle}']`)), 10000)

  await pauseForViewer()
  await driver.findElement(By.xpath("//button[normalize-space()='Edit']")).click()
  await driver.wait(until.urlContains('/jobs?edit='), 10000)
  const titleInput = await driver.findElement(By.id('job-title'))
  await titleInput.clear()
  const updatedTitle = `${jobTitle} updated`
  await titleInput.sendKeys(updatedTitle)
  await pauseForViewer()
  await clickCentered(By.xpath("//button[.//span[normalize-space()='Update Job']]"))
  await driver.wait(until.elementLocated(By.xpath(`//h3[normalize-space()='${updatedTitle}']`)), 10000)

  await pauseForViewer()
  await driver.findElement(By.xpath("//button[normalize-space()='Delete']")).click()
  await driver.wait(async () => (
    await driver.findElements(By.xpath(`//h3[normalize-space()='${updatedTitle}']`))
  ).length === 0, 10000)
  await pauseForViewer()
  await driver.findElement(By.xpath("//button[normalize-space()='Logout']")).click()
  await driver.wait(until.urlIs(`${baseUrl}/`), 10000)
})