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

package com.google.sps.data;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import com.google.gson.Gson;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;

/** Keeps track of persisted comments with ability to add/remove comments */
public class CommentPersistHelper {
  private final DatastoreService datastore;
  private final List<Comment> comments;

  public CommentPersistHelper() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    comments = new ArrayList<>();
    loadComments();
  }

  /** Loads comments from persist storage and adds to the comments list */
  public void loadComments() {
    Query query = new Query("Comment").addSort(Comment.COMMENT_TIMESTAMP, SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);
    results.asList(FetchOptions.Builder.withDefaults()).forEach(entity -> {
      comments.add(Comment.fromEntity(entity));
    });
  }

  /** Adds a new comment from the given HTTP POST */
  public void addComment(HttpServletRequest request) {
    Entity entity = new Entity("Comment");
    entity.setProperty(Comment.COMMENT_TEXT, request.getParameter(Comment.COMMENT_TEXT));
    entity.setProperty(Comment.COMMENT_NAME, request.getParameter(Comment.COMMENT_NAME));
    entity.setProperty(Comment.COMMENT_TIMESTAMP, System.currentTimeMillis());

    comments.add(Comment.fromEntity(entity));
    Collections.sort(comments);

    // Store the comment so it persists
    datastore.put(entity);
  }

  /** Deletes the given comment permanently. */
  public void deleteComment(int id) {
    for(int i = 0; i < comments.size(); i++) {
      Comment comment = comments.get(i);
      if(comment.getId() == id) {
        // Remove the comment from persistent storage and the comments list
        comments.remove(i);
        datastore.delete(comment.getKey());
        break;
      }
    }
  }

  public String stringifyComments(int numComments, boolean sortAscending) {
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

  public int getNumComments() {
    return comments.size();
  }
}