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

function generateFact() {
    let dropdown = document.getElementById("type");
    let selection = dropdown.options[dropdown.selectedIndex].value;
    let options;

    /* Check if we need to merge all lists */
    if (selection === "all") {
        options = mergeLists();
    } else {
        options = facts[selection];
    }

    let fact = randomElement(options);

    // Add it to the page.
    let container = document.getElementById("fact-container");
    container.innerText = fact;
}

