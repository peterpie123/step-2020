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
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import com.google.gson.Gson;
import com.google.common.collect.Lists;
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
  public enum SortMethod { ASCENDING, DESCENDING }

  private final DatastoreService datastore;
  private final List<Comment> comments;

  public CommentPersistHelper() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    comments = new ArrayList<>();
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
    // Store the comment so it persists
    datastore.put(entity);

    // Insert new comment at the beginning to preserve sort
    comments.add(0, Comment.fromEntity(entity));
  }

  /** Deletes the given comment permanently. */
  public void deleteComment(long id) {
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

  /** Returns only comments with either name or content containing filter */
  private List<Comment> filterList(String filter) {
    if(filter == null) {
      // Don't filter
      return comments;
    }

    // Return comments that contain the filter string
    return comments.stream().filter(c -> c.contains(filter)).collect(Collectors.toList());
  }

  /** Stringifies the comments in the desired order, including pagination and filtering */
  public String stringifyComments(int numberComments, SortMethod sort, 
                                  int paginationFrom, String filter) {
    Gson gson = new Gson();
    List<Comment> filteredList = filterList(filter);
    List<Comment> send;
    // List that will either be reversed or not, depending on the sort
    List<Comment> readList = null;

    if(paginationFrom < 0) {
      paginationFrom = 0;
    }
    if(paginationFrom > filteredList.size()) {
      throw new IllegalArgumentException("Error: Cannot paginate starting at " + paginationFrom +
                " when there are only " + filteredList.size() + " comments!");
    }

    if(sort == SortMethod.ASCENDING) {
      readList = filteredList;
    } else if(sort == SortMethod.DESCENDING) {
      // Comments is already sorted, so just reverse
      readList = Lists.reverse(filteredList);
    }

    int paginationTo = numberComments + paginationFrom;
    // Make sure pagination doesn't go out of bounds
    if(paginationTo > filteredList.size()) {
      paginationTo = filteredList.size();
    }
    send = readList.subList(paginationFrom, paginationTo);

    return gson.toJson(send);
  }

  /** Returns the total number of comments that are stored */
  public int getNumberComments() {
    return comments.size();
  }

  /** Returns the total number of comments that correspond to the given filter */
  public int getNumberComments(String filter) {
    return filterList(filter).size();
  }
}
