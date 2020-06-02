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

import java.util.Date;
import java.text.DateFormat;
import java.lang.Comparable;
import com.google.appengine.api.datastore.Entity;
import javax.servlet.http.HttpServletRequest;

/** Represents a single comment from a user. */
public class Comment implements Comparable<Comment> {
  public static final String COMMENT_NAME = "name";
  public static final String COMMENT_TEXT = "comment";
  public static final String COMMENT_TIMESTAMP = "timestamp";

  private final String text;
  private final String name;
  private final long timestamp;

  private Comment(String text, String name) {
    this(text, name, System.currentTimeMillis());
  }

  /** Create a comment with a posted date of given milliseconds from the epoch */
  private Comment(String text, String name, long timestamp) {
    this.text = text;
    this.name = name;
    this.timestamp = timestamp;
  }

  /** Create a new Comment from an Entity representing a comment */
  public static Comment fromEntity(Entity entity) {
    String text = (String) entity.getProperty(COMMENT_TEXT);
    String name = (String) entity.getProperty(COMMENT_NAME);
    long time = (long) entity.getProperty(COMMENT_TIMESTAMP);
    return new Comment(text, name, time);
  }

  /** Create a new Comment from an incoming HTTP POST */
  public static Comment fromHttpRequest(HttpServletRequest request) {
    return new Comment(request.getParameter(COMMENT_TEXT), request.getParameter(COMMENT_NAME));
  }

  /** Converts this comment to an entity */
  public Entity toEntity() {
    Entity entity = new Entity("Comment");
    entity.setProperty(COMMENT_TEXT, getText());
    entity.setProperty(COMMENT_NAME, getName());
    entity.setProperty(COMMENT_TIMESTAMP, getTimestamp());
    return entity;
  }

  public String getText() {
    return text;
  }

  public String getName() {
    return name;
  }

  /** Return milliseconds since the epoch, in UTC */
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public int compareTo(Comment other) {
    int compare = (int) (other.timestamp - timestamp);
    
    // If timestamps are equal, sort by name field
    if(compare == 0) {
      return name.compareTo(other.name);
    }
    return compare;
  }

  @Override
  public int hashCode() {
    int hash = 13;
    hash = 31 * hash + text.hashCode();
    hash = 31 * hash + name.hashCode();
    hash = 31 * hash + ((Long) timestamp).hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if(!(other instanceof Comment)) {
      return false;
    }
    Comment c = (Comment) other;

    if(this == c) {
      return true;
    }

    if(!(name.equals(c.name) && text.equals(c.text) && timestamp == c.timestamp)) {
      return false;
    }
    return true;
  }
}
