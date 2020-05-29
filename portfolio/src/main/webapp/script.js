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

//Code corresponding to the enter key in a key event
const ENTER_CODE = 13;
//ID of the dropdown where the user picks which category to pull favorites from
const FAVORITE_CATEGORY_ID = 'type'
//User selection indicating that a random item should be pulled from the merged lists
const MERGE_ALL_LISTS = 'all';
//ID of where the randomly generated fact is sent
const RANDOM_FACT_OUTPUT_ID = 'fact-container';
//ID of the field that the user puts their favorites in
const USER_FAVORITE_INPUT_ID = 'agree-field';
//ID of the div where favorite search results are put
const FAVORITE_SEARCH_RESULT_ID = 'agree-container';
//ID of the sidebar where user-selected favorites go
const USER_FAVORITE_LIST_ID = 'fav-list';
//ID of the placeholder text in the favorites are when there are no favorites selected
const FAVORITE_PLACEHOLDER_ID = 'fav-placeholder';


//dictionary of all the facts that can be pulled up
const FACTS = {
  movies: ['V for Vendetta', 'Monty Python and the Holy Grail', 
           'Lord of the Rings (all of them)'],
  shows: ['Seinfeld', 'Star Trek: DS9', 'The Mandalorian', 'Night on Earth'],
  food: ['Sushi', 'Pizza', 'Yogurt', 'Bacon', 'Lasagna', '50¢ Walmart Pies'],
  videoGames: ['Civ 5', 'Portal 2', 'Kerbal Space Program', 'Skyrim'],
}
//array of all the facts, pulled from above
const FACTS_ARR = mergeLists();
//lowercase array of the facts
const FACTS_ARR_LC = FACTS_ARR.map(v => v.toLowerCase());
//store what the user favorites
let userFavorites = new Set();

//perform necessary setup
document.addEventListener('DOMContentLoaded', () => {
  //Populate from built-in favorites automatically when typing
  document.getElementById(USER_FAVORITE_INPUT_ID).addEventListener('keyup', e => {
    checkAgree();

    if (e.key == ENTER_CODE) {
      processNewFavorite();
    }
  });
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

/* Put a random fact from the given list into the appropriate container */
function generateFact() {
  let dropdown = document.getElementById(FAVORITE_CATEGORY_ID);
  let selection = dropdown.options[dropdown.selectedIndex].value;
  let options;

  /* Check if we need to merge all lists */
  if (selection === MERGE_ALL_LISTS) {
    options = FACTS_ARR;
  } else {
    options = FACTS[selection];
  }

  let fact = randomElement(options);
  // Add fact to the page.
  let container = document.getElementById(RANDOM_FACT_OUTPUT_ID);
  container.innerText = fact;
}

/* Checks user input against all the facts above and shows options similar
 * what they typed. */
function checkAgree() {
  let textbox = document.getElementById(USER_FAVORITE_INPUT_ID);
  let input = textbox.value;
  //the value being put in the agree container
  let output = '';

  //first check if input exactly matches a fact, ignoring case
  if (FACTS_ARR_LC.includes(input.toLowerCase())) {
    output = '<p>Exact match!</p>';
  } else {
    //check if input is a substring of all the facts
    let substrings = findSubstrings(FACTS_ARR, input);

    //Show the user each possibilty and attach a listener to auto-populate the textbox
    if (substrings.length > 0) {
      output += `<br/>Tip: These are some of my favorites. Clicking on one 
                       will favorite it for you too!\n`
      output += '<ul>\n'

      for (let i = 0; i < substrings.length; i++) {
        //On click add the item to the users' favorite list
        output += `<li onclick="addUserFavorite('${substrings[i]}')">
                    ${substrings[i]}</li>\n`;
      }

      output += '</ul>'
    } else {
      output = 'No match here. Try another search or add this to your favorites!';
    }
  }

  let container = document.getElementById(FAVORITE_SEARCH_RESULT_ID);
  container.innerHTML = output;
}

/* Called when the user wants to add to their favorites */
function processNewFavorite() {
  let textbox = document.getElementById(USER_FAVORITE_INPUT_ID);
  addUserFavorite(textbox.value);
}

/* Handles adding the apropriate HTML when adding a new favorite */
function addUserFavorite(favorite) {
  //Only add if we need to
  if (!userFavorites.has(favorite)) {
    //if this is the first favorite, need to remove the placeholder
    if (userFavorites.size === 0) {
      document.getElementById(FAVORITE_PLACEHOLDER_ID).remove();
    }

    userFavorites.add(favorite);

    let labelTag = document.createElement('label');
    labelTag.innerHTML = `<input type="checkbox"/>${favorite}`

    document.getElementById(USER_FAVORITE_LIST_ID).appendChild(labelTag);
  }
}

/* Handles removing all the entries with checkboxes ticked */
function removeUserFavorites() {
  let list = document.getElementById(USER_FAVORITE_LIST_ID);

  //look through all list elements for checked boxes
  //iterate backwards as this list is live, so we avoid needing to adjust the index
  for (let i = list.childNodes.length - 1; i >= 0; i--) {
    let node = list.childNodes[i];

    //if this item's checkbox is ticked, remove it
    if (node.hasChildNodes() && node.firstChild.checked) {
      userFavorites.delete(node.innerText);
      node.remove();
    }
  }

  //hasChildNodes isn't working since there's whitespace treated as text by the DOM,
  //so instead check if there is any non-whitespace left
  if (list.innerHTML.trim() === '') {
    //add placeholder message
    let listTag = document.createElement('p');

    listTag.id = FAVORITE_PLACEHOLDER_ID;
    listTag.innerText = `You haven't favorited any items yet!`;
    list.appendChild(listTag);
  }
}
