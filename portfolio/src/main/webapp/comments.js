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
/** Class of the icon that indicates which direction to sort */
const COMMENTS_SORT_ICON = 'icon-selected';
/** Icon corresponding to having the newest comment on top */
const COMMENTS_ICON_NEWEST = 'comment-newest';
/** Icon corresponding to having the oldest comment on top */
const COMMENTS_ICON_OLDEST = 'comment-oldest';
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
/** Class of the delete icon when inactive (No comments to delete) */
const DELETE_INACTIVE = 'trash-inactive';
/** Class of the delete icon when active (Comments to delete) */
const DELETE_ACTIVE = 'trash-active';
/** ID of the textbox where filter text goes */
const COMMENT_FILTER_INPUT = 'comment-filter';
/** Code corresponding to the enter key in a key event */
const ENTER_CODE = 13;
/** Query string for filtering comments */
const FILTER_QUERY = 'filter';
/** Class applied to an element to have it rotate */
const ROTATE_CLASS = 'rotate-animated';
/** The rotation animation takes 1 second */
const ROTATE_TIME = 1000;
/** ID of the comment refresh button */
const REFRESH_BUTTON = 'comment-refresh-button';
/** ID of the button clicked when a user wants to submit a comment */
const CREATE_COMMENT_BUTTON = 'comment-submit';
/** Field containing the name of the person who authored the comment */
const COMMENT_NAME_FIELD = 'comment-name';
/** Field containing the text of the comment to be POSTed */
const COMMENT_TEXT_FIELD = 'comment-text-input';
/** Class applied to an element to have it pop out */
const POP_CLASS = 'pop-animated';
/** The pop animation takes .75 seconds */
const POP_TIME = 750;
/** Class applied to an element to have it jitter upwards temporarily */
const TRANSFORM_UP_CLASS = 'transform-up-animated';
/** The transform animation takes .75 seconds */
const TRANSFORM_UP_TIME = 300;
/** Class applied to an element to have it jitter downwards temporarily */
const TRANSFORM_DOWN_CLASS = 'transform-down-animated';
/** The transform animation takes .75 seconds */
const TRANSFORM_DOWN_TIME = 300;
/** ID of the comment delete button */
const DELETE_BUTTON = 'comments-delete';
/** Class for the shake animation */
const SHAKE_CLASS = 'shake-animated';
/** Shake animation takes .5 seconds */
const SHAKE_TIME = 500;
/** ID of the file input */
const IMAGE_ATTACHMENT_BUTTON = 'comment-attachment';
/** URL for the image servlet */
const IMAGE_SERVLET_URL = '/image';

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
/** The URL for an image upload */
let imageUploadUrl;

// Perform necessary setup
document.addEventListener('DOMContentLoaded', () => {
  if (documentHasElement(COMMENTS_CONTAINER)) {
    // Retrieve comments data from the servlet and add to DOM
    refreshComments();
  }

  if (documentHasElement(NUM_COMMENTS_FIELD)) {
    // Refresh when the user presses enter
    document.getElementById(NUM_COMMENTS_FIELD).addEventListener('keyup', e => {
      if (e.keyCode === ENTER_CODE) {
        refreshComments();
      }
    });
  }

  if (documentHasElement(COMMENT_FILTER_INPUT)) {
    // Filter comments when the user presses enter
    document.getElementById(COMMENT_FILTER_INPUT).addEventListener('keyup', e => {
      if (e.keyCode === ENTER_CODE) {
        // When the user presses enter, reset all pagination and filter
        currCommentPage = 1;
        refreshComments(0, getCommentFilter());
      }
    });
  }

  if (documentHasElement(REFRESH_BUTTON)) {
    document.getElementById(REFRESH_BUTTON).addEventListener('click', e => {
      refreshComments();
    });
  }

  // Hook the submit button in the form to prevent needing a page refresh
  let commentForm = document.getElementById('comment-form');
  commentForm.addEventListener('submit', e => {
    e.preventDefault();
    new FormData(commentForm);
  });
  commentForm.addEventListener('formdata', e => {
    animateElement(CREATE_COMMENT_BUTTON, POP_CLASS, POP_TIME);
    submitComment(e.formData);

    refreshComments();
    // Clear field values and refocus on the name field
    document.getElementById(COMMENT_NAME_FIELD).value = '';
    document.getElementById(COMMENT_TEXT_FIELD).value = '';
    document.getElementById(IMAGE_ATTACHMENT_BUTTON).value = '';
    document.getElementById(COMMENT_NAME_FIELD).focus();
  });

  if(documentHasElement(IMAGE_ATTACHMENT_BUTTON)) {
    fetch(IMAGE_SERVLET_URL).then(response => response.text()).then(text => {
      imageUploadUrl = text;
    });
  }
});

/** Adds the animationClass to the given element for a given number of milliseconds */
function animateElement(id, animationClass, animationTime) {
  addClass(id, animationClass);
  setTimeout(() => removeClass(id, animationClass), animationTime);
}

/** Checks if the number of comments entered by the user is valid */
function validNumberComments() {
  let numComments = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  if (numComments.length > 0 && numComments > 0) {
    return true;
  }
  return false;
}

/** Reads fields from comments-create and POSTs it to server. */
function submitComment(formData) {
  let request = new XMLHttpRequest();
  request.open('POST', imageUploadUrl, false); // False makes this request synchronous
  request.send(formData);
}

/** Adds the given comment to the page. */
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
  appendElement(contentId, 'p', `<b>${comment.name}</b>\t` +
    `<span title="${date.toLocaleString()}">${timePassed(date)}</span>`);
  appendElement(contentId, 'p', comment.text);

  if(comment.imageUrl) {
    appendElement(contentId, 'span', `<img src="${comment.imageUrl}"/>`)
  }

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
    commentsSort = sortDirection;

    if (sortDirection === COMMENTS_SORT_NEWEST) {
      // Remove the coloring from the other icon
      removeClass(COMMENTS_ICON_OLDEST, COMMENTS_SORT_ICON);

      // Add the color to the icon corresponding to newest and animate
      addClass(COMMENTS_ICON_NEWEST, COMMENTS_SORT_ICON);
      animateElement(COMMENTS_ICON_NEWEST, TRANSFORM_UP_CLASS, TRANSFORM_UP_TIME);
    } else if (sortDirection === COMMENTS_SORT_OLDEST) {
      // Remove the coloring from the other icon
      removeClass(COMMENTS_ICON_NEWEST, COMMENTS_SORT_ICON);

      // Add the color to the icon corresponding to oldest and animate
      addClass(COMMENTS_ICON_OLDEST, COMMENTS_SORT_ICON);
      animateElement(COMMENTS_ICON_OLDEST, TRANSFORM_DOWN_CLASS, TRANSFORM_DOWN_TIME);
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
  let numEachPage;
  if (validNumberComments()) {
    numEachPage = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  } else {
    numEachPage = pageComments.length;
  }
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
  let numEachPage = pageComments.length;
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

/** Returns the filter contained in the filter text-box. Returns undefined if blank */
function getCommentFilter() {
  let property = retrieveProperty(COMMENT_FILTER_INPUT, TEXT_SELECTION);
  if (property.length === 0) {
    return undefined;
  }
  return property;
}

/** Retrieves comments from the server and places them on the DOM, clearing existing comments 
 *  By default, starts reading from the appropriate comment page and filters based on 
 *  the contents of the search box. */
function refreshComments(from = getPaginationStartIndex(), filter = getCommentFilter()) {
  let ascending = commentsSort === COMMENTS_SORT_NEWEST ? true : false;
  // Query string built from filter. Blank if no filter
  let filterQuery;

  animateElement(REFRESH_BUTTON, ROTATE_CLASS, ROTATE_TIME);

  // Reset to first page if the currently selected page is out of bounds
  if (getNumberCommentPages() < currCommentPage) {
    currCommentPage = 1;
    from = 0;
  }

  if (filter !== undefined) {
    filterQuery = '&' + FILTER_QUERY + '=' + encodeURI(filter);
  } else {
    filterQuery = '';
  }

  // Contains all the query strings for the GET request
  let queryString = `?${SORT_QUERY_STRING}=${ascending}&${PAGINATION_START}=${from}${filterQuery}`

  let numComments = retrieveProperty(NUM_COMMENTS_FIELD, TEXT_SELECTION);
  if (validNumberComments()) {
    queryString += '&' + NUM_COMMENTS_QUERY + '=' + numComments;
  } else {
    // Shake the element to signify invalid value and don't add to query
    animateElement(NUM_COMMENTS_FIELD, SHAKE_CLASS, SHAKE_TIME);
  }

  // Construct a query string with the appropriate number of comments being retrieved
  fetch(`${COMMENTS_URL}${queryString}`).then(
    /* Convert from response stream */r => {
      // Get the total number of stored comments
      totalNumComments = r.headers.get(TOTAL_NUMBER_HEADER);
      return r.json();
    }).then(comments => {
      deleteChildren(COMMENTS_CONTAINER);
      pageComments = [];
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

    // Remove color of delete button if none are to be deleted
    if (commentsToDelete.size === 0) {
      removeClass(DELETE_BUTTON, DELETE_ACTIVE);
      addClass(DELETE_BUTTON, DELETE_INACTIVE);
    }
  } else {
    // Add to deletion
    commentsToDelete.add(id);
    addClass(COMMENT_CONTAINER_PREFIX + id, COMMENT_DELETE_CLASS);

    // Add styling to delete button if this is the first to be added
    if (commentsToDelete.size === 1) {
      removeClass(DELETE_BUTTON, DELETE_INACTIVE);
      addClass(DELETE_BUTTON, DELETE_ACTIVE)
    }
  }
}

/** Deletes all the comments that have been signaled for deletion */
function deleteComments() {
  if (commentsToDelete.size > 0) {
    let deleteString = '?';

    animateElement(DELETE_BUTTON, POP_CLASS, POP_TIME);

    Array.from(commentsToDelete).forEach((id, index) => {
      if (index > 0) {
        deleteString += '&';
      }
      deleteString += DELETE_QUERY_STRING + '=' + id;
    });

    fetch(`${COMMENTS_URL}${deleteString}`, {
      method: 'DELETE',
    }).then(r => {
      refreshComments();
      // Clear the deletion list and remove styling from the trash can
      removeClass(DELETE_BUTTON, DELETE_ACTIVE);
      addClass(DELETE_BUTTON, DELETE_INACTIVE);
      commentsToDelete = new Set();
    });
  }
}

// Add functions to window so onclick works in HTML
window.commentSort = commentSort;
window.refreshComments = refreshComments;
window.prepareDelete = prepareDelete;
window.deleteComments = deleteComments;
