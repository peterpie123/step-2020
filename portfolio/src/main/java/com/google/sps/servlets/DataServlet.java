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

  private static CommentPersistHelper comments;

  @Override
  public void init() {
    comments = new CommentPersistHelper();
    comments.loadComments();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int numberComments = DEFAULT_COMMENT_COUNT;
    CommentPersistHelper.SortMethod sort = CommentPersistHelper.SortMethod.ASCENDING;

    if(request.getParameter(NUMBER_COMMENTS_QUERY) != null) {
      numberComments = Integer.parseInt(request.getParameter(NUMBER_COMMENTS_QUERY));
      // Revert to default if user specified an invalid number
      if(numberComments <= 0) {
        numberComments = DEFAULT_COMMENT_COUNT;
      }
    }
    if(request.getParameter(SORT_ASCENDING_QUERY) != null) {
      boolean sortAscending = Boolean.parseBoolean(request.getParameter(SORT_ASCENDING_QUERY));
      if(!sortAscending) {
        sort = CommentPersistHelper.SortMethod.DESCENDING;
      }
    }

    response.setContentType("application/json;");
    response.getWriter().println(comments.stringifyComments(numberComments, sort));
    // Send the total number of comments
    response.addIntHeader(TOTAL_NUMBER_HEADER, comments.getNumberComments());
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
        long id = Long.parseLong(idStr);
        comments.deleteComment(id);
      } catch(NumberFormatException e) {
        throw new IllegalArgumentException(
          idStr + " is not a valid comment ID. Aborting comment deletion...");
      }
    });
  }
}
