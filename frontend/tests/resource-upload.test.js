import assert from 'node:assert/strict'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
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

async function clickElementCentered(element) {
  await driver.executeScript("arguments[0].scrollIntoView({ block: 'center', behavior: 'instant' });", element)
  await driver.wait(() => driver.executeScript(
    'const rect = arguments[0].getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= window.innerHeight;',
    element,
  ), 5000)
  await pauseForViewer()
  await element.click()
  await pauseForViewer()
}

async function clickCentered(locator) {
  await clickElementCentered(await driver.findElement(locator))
}

async function signIn() {
  assert.ok(testEmail && testPassword, 'Run run_resource_upload_test.bat to use your saved student credentials.')
  await driver.get(baseUrl)
  await pauseForViewer()
  const emailInput = await driver.findElement(By.id('email'))
  await pauseForViewer()
  await emailInput.sendKeys(testEmail)
  await pauseForViewer()
  const passwordInput = await driver.findElement(By.id('password'))
  await pauseForViewer()
  await passwordInput.sendKeys(testPassword)
  await pauseForViewer()
  const signInButton = await driver.findElement(By.css("button[type='submit']"))
  await pauseForViewer()
  await signInButton.click()
  await pauseForViewer()
  await driver.wait(until.urlIs(`${baseUrl}/dashboard`), 20000, 'Sign-in failed. Check the credentials and make sure the backend is running.')
  await pauseForViewer()
}

before(async () => {
  assert.ok(testEmail && testPassword, 'Run run_resource_upload_test.bat to use your saved student credentials.')
  const options = new chrome.Options().addArguments('--disable-gpu', '--start-maximized').detachDriver(true)
  options.setChromeBinaryPath(
    process.env.TEACHQUEST_BROWSER_PATH || 'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  )
  driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build()
})

after(async () => {
  if (driver) await driver.quit()
})

test('student uploads a resource and finds it in the library', async () => {
  const fixtureDirectory = await mkdtemp(path.join(os.tmpdir(), 'teachquest-resource-'))
  const fileName = `selenium-resource-${Date.now()}.txt`
  const filePath = path.join(fixtureDirectory, fileName)

  try {
    await writeFile(filePath, 'TeachQuest Selenium resource upload test fixture.')
    await signIn()
    await driver.get(`${baseUrl}/upload-resources`)
    await pauseForViewer()
    await driver.wait(until.elementLocated(By.id('department')), 10000)
    await pauseForViewer()

    await pauseForViewer()
    await driver.findElement(By.css('#department option[value="cse"]')).click()
    await pauseForViewer()
    await driver.findElement(By.css('#category option[value="notes"]')).click()
    await pauseForViewer()
    await driver.findElement(By.css('#trimester option[value="Trimester 1"]')).click()
    await pauseForViewer()
    await driver.findElement(By.css('#course option[value="CSE3711"]')).click()
    await pauseForViewer()
    await driver.findElement(By.id('description')).sendKeys('Selenium resource upload verification')
    await pauseForViewer()
    await driver.findElement(By.css('input[type="file"]')).sendKeys(filePath)
    await pauseForViewer()
    await clickCentered(By.xpath("//button[normalize-space()='Upload File']"))

    await driver.wait(until.urlIs(`${baseUrl}/resources`), 20000, 'Upload did not return to the resource library.')
    await pauseForViewer()
    const courseCard = By.xpath("//article[contains(@class, 'resource-card')][.//h3[normalize-space()='CSE3711']]")
    await driver.wait(until.elementLocated(courseCard), 15000, 'Uploaded course was not listed in the resource library.')
    await pauseForViewer()
    const categoryButtons = await driver.findElements(By.xpath("//article[contains(@class, 'resource-card')][.//h3[normalize-space()='CSE3711']]//button[contains(@class, 'resource-folder-item')]"))
    const uploadedFile = By.xpath(`//span[contains(@class, 'resource-file-name') and normalize-space()='${fileName}']`)
    let uploadedFileIsVisible = false

    for (const categoryButton of categoryButtons) {
      await clickElementCentered(categoryButton)
      try {
        await driver.wait(until.elementLocated(uploadedFile), 1500)
        uploadedFileIsVisible = true
        break
      } catch {
        // Continue through this course's folders until the new file is found.
      }
    }

    assert.equal(uploadedFileIsVisible, true, 'Uploaded file was not visible in its resource category.')
    await pauseForViewer()
  } finally {
    await rm(fixtureDirectory, { recursive: true, force: true })
  }
})