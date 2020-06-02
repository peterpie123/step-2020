// Copyright 2020 Google LLC
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

// Import utlity functions
import { documentHasElement, appendElement, deleteChildren, retrieveProperty } from './script.js';

// ID of the comments container
const COMMENTS_CONTAINER = 'comments-container';
// URL for the comments servlet
const COMMENTS_URL = '/data';
// Indicates that comments should be sorted with the newest on top
const COMMENTS_SORT_NEWEST = 1;
// Indicates that comments should be sorted with the oldest on top
const COMMENTS_SORT_OLDEST = -1;
// ID of the icon that indicates which direction to sort
const COMMENTS_SORT_ICON = 'icon-selected';
// Icon corresponding to having the newest comment on top
const COMMENTS_ICON_NEWEST = 'fa-chevron-circle-up';
// Icon corresponding to having the oldest comment on top
const COMMENTS_ICON_OLDEST = 'fa-chevron-circle-down';
// Query string for indicating how many comments to retrieve
const NUM_COMMENTS_QUERY = 'num-comments';
// Property for what the user typed/selected in an input field
const TEXT_SELECTION = 'value';
// ID of the field where the user selects how many comments to retrieve
const NUM_COMMENTS_FIELD = 'num-comments';
// Header containing the total number of comments stored
const TOTAL_NUMBER_HEADER = "num-comments";

// List of comments currently on the page
let pageComments = [];
// How the comments are currently sorted
let commentsSort = COMMENTS_SORT_NEWEST;

// Perform necessary setup
document.addEventListener('DOMContentLoaded', () => {
  if (documentHasElement(COMMENTS_CONTAINER)) {
    // Retrieve comments data from the servlet and add to DOM
    retrieveComments();
  }
});

/** Adds the given comment to the page with the given ID. */
function addSingleComment(comment, id) {
  appendElement(COMMENTS_CONTAINER, 'div', '', /* id = */ id, undefined, 'comment');
  // Format the time nicely, by starting at the epoch and setting the milliseconds
  let date = new Date(0);
  date.setUTCMilliseconds(comment.timestamp);

  // ID for the container which houses the information for a comment
  let contentId = id + '-content';
  appendElement(id, 'div', '', contentId, undefined, 'comment-text');

  appendElement(contentId, 'p', `<b>${comment.name}</b>\t${date.toLocaleTimeString()}`);
  appendElement(contentId, 'p', comment.text);
  // Add the checkbox that will toggle deleting this comment
  appendElement(id, 'div', '', undefined, () => prepareDelete(id), 'comment-toggle');
}

/**Adds the given list of comments to the page, and only adds them to the pageComments
 * array if pushComments is true. startIndex indicates where to start counting up comment ID's */
function addComments(comments, pushComments = true, startIndex = pageComments.length) {
  comments.forEach((comment, index) => {
    let id = `comment-${index + startIndex}`;
    addSingleComment(comment, id);

    if (pushComments) {
      pageComments.push(comment);
    }
  });
}

/** Reverses the order of comments */
function reverseComments() {
  pageComments.reverse();
  // Remove existing comments and add the reversed list
  deleteChildren(COMMENTS_CONTAINER);
  addComments(pageComments, /* pushComments = */ false, /* startIndex = */ 0);
}

/** Called when one of the sorting icons is picked. It changes the sort direction, if necessary */
function commentSort(sortDirection) {
  // Only change if the sorting direction has changed
  if (sortDirection !== commentsSort) {
    // Remove the ID (removes coloring) from the current sort
    document.getElementById(COMMENTS_SORT_ICON).id = '';
    commentsSort = sortDirection;

    if (sortDirection === COMMENTS_SORT_NEWEST) {
      // Add the ID (color) to the icon corresponding to newest
      document.getElementsByClassName(COMMENTS_ICON_NEWEST)[0].id = COMMENTS_SORT_ICON;
      reverseComments()
    } else if (sortDirection === COMMENTS_SORT_OLDEST) {
      // Add the ID (color) to the icon corresponding to oldest
      document.getElementsByClassName(COMMENTS_ICON_OLDEST)[0].id = COMMENTS_SORT_ICON;
      reverseComments();
    }
  }
}

/** Retrieves comments from the server and places them on the DOM, clearing existing comments */
function retrieveComments() {
  deleteChildren(COMMENTS_CONTAINER);
  pageComments = [];
  let numComments;

  // Construct a query string with the appropriate number of comments being retrieved
  fetch(`${COMMENTS_URL}?${NUM_COMMENTS_QUERY}=` +
    `${retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION)}`).then(
    /* Convert from response stream */r => {
        // Get the total number of stored comments
        numComments = r.headers.get(TOTAL_NUMBER_HEADER);
        return r.json();
      }).then(comments => {
        addComments(comments);
      });
}

/** Prepares the given comment ID for deletion */
function prepareDelete(id) {
  console.log('delete ' + id);
}

// Add functions to window so onclick works in HTML
window.commentSort = commentSort;
window.retrieveComments = retrieveComments;
window.prepareDelete = prepareDelete;
