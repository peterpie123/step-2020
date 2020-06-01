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
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private static final String COMMENT_NAME = "name";
  private static final String COMMENT_TEXT = "comment";
  private static final String COMMENT_TIMESTAMP = "datePosted";

  private static List<Comment> comments = new ArrayList<>();

  @Override
  public void init() {
    // Retrieve stored comments
    Query query = new Query("Comment").addSort(COMMENT_TIMESTAMP, SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    for(Entity entity : results.asIterable()) {
      String text = (String) entity.getProperty(COMMENT_TEXT);
      String name = (String) entity.getProperty(COMMENT_NAME);
      long time = (long) entity.getProperty(COMMENT_TIMESTAMP);

      comments.add(new Comment(text, name, time));
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Create and add the new comment
    Comment comment = new Comment(request.getParameter(COMMENT_TEXT), 
                                  request.getParameter(COMMENT_NAME));
    comments.add(comment);

    // Store the comment so it persists
    Entity entity = new Entity("Comment");
    entity.setProperty(COMMENT_TEXT, comment.getText());
    entity.setProperty(COMMENT_NAME, comment.getName());
    entity.setProperty(COMMENT_TIMESTAMP, comment.getTimestamp());
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(entity);
    
    // Redirect back to the home page.
    response.sendRedirect("/index.html");
  }
}
