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

// Import utlity functions, unfortunately cannot be line-wrapped
import { documentHasElement, appendElement, deleteChildren, retrieveProperty, removeClass, addClass, setId, timePassed } from './script.js';

/** ID of the comments container */
const COMMENTS_CONTAINER = 'comments-container';
/** URL for the comments servlet */
const COMMENTS_URL = '/data';
/** Indicates that comments should be sorted with the newest on top */
const COMMENTS_SORT_NEWEST = 1;
/** Indicates that comments should be sorted with the oldest on top */
const COMMENTS_SORT_OLDEST = -1;
/** ID of the icon that indicates which direction to sort */
const COMMENTS_SORT_ICON = 'icon-selected';
/** Icon corresponding to having the newest comment on top */
const COMMENTS_ICON_NEWEST = 'fa-chevron-circle-up';
/** Icon corresponding to having the oldest comment on top */
const COMMENTS_ICON_OLDEST = 'fa-chevron-circle-down';
/** Query string for indicating how many comments to retrieve */
const NUM_COMMENTS_QUERY = 'num-comments';
/** Property for what the user typed/selected in an input field */
const TEXT_SELECTION = 'value';
/** ID of the field where the user selects how many comments to retrieve */
const NUM_COMMENTS_FIELD = 'num-comments';
/** Header containing the total number of comments stored */
const TOTAL_NUMBER_HEADER = "num-comments";
/** Prefix for the ID of the div containing an entire comment. Postfixed by the comment ID */
const COMMENT_CONTAINER_PREFIX = 'comment-';
/** Class on the container for each individual comment */
const COMMENT_CLASS = 'comment';
/** Class applied to a comment when it's marked for deletion  */
const COMMENT_DELETE_CLASS = 'comment-delete';
/** Class for the clickable button for a comment */
const COMMENT_SELECT_CLASS = 'comment-toggle';
/** Class which indicates the text part of a comment */
const COMMENT_TEXT_CLASS = 'comment-text';
/** Query string used to mark which comments will be deleted */
const DELETE_QUERY_STRING = 'delete';
/** Query string used to note whether comments are being sorted ascending or descending */
const SORT_QUERY_STRING = 'sort-ascending';
/** ID of the pagination container for comments */
const PAGINATION_CONTAINER = 'comment-pagination-container';
/** Query string which indicates which comment to start at for pagination purposes */
const PAGINATION_START = 'pagination';
/** ID of the pagination button that's selected */
const PAGINATION_SELECTED = 'pagination-selected';
/** Class name of the pagination buttons */
const PAGINATION_SELECT = 'pagination-select';
/** ID of the delete icon when inactive (No comments to delete) */
const DELETE_INACTIVE = 'trash-inactive';
/** ID of the delete icon when active (Comments to delete) */
const DELETE_ACTIVE = 'trash-active';

/** List of comments currently on the page */
let pageComments = [];
/** How the comments are currently sorted */
let commentsSort = COMMENTS_SORT_NEWEST;
/** Comments currently selected for deletion */
let commentsToDelete = new Set();
/** The total number of comments on the server. Used for pagination */
let totalNumComments;
/** The current page of comments that's on */
let currCommentPage = 1;

// Perform necessary setup
document.addEventListener('DOMContentLoaded', () => {
  if (documentHasElement(COMMENTS_CONTAINER)) {
    // Retrieve comments data from the servlet and add to DOM
    refreshComments();
  }
});

/** Adds the given comment to the page with the given ID. */
function addSingleComment(comment) {
  let containerId = COMMENT_CONTAINER_PREFIX + comment.id;

  appendElement(COMMENTS_CONTAINER, 'div', '', /* id = */ containerId,
                /* onClick = */ undefined, /* class = */ COMMENT_CLASS);
  // Format the time nicely, by starting at the epoch and setting the milliseconds
  let date = new Date(0);
  date.setUTCMilliseconds(comment.timestamp);

  // ID for the container which houses the information for a comment
  let contentId = containerId + '-content';
  appendElement(containerId, 'div', '', contentId, undefined, COMMENT_TEXT_CLASS);
  // Add the name, time, and content
  appendElement(contentId, 'p', `<b>${comment.name}</b>\t${timePassed(date)}`);
  appendElement(contentId, 'p', comment.text);

  // Add the element that will toggle deleting this comment
  appendElement(containerId, 'div', '', undefined, () => prepareDelete(comment.id),
    COMMENT_SELECT_CLASS);
}

/**Adds the given list of comments to the page, and only adds them to the pageComments
 * array if pushComments is true. startIndex indicates where to start counting up comment ID's */
function addComments(comments, pushComments = true) {
  comments.forEach(comment => {
    addSingleComment(comment);

    if (pushComments) {
      pageComments.push(comment);
    }
    if (commentsToDelete.has(comment.id)) {
      addClass(COMMENT_CONTAINER_PREFIX + comment.id, COMMENT_DELETE_CLASS);
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
    } else if (sortDirection === COMMENTS_SORT_OLDEST) {
      // Add the ID (color) to the icon corresponding to oldest
      document.getElementsByClassName(COMMENTS_ICON_OLDEST)[0].id = COMMENTS_SORT_ICON;
    }

    // Finally, re-update the comments section
    refreshComments();
  }
}

/** Calculates the start index for comment pagination based on comments per page
 *  and the current page. */
function getPaginationStartIndex() {
  let numEachPage = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  return (currCommentPage - 1) * numEachPage;
}

/** Returns the total number of pages of comments that should be listed */
function getNumberCommentPages() {
  let numEachPage = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  return Math.ceil(totalNumComments / numEachPage);
}

/** Navigates the comments list to the given page number */
function paginate(num) {
  currCommentPage = num;
  refreshComments(getPaginationStartIndex());
}

/** Adds the pagination section */
function addPagination() {
  // Number of comments on each page
  let numEachPage = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  let numPages = getNumberCommentPages();

  // Only add pagination if we need to
  if (numPages > 1) {
    appendElement(COMMENTS_CONTAINER, 'div', '', PAGINATION_CONTAINER);

    // Add a pagination button for each page of comments
    for (let i = 1; i <= numPages; i++) {
      // If this is the current comments page, add special styling
      let buttonId = undefined;
      if (i === currCommentPage) {
        buttonId = PAGINATION_SELECTED;
      }

      appendElement(PAGINATION_CONTAINER, 'span', `<input type="button" value="${i}"/>`,
        buttonId, /* onclick */() => paginate(i), PAGINATION_SELECT);

    }
  }
}

/** Retrieves comments from the server and places them on the DOM, clearing existing comments 
 *  By default, starts reading from the appropriate comment page */
function refreshComments(from = getPaginationStartIndex()) {
  deleteChildren(COMMENTS_CONTAINER);
  pageComments = [];
  let ascending = commentsSort === COMMENTS_SORT_NEWEST ? true : false;

  // Reset to first page if the currently selected page is out of bounds
  if(getNumberCommentPages() < currCommentPage) {
    currCommentPage = 1;
    from = 0;
  }

  // Construct a query string with the appropriate number of comments being retrieved
  fetch(`${COMMENTS_URL}?${NUM_COMMENTS_QUERY}=` +
    `${retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION)}&` +
    `${SORT_QUERY_STRING}=${ascending}&${PAGINATION_START}=${from}`).then(
    /* Convert from response stream */r => {
        // Get the total number of stored comments
        totalNumComments = r.headers.get(TOTAL_NUMBER_HEADER);
        return r.json();
      }).then(comments => {
        addComments(comments);
        addPagination();
      });
}

/** Prepares the given comment ID for deletion */
function prepareDelete(id) {
  if (commentsToDelete.has(id)) {
    // Remove from deletion
    commentsToDelete.delete(id);
    removeClass(COMMENT_CONTAINER_PREFIX + id, COMMENT_DELETE_CLASS);

    // Remove color of trash can if none are to be deleted
    if (commentsToDelete.size === 0) {
      setId(DELETE_ACTIVE, DELETE_INACTIVE);
    }
  } else {
    // Add to deletion
    commentsToDelete.add(id);
    addClass(COMMENT_CONTAINER_PREFIX + id, COMMENT_DELETE_CLASS);

    // Add color fo trash can if this is the first to be added
    if (commentsToDelete.size === 1) {
      setId(DELETE_INACTIVE, DELETE_ACTIVE);
    }
  }
}

/** Deletes all the comments that have been signaled for deletion */
function deleteComments() {
  if (commentsToDelete.size > 0) {
    let deleteString = '?';

    Array.from(commentsToDelete).forEach((id, index) => {
      if (index > 0) {
        deleteString += '&';
      }
      deleteString += DELETE_QUERY_STRING + '=' + id;
    });

    fetch(`${COMMENTS_URL}${deleteString}`, {
      method: 'DELETE',
    }).then(r => refreshComments());
  }
}

// Add functions to window so onclick works in HTML
window.commentSort = commentSort;
window.refreshComments = refreshComments;
window.prepareDelete = prepareDelete;
window.deleteComments = deleteComments;
