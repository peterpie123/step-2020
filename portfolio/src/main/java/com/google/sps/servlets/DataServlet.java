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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.io.IOException;
import java.util.Collections;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.gson.Gson;
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

  private static List<Comment> comments;

  @Override
  public void init() {
    // Retrieve stored comments
    comments = Comment.loadComments();
  }

  private static String stringifyComments(int numComments, boolean sortAscending) {
    Gson gson = new Gson();
    List<Comment> send = new ArrayList<>(numComments);

    if(sortAscending) {
      // Send the first numComments entries
      for(int i = 0; i < numComments && i < comments.size(); i++) {
        send.add(comments.get(i));
      }
    } else {
      // Send the last numComments entries 
      if(numComments > comments.size()) {
        for(int i = comments.size() - 1; i >= 0; i--) {
          send.add(comments.get(i));
        }
      } else {
        for(int i = comments.size() - 1; i >= comments.size() - numComments; i--) {
            send.add(comments.get(i));
        }
      }
    }

    return gson.toJson(send);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int numComments = DEFAULT_COMMENT_NUM;
    boolean sortAscending = true;

    if(request.getParameter(NUM_COMMENTS_QUERY) != null) {
      numComments = Integer.parseInt(request.getParameter(NUM_COMMENTS_QUERY));
      // Revert to default if user specified an invalid number
      if(numComments <= 0) {
        numComments = DEFAULT_COMMENT_NUM;
      }
    }
    if(request.getParameter(SORT_ASCENDING_QUERY) != null) {
      sortAscending = Boolean.parseBoolean(request.getParameter(SORT_ASCENDING_QUERY));
    }

    response.setContentType("application/json;");
    response.getWriter().println(stringifyComments(numComments, sortAscending));
    // Send the total number of comments
    response.addIntHeader(TOTAL_NUMBER_HEADER, comments.size());
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Create and add the new comment
    Comment comment = Comment.fromHttpRequest(request);
    comments.add(comment);

    Collections.sort(comments);
    
    // Redirect back to the home page.
    response.sendRedirect("/index.html");
  }

  /** Deletes the given comment from the comments list and data storage */
  private static void deleteComment(int id) {
    for(int i = 0; i < comments.size(); i++) {
      Comment comment = comments.get(i);
      if(comment.getId() == id) {
        // Remove the comment from persistent storage and the comments list
        comment.removePersistent();
        comments.remove(i);
        break;
      }
    }
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String[] toDelete = request.getParameterValues(DELETE_PARAMETER);
    for(int i = 0; i < toDelete.length; i++) {
        try {
          int id = Integer.parseInt(toDelete[i]);
          deleteComment(id);
        } catch(NumberFormatException e) {
          // Ignore, as should never happen
        }
    }
  }
}
