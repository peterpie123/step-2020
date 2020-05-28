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
    movies: ["V for Vendetta", "Monty Python and the Holy Grail", "Lord of the Rings (all of 'em)"],
    shows: ["Seinfeld", "Star Trek: DS9", "The Mandalorian", "Night on Earth"],
    food: ["Sushi", "Pizza", "Yogurt", "Bacon", "Lasagna", "50¢ Walmart Pies"],
    videoGames: ["Civ 5", "Portal 2", "Kerbal Space Program", "Skyrim"],
}
//array of all the facts, pulled from above
const factsArr = mergeLists();
//lowercase array of the facts
const factsArrLowerCase = factsArr.map(v => v.toLowerCase());

document.addEventListener("DOMContentLoaded", init);
/* Called when DOM is loaded, performing necessary setup */
function init() {
    //simulate a button click when the user types enter in the agree box
    document.getElementById("agree-field").addEventListener("keyup", e => {
        //13 is the enter key
        if (e.keyCode === 13) {
            checkAgree();
        }
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

/* Sets the value of the agree-field textbox to the given value */
function setAgree(value) {
    let textbox = document.getElementById("agree-field");
    textbox.value = value;
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
            output += "<br/>Tip: try clicking one of following options!\n"
            output += "<br/><i>Did you mean:</i>\n";
            
            output += "<ul>\n"

            for(let i = 0; i < substrings.length; i++) {
                //cal the setAgree function, which will set the value of the agree textbox
                //to the value clicked
                output += `<li onclick="setAgree('${substrings[i]}')">${substrings[i]}</li>\n`;
            }
                

            output += "</ul>"
        } else {
            output = `No match. Please try again!`;
        }
    }


    let container = document.getElementById("agree-container");
    container.innerHTML = output;
}

