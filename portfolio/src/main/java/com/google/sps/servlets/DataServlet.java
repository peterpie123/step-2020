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

package com.google.sps.servlets;

import java.io.IOException;
import java.util.Arrays;
import java.lang.IllegalArgumentException;
import com.google.sps.data.CommentPersistHelper;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  /** Default number of comments to send */
  private static final int DEFAULT_COMMENT_COUNT = 25;
  /** Query string which contains the number of comments to send */
  private static final String NUMBER_COMMENTS_QUERY = "num-comments";
  /** Header containing the total number of comments stored */
  private static final String TOTAL_NUMBER_HEADER = "num-comments";
  /** Query string that indicates whether to sort ascending or descending */
  private static final String SORT_ASCENDING_QUERY = "sort-ascending";
  /** Parameter which contains the ID's of the comments to delete */
  private static final String DELETE_PARAMETER = "delete";
  /** Query string that tells which comment to start at */
  private static final String PAGINATION_START = "pagination";
  /** Default sorting method for retrieving comments */
  private static final CommentPersistHelper.SortMethod DEFAULT_SORT = CommentPersistHelper.SortMethod.ASCENDING;

  private static CommentPersistHelper commentStore;

  @Override
  public void init() {
    commentStore = new CommentPersistHelper();
    commentStore.loadComments();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int paginationFrom;
    // The number of comments to send
    int commentsToSend;
    CommentPersistHelper.SortMethod sort;

    try {
      commentsToSend = Integer.parseInt(request.getParameter(NUMBER_COMMENTS_QUERY));
      // Revert to default if user specified an invalid number
      if(commentsToSend <= 0) {
        commentsToSend = DEFAULT_COMMENT_COUNT;
      }
    } catch(NullPointerException e) {
      // Number of comments is not included, so use default
      commentsToSend = DEFAULT_COMMENT_COUNT;
    } catch(NumberFormatException e) {
      // Number of comments is malformed, so complain about it!
      throw new IllegalArgumentException(request.getParameter(NUMBER_COMMENTS_QUERY) +
                " is an invalid number of comments. Aborting GET...");
    }
    try {
      boolean sortAscending = Boolean.parseBoolean(request.getParameter(SORT_ASCENDING_QUERY));
      if(sortAscending) {
        sort = CommentPersistHelper.SortMethod.ASCENDING;
      } else {
        sort = CommentPersistHelper.SortMethod.DESCENDING;
      }
    } catch(NullPointerException e) {
      // Sort method is not included, so use default
      sort = DEFAULT_SORT;
    }
    
    try {
      paginationFrom = Integer.parseInt(request.getParameter(PAGINATION_START));
      if(paginationFrom < 0) {
        paginationFrom = 0;
      }
    } catch(NullPointerException e) {
      // Pagination isn't included, so just start at 0
      paginationFrom = 0;
    } catch(NumberFormatException e) {
      // Pagination is malformed, so kick up a fuss
      throw new IllegalArgumentException(request.getParameter(PAGINATION_START) +
                " is an invalid pagination start. Aborting GET...");
    }

    response.setContentType("application/json;");
    response.getWriter().println(commentStore.stringifyComments(commentsToSend, sort, paginationFrom));
    // Send the total number of comments
    response.addIntHeader(TOTAL_NUMBER_HEADER, commentStore.getNumberComments());
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Create and add the new comment
    commentStore.addComment(request);
    
    // Redirect back to the home page.
    response.sendRedirect("/index.html");
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] toDelete = request.getParameterValues(DELETE_PARAMETER);
    Arrays.stream(toDelete).forEach(idStr -> {
      try {
        long id = Long.parseLong(idStr);
        commentStore.deleteComment(id);
      } catch(NumberFormatException e) {
        throw new IllegalArgumentException(
          idStr + " is not a valid comment ID. Aborting comment deletion...");
      }
    });
  }
}
