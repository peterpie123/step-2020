import '@babel/polyfill';
import chrome from 'selenium-webdriver/chrome';
import { Builder, By, Key, Capabilities } from 'selenium-webdriver';
import assert, { doesNotMatch } from 'assert';
import { path } from 'chromedriver';
const fetch = require('node-fetch');

let driver = null;
const URL = 'http://localhost:8081';
const GET_COMMENTS = '/data?sort-ascending=true&pagination=0';
const WAIT_TIME = 500;

/** Gets comments from the server */
async function getComments() {
  return fetch(`${URL}${GET_COMMENTS}`).then(r => r.json());
}

/** Checks if the server has the given comment */
async function hasComment(name, text) {
  let comments = await getComments();
  return comments.filter(c => c.name === name && c.text === text).length > 0;
}

/** Waits the given number of milliseconds */
async function wait(time = WAIT_TIME) {
  return new Promise(resolve => {
    setTimeout(() => resolve(), time);
  });
}

describe('Selenium tests', () => {
  let nameField, textField, submitButton;
  before(async () => {
    let options = new chrome.Options();

    driver = await new Builder(path).forBrowser('chrome').setChromeOptions(options).build();
    // Pull up the comments webpage
    await driver.get(URL);
    nameField = await driver.findElement(By.id('comment-name'));
    textField = await driver.findElement(By.id('comment-text-input'));
    submitButton = await driver.findElement(By.id('comment-submit'));
  });

  /** Uses the comment creation form to create the given comment */
  async function createComment(name, text) {
    await nameField.sendKeys(name);
    await textField.sendKeys(text);
    await submitButton.click();
  }

  describe('Comment management', () => {
    it('should create a comment with just a name and text', async () => {
      let name = 'Test name';
      let text = 'Test comment';
      await createComment(name, text);

      // Really not good practice, but there is a delay between posting a comment and it 
      // appearing in the database
      await wait();
      let newNum = (await getComments()).length;
      // New comment should exist
      assert.strictEqual(await hasComment(name, text), true);
    });

    it('should be able to delete a comment', async () => {
      let name = 'Temp name';
      let text = 'Temp comment';
      await createComment(name, text);

      await wait();
      // New comment should exist
      assert.strictEqual(await hasComment(name, text), true);

      let commentToggle = await driver.findElement(By.className('comment-toggle'));
      await commentToggle.click();
      let deleteButton = await driver.findElement(By.id('comments-delete'));
      await deleteButton.click();

      await wait();
      // New comment should not exist
      assert.strictEqual(await hasComment(name, text), false);
    });

    it('should filter correctly', async () => {
      for (let i = 0; i < 2; i++) {
        await createComment('Filter name', 'Comment ' + i);
      }
      await wait();

      let filter = await driver.findElement(By.id('comment-filter'));
      filter.sendKeys('1');
      let refresh = await driver.findElement(By.id('comment-refresh-button'));
      refresh.click();
      await wait();

      // Make sure 'Comment 1' is present
      let text = await (await driver.findElement(By.css('.comment-text p:last-of-type'))).getText();
      assert.strictEqual(text, 'Comment 1');
      filter.clear();
    });
  });

  describe('Comment-analytics', () => {
    before(async () => {
      await createComment('Analytics name', 'Analytics text');
    });

    it('should be able to display comment analysis', async () => {
      let expand = await driver.findElement(By.className('comment-expand'));
      await expand.click();

      await wait();
      let analysis = await driver.findElement(By.className('comment-analysis-box'));
      let text = await (await analysis.findElement(By.tagName('p'))).getText();

      // Should be same number after creating and deleting a comment
      assert.strictEqual(text, 'Sentiment score: 4');
    });
  });

  after(async () => {
    await driver.quit();
  });
});
