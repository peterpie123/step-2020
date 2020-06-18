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

describe('Selenium tests', () => {
  before(async () => {
    let options = new chrome.Options();

    driver = await new Builder(path).forBrowser('chrome').setChromeOptions(options).build();
    // Pull up the comments webpage
    await driver.get(URL);
  });

  describe('Comment management', () => {
    let nameField, textField, submitButton;
    let numComments;
    before(async () => {
      nameField = await driver.findElement(By.id('comment-name'));
      textField = await driver.findElement(By.id('comment-text-input'));
      submitButton = await driver.findElement(By.id('comment-submit'));
      numComments = (await getComments()).length;
    });

    it('should create a comment with just a name and text', async () => {
      await nameField.sendKeys('Test name');
      await textField.sendKeys('Test comment');
      await submitButton.click();

      // Really not good practice, but there is a delay between posting a comment and it 
      // appearing in the database
      setTimeout(async () => {
        let newNum = (await getComments()).length;
        // There should be one more comment
        assert.strictEqual(numComments + 1, newNum);
        numComments = newNum;
      }, WAIT_TIME);
    });

    it('should be able to delete a comment', async () => {
      await nameField.sendKeys('Delete name');
      await textField.sendKeys('Delete comment');
      await submitButton.click();

      setTimeout(async () => {
        let commentToggle = await driver.findElement(By.className('comment-toggle'));
        await commentToggle.click();
        let deleteButton = await driver.findElement(By.id('comments-delete'));
        await deleteButton.click();

        // Should be same number after creating and deleting a comment
        assert.strictEqual(numComments, (await getComments()).length)
      }, WAIT_TIME);
    });
  });

  after(async () => {
    await driver.quit();
  });
});
