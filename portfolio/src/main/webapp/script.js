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

//dictionary of all the facts that can be pulled up
const facts = {
    movies: ["V for Vendetta", "Monty Python and the Holy Grail", "Lord of the Rings (all of them)"],
    shows: ["Seinfeld", "Star Trek: DS9", "The Mandalorian", "Night on Earth"],
    food: ["Sushi", "Pizza", "Yogurt", "Bacon", "Lasagna", "50¢ Walmart Pies"],
    videoGames: ["Civ 5", "Portal 2", "Kerbal Space Program", "Skyrim"],
}
//array of all the facts, pulled from above
const factsArr = mergeLists();
//lowercase array of the facts
const factsArrLowerCase = factsArr.map(v => v.toLowerCase());
//store what the user favorites
var userFavorites = new Set();

document.addEventListener("DOMContentLoaded", init);
/* Called when DOM is loaded, performing necessary setup */
function init() {
    //Populate from built-in favorites automatically when typing
    document.getElementById("agree-field").addEventListener("keyup", e => {
        checkAgree();
    });
}

/* Returns a (pseudo)random element of the given array */
function randomElement(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

/* Merge all the lists in the dictionary facts into one list */
function mergeLists() {
    let out = [];

    Object.keys(facts).forEach(key => {
        out = out.concat(facts[key]);
    });

    return out;
}

/* Called when the user wants to add the contents of agree-field to their favorites */
function processNewFavorite() {
    let textbox = document.getElementById("agree-field");
    addUserFavorite(textbox.value);
}

/* Handles adding the apropriate HTML when adding a new favorite */
function addUserFavorite(favorite) {
    //Only add if we need to
    if(!userFavorites.has(favorite)) {
        //if this is the first favorite, need to remove the placeholder
        if(userFavorites.size === 0) {
            document.getElementById("fav-placeholder").remove();
        }

        userFavorites.add(favorite);

        let listTag = document.createElement("li");
        listTag.innerHTML = `<input type="checkbox"/>${favorite}`

        document.getElementById("fav-list").appendChild(listTag);
    }
}

/* Handles removing all the entries with checkboxes ticked */
function removeUserFavorites() {
    let list = document.getElementById("fav-list");

    //look through all list elements for checked boxes
    //iterate backwards as this list is live, so we avoid needing to adjust the index
    for(let i = list.childNodes.length - 1; i >= 0; i--) {
        let node = list.childNodes[i];

        //if this item's checkbox is ticked, remove it
        if(node.hasChildNodes() && node.firstChild.checked) {
            userFavorites.delete(node.innerText);
            node.remove();
        }
    }

    //hasChildNodes isn't working since there's whitespace treated as text by the DOM,
    //so instead check if there is any non-whitespace left
    if(list.innerHTML.trim() === "") {
        //add placeholder message
        let listTag = document.createElement("li");
        listTag.id = "fav-placeholder";
        listTag.innerText = "You haven't favorited any items yet!";

        list.appendChild(listTag);
    }

}

/* Finds all occurances of substring in the values of arr and returns a list containing
 * the full strings in arr. Not that this ignores case for searching purposes */
function findSubstrings(arr, substring) {
    let out = [];

    substring = substring.toLowerCase();

    arr.forEach(value => {
        if(value.toLowerCase().includes(substring)) {
            out.push(value);
        }
    });

    return out;
}

/* Put a random fact from the given list in the fact-container */
function generateFact() {
    let dropdown = document.getElementById("type");
    let selection = dropdown.options[dropdown.selectedIndex].value;
    let options;

    /* Check if we need to merge all lists */
    if (selection === "all") {
        options = factsArr;
    } else {
        options = facts[selection];
    }

    let fact = randomElement(options);

    // Add it to the page.
    let container = document.getElementById("fact-container");
    container.innerText = fact;
}

/* Checks user input against all the facts that have been input */
function checkAgree() {
    let textbox = document.getElementById("agree-field");
    let input = textbox.value;
    //the value being put in the agree container
    let output = "";

    //first check if input exactly matches a fact, ignoring case
    if(factsArrLowerCase.includes(input.toLowerCase())) {
        output = "<p>Exact match!</p>";
    } else {
        //check if input is a substring of all the facts
        let substrings = findSubstrings(factsArr, input);

        //Show the user each possibilty and attach a listener to auto-populate the textbox
        if(substrings.length > 0) {
            output += `<br/>Tip: These are some of my favorites. Clicking on one 
                       will favorite it for you too!\n`            
            output += "<ul>\n"

            for(let i = 0; i < substrings.length; i++) {
                //On click add the item to the users' favorite list
                output += `<li onclick="addUserFavorite('${substrings[i]}')">${substrings[i]}</li>\n`;
            }
                

            output += "</ul>"
        } else {
            output = `No match here. Try another search or add this to your favorites!`;
        }
    }

    let container = document.getElementById("agree-container");
    container.innerHTML = output;
}

