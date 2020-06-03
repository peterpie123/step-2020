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
import com.google.sps.data.CommentPersistHelper;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  /** Default number of comments to send */
  private static final int DEFAULT_COMMENT_NUM = 25;
  /** Query string which contains the number of comments to send */
  private static final String NUM_COMMENTS_QUERY = "num-comments";
  /** Header containing the total number of comments stored */
  private static final String TOTAL_NUMBER_HEADER = "num-comments";
  /** Query string that indicates whether to sort ascending or descending */
  private static final String SORT_ASCENDING_QUERY = "sort-ascending";
  /** Parameter which contains the ID's of the comments to delete */
  private static final String DELETE_PARAMETER = "delete";
  /** Query string that tells which comment to start at */
  private static final String PAGINATION_START = "pagination";

  private static CommentPersistHelper comments;

  @Override
  public void init() {
    comments = new CommentPersistHelper();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int numComments = DEFAULT_COMMENT_NUM;
    boolean sortAscending = true;
    // Where to start retrieving comments for pagination purposes
    int from = 0;

    if(request.getParameter(NUM_COMMENTS_QUERY) != null) {
      try {
        numComments = Integer.parseInt(request.getParameter(NUM_COMMENTS_QUERY));
      } catch(NumberFormatException e) {
        // Ignore and use default
      }
      // Revert to default if user specified an invalid number
      if(numComments <= 0) {
        numComments = DEFAULT_COMMENT_NUM;
      }
    }
    if(request.getParameter(SORT_ASCENDING_QUERY) != null) {
      sortAscending = Boolean.parseBoolean(request.getParameter(SORT_ASCENDING_QUERY));
    }
    if(request.getParameter(PAGINATION_START) != null) {
      try {
        from = Integer.parseInt(request.getParameter(PAGINATION_START));
      } catch(NumberFormatException e) {
        // Ignore and use default
      }
      if(from < 0) {
        from = 0;
      }
    }

    response.setContentType("application/json;");
    response.getWriter().println(comments.stringifyComments(numComments, sortAscending, from));
    // Send the total number of comments
    response.addIntHeader(TOTAL_NUMBER_HEADER, comments.getNumComments());
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Create and add the new comment
    comments.addComment(request);
    
    // Redirect back to the home page.
    response.sendRedirect("/index.html");
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] toDelete = request.getParameterValues(DELETE_PARAMETER);
    Arrays.stream(toDelete).forEach(idStr -> {
      try {
        int id = Integer.parseInt(idStr);
        comments.deleteComment(id);
      } catch(NumberFormatException e) {
        // Ignore, as should never happen
      }
    });
  }
}
