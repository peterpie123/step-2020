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

/** 
 * @fileoverview This file contains various utility functions, mostly related
 * to DOM interaction for the other JS files in this project */

/** Returns a (pseudo)random element of the given array */
export function randomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/** Finds all occurances of substring in the values of arr and returns a list containing
 * the full strings in arr. Not that this ignores case for searching purposes */
export function findSubstrings(arr, substring) {
  let out = [];

  substring = substring.toLowerCase();
  arr.forEach(value => {
    if (value.toLowerCase().includes(substring)) {
      out.push(value);
    }
  });

  return out;
}

/** Returns true if the document has an element with the given ID */
export function documentHasElement(elementId) {
  return document.getElementById(elementId) ? true : false;
}

/** Appends a new HTML element to the given parent ID with the given information */
export function appendElement(parentId, tagName, innerHtml, elementId = undefined,
  onclick = undefined, className = undefined) {
  let tag = document.createElement(tagName);
  let parent = document.getElementById(parentId);

  tag.innerHTML = innerHtml;
  if (elementId !== undefined) {
    tag.id = elementId;
  }
  if (onclick !== undefined) {
    tag.onclick = onclick;
  }
  if (className !== undefined) {
    tag.className = className;
  }
  parent.appendChild(tag);
}

/** Removes the given element from the DOM */
export function removeElement(elementId) {
  document.getElementById(elementId).remove();
}

/** Removes all the children from the parent element */
export function deleteChildren(parentId) {
  document.getElementById(parentId).innerHTML = '';
}

/** Retrieves the specified property from the given html element id */
export function retrieveProperty(elementId, propertyName) {
  return document.getElementById(elementId)[propertyName];
}

/** Adds the given className to the given element */
export function addClass(elementId, className) {
  document.getElementById(elementId).classList.add(className);
}

/** Removes the given className from the given element */
export function removeClass(elementId, className) {
  document.getElementById(elementId).classList.remove(className);
}

/** Sets the ID of the given element to the new ID */
export function setId(elementId, newId) {
  document.getElementById(elementId).id = newId;
}

/** Returns a nicely-formatted string with information about time passed (e.g. 5 minutes ago).
 *  Will ignore leap years. */
export function timePassed(date) {
  // Will be set to 's' if statement is plural
  let plural = '';

  // Seconds of difference
  let seconds = Math.floor((new Date() - date) / 1000);
  if (seconds < 60) {
    return 'Seconds ago';
  }

  let minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    if (minutes > 1) {
      plural = 's';
    }
    return `${minutes} minute${plural} ago`;
  }

  let hours = Math.floor(minutes / 60);
  if (hours < 24) {
    if (hours > 1) {
      plural = 's';
    }
    return `${hours} hour${plural} ago`;
  }

  let days = Math.floor(hours / 24);
  if (days < 365) {
    // Ignore leap years
    if (days > 1) {
      plural = 's';
    }
    return `${days} day${plural} ago`;
  }

  // Ignore leap years
  let years = Math.floor(days / 365);
  if (years > 1) {
    plural = 's';
  }
  return `${years} year${plural} ago`;
}
