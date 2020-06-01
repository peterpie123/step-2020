// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Code corresponding to the enter key in a key event
const ENTER_CODE = 13;
// ID of the dropdown where the user picks which category to pull favorites from
const FAVORITE_CATEGORY_ID = 'type'
// User selection indicating that a random item should be pulled from the merged lists
const MERGE_ALL_LISTS = 'all';
// ID of where the randomly generated fact is sent
const RANDOM_FACT_OUTPUT_ID = 'fact-container';
// ID of the field that the user puts their favorites in
const USER_FAVORITE_INPUT_ID = 'agree-field';
// ID of the div where favorite search results are put
const FAVORITE_SEARCH_RESULT_ID = 'agree-container';
// Message shown when the user's favorite is the same as one given here
const FAVORITE_EXACT_MATCH = 'Exact match!';
// Message shown when the user's favorite is not similar to any here
const FAVORITE_NO_MATCH = 'No match here. Try another search or add this to your favorites!';
// Message prompting users to click on one of the favorite links
const FAVORITE_SEARCH_TIP = 'Tip: These are some of my favorites. Clicking on one'
                             + ' will favorite it for you too!'
// ID of the list containing all the search results from favorite search
const FAVORITE_SEARCH_RESULT_LIST_ID = 'fav-search-list';
// ID of the sidebar where user-selected favorites go
const USER_FAVORITE_LIST_ID = 'fav-list';
// ID of the placeholder text in the favorites are when there are no favorites selected
const FAVORITE_PLACEHOLDER_ID = 'fav-placeholder';
// Text in the favorite placeholder
const FAVORITE_PLACEHOLDER_TEXT = `You haven't favorited any items yet!`;
// Tag for creating a single checkbox in HTML
const HTML_CHECKBOX_TAG = '<input type="checkbox"/>'
// Property for what the user typed/selected in an input field
const TEXT_SELECTION = 'value';
// Property for whether a checkbox is checked or not
const CHECKBOX_IS_CHECKED = 'checked';
// ID of the comments container
const COMMENTS_CONTAINER = 'comments-container';
// URL for the comments servlet
const COMMENTS_URL = '/data';

// Dictionary of all the facts that can be pulled up
const FACTS = {
  movies: ['V for Vendetta', 'Monty Python and the Holy Grail',
    'Lord of the Rings (all of them)'],
  shows: ['Seinfeld', 'Star Trek: DS9', 'The Mandalorian', 'Night on Earth'],
  food: ['Sushi', 'Pizza', 'Yogurt', 'Bacon', 'Lasagna', '50¢ Walmart Pies'],
  videoGames: ['Civ 5', 'Portal 2', 'Kerbal Space Program', 'Skyrim'],
}
// Array of all the facts, pulled from above
const FACTS_NAMES = mergeLists();
// Lowercase array of the facts
const FACTS_NAMES_LOWERCASE = FACTS_NAMES.map(v => v.toLowerCase());

// Store what the user favorites
let userFavorites = new Set();

// Perform necessary setup
document.addEventListener('DOMContentLoaded', () => {
  // Populate from built-in favorites automatically when typing, when present
  if(documentHasElement(USER_FAVORITE_INPUT_ID)) {
    document.getElementById(USER_FAVORITE_INPUT_ID).addEventListener('keyup', e => {
      checkAgree();

      if (e.key == ENTER_CODE) {
        processNewFavorite();
      }
    });
  }

  if(documentHasElement(COMMENTS_CONTAINER)) {
    // Retrieve comments data from the servlet and add to DOM
    fetch(COMMENTS_URL).then(/* Convert from response stream */r => r.json()).then(comments => {
      comments.forEach(comment => appendElement(COMMENTS_CONTAINER, 'p', comment));
    })
  }
});

/* Returns a (pseudo)random element of the given array */
function randomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/* Merge all the lists in the dictionary facts into one list */
function mergeLists() {
  let out = [];

  Object.keys(FACTS).forEach(key => {
    out = out.concat(FACTS[key]);
  });
  return out;
}

/* Finds all occurances of substring in the values of arr and returns a list containing
 * the full strings in arr. Not that this ignores case for searching purposes */
function findSubstrings(arr, substring) {
  let out = [];

  substring = substring.toLowerCase();
  arr.forEach(value => {
    if (value.toLowerCase().includes(substring)) {
      out.push(value);
    }
  });

  return out;
}

/* Returns true if the document has an element with the given ID */
function documentHasElement(elementId) {
  return document.getElementById(elementId) ? true : false;
}

/* Appends a new HTML element to the given parent ID with the given information */
function appendElement(parentId, tagName, innerHtml, elementId = undefined, onclick = undefined) {
  let tag = document.createElement(tagName);
  let parent = document.getElementById(parentId);

  tag.innerHTML = innerHtml;
  if (elementId !== undefined) {
    tag.id = elementId;
  }
  if(onclick !== undefined) {
    tag.onclick = onclick;
  }
  parent.appendChild(tag);
}

/* Removes the given element from the DOM */
function removeElement(elementId) {
  document.getElementById(elementId).remove();
}

/* Removes all the children from the parent element */
function deleteChildren(parentId) {
  document.getElementById(parentId).innerHTML = '';
}

/* Retrieves the specified property from the given html element id */
function retrieveProperty(elementId, propertyName) {
  return document.getElementById(elementId)[propertyName];
}

/* Put a random fact from the given list into the appropriate container */
function generateFact() {
  let selection = retrieveProperty(FAVORITE_CATEGORY_ID, TEXT_SELECTION);
  let options;

  // Check if we need to merge all lists
  if (selection === MERGE_ALL_LISTS) {
    options = FACTS_NAMES;
  } else {
    options = FACTS[selection];
  }

  let fact = randomElement(options);
  // Replace the old fact with the one we just generated
  deleteChildren(RANDOM_FACT_OUTPUT_ID);
  appendElement(RANDOM_FACT_OUTPUT_ID, 'p', fact);
}

/* Checks user input against all the facts above and shows options similar
 * what they typed. */
function checkAgree() {
  let input = retrieveProperty(USER_FAVORITE_INPUT_ID, TEXT_SELECTION);

  // Remove any current contents so we can insert the results of this search
  deleteChildren(FAVORITE_SEARCH_RESULT_ID);

  // First check if input exactly matches a fact, ignoring case
  if (FACTS_NAMES_LOWERCASE.includes(input.toLowerCase())) {
    appendElement(FAVORITE_SEARCH_RESULT_ID, 'p', FAVORITE_EXACT_MATCH);
  } else {
    // Check if input is a substring of all the facts
    let substrings = findSubstrings(FACTS_NAMES, input);

    // Show the user each possibilty
    if (substrings.length > 0) {
      // Show the tip so users know what to do
      appendElement(FAVORITE_SEARCH_RESULT_ID, 'p', FAVORITE_SEARCH_TIP);

      // Create a list to hold the results and populate it
      appendElement(FAVORITE_SEARCH_RESULT_ID, 'ul', '', FAVORITE_SEARCH_RESULT_LIST_ID);

      for (let i = 0; i < substrings.length; i++) {
        // Call addUserFavorite when this element is clicked
        appendElement(FAVORITE_SEARCH_RESULT_LIST_ID, 'li', substrings[i], /* id = */ undefined, 
                      /* onclick = */ () => addUserFavorite(substrings[i]));
      }
    } else {
      appendElement(FAVORITE_SEARCH_RESULT_ID, 'p', FAVORITE_NO_MATCH);
    }
  }
}

/* Called when the user wants to add to their favorites */
function processNewFavorite() {
  addUserFavorite(retrieveProperty(USER_FAVORITE_INPUT_ID, TEXT_SELECTION));
}

/* Handles adding the apropriate HTML when adding a new favorite */
function addUserFavorite(favorite) {
  // Only add if we need to
  if (!userFavorites.has(favorite)) {
    // If this is the first favorite, need to remove the placeholder
    if (userFavorites.size === 0) {
      removeElement(FAVORITE_PLACEHOLDER_ID);
    }

    userFavorites.add(favorite);
    // Append the element with a checkbox as well as the text
    appendElement(USER_FAVORITE_LIST_ID, 'label', `${HTML_CHECKBOX_TAG}${favorite}`);
  }
}

/* Handles removing all the entries with checkboxes ticked */
function removeUserFavorites() {
  let list = document.getElementById(USER_FAVORITE_LIST_ID);

  // Look through all list elements for checked boxes
  // Iterate backwards as this list is live, so we avoid needing to adjust the index
  for (let i = list.childNodes.length - 1; i >= 0; i--) {
    let node = list.childNodes[i];

    // If this item's checkbox is ticked, remove it
    if (node.hasChildNodes() && node.firstChild.checked) {
      userFavorites.delete(node.innerText);
      node.remove();
    }
  }

  /* hasChildNodes() isn't working since there's whitespace treated as text by the DOM,
   * so instead check if there is any non-whitespace left */
  if (list.innerHTML.trim() === '') {
    // Add placeholder message
    appendElement(USER_FAVORITE_LIST_ID, 'p', FAVORITE_PLACEHOLDER_TEXT, FAVORITE_PLACEHOLDER_ID);
  }
}
