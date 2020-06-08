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

import java.lang.Comparable;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import org.apache.commons.lang3.StringUtils;

/** Represents a single comment from a user. */
public class Comment implements Comparable<Comment> {
  /** Key for the name of the commenter */
  static final String COMMENT_NAME = "name";
  /** Key for the comment contents */
  static final String COMMENT_TEXT = "comment";
  /** Key for the timestamp */
  static final String COMMENT_TIMESTAMP = "timestamp";
  /** URL for the attached picture */
  static final String COMMENT_PICTURE_URL = "picture";

  /** Internal counter used for dynamically assigning an ID */
  private static int commentCount = 0;

  /** Text of the comment */
  private final String text;
  /** Name of the person who submitted the comment */
  private final String name;
  /** Time comment was created, in milliseconds since epoch (UTC) */
  private final long timestamp;
  /**
   * The ID of the key associated with the entity that keeps this comment in
   * persistent storage.
   */
  private final long id;
  /** The key that keeps this comment in persistent storage */
  private final Key key;
  /** URL for an attached image. null if there is none */
  private final String imageUrl;

  /** Create a comment with a posted date of given milliseconds from the epoch. */
  public Comment(String text, String name, Key key, long timestamp, String imageUrl) {
    this.text = text;
    this.name = name;
    this.timestamp = timestamp;
    this.key = key;
    this.id = key.getId();
    this.imageUrl = imageUrl;
  }

  /** Create a comment with the current time as creation date */
  public Comment(String text, String name, Key key) {
    this(text, name, key, System.currentTimeMillis(), null);
  }

  /** Create a new Comment from an Entity representing a comment */
  public static Comment fromEntity(Entity entity) {
    String text = (String) entity.getProperty(COMMENT_TEXT);
    String name = (String) entity.getProperty(COMMENT_NAME);
    long time = (long) entity.getProperty(COMMENT_TIMESTAMP);
    // URL is optional
    String url = null;
    if(entity.hasProperty(COMMENT_PICTURE_URL)) {
      url = (String) entity.getProperty(COMMENT_PICTURE_URL);
    }
    return new Comment(text, name, entity.getKey(), time, url);
  }

  /**
   * Returns true if text or name (inclusive) contains the filter string, ignoring
   * case
   */
  public boolean contains(String filter) {
    return StringUtils.containsIgnoreCase(text, filter) || StringUtils.containsIgnoreCase(name, filter);
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

  public long getId() {
    return id;
  }

  public Key getKey() {
    return key;
  }

  @Override
  public int compareTo(Comment other) {
    int compare = (int) (other.timestamp - timestamp);

    // If timestamps are equal, sort by name field
    if (compare == 0) {
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
    if (!(other instanceof Comment)) {
      return false;
    }
    Comment c = (Comment) other;

    if (this == c) {
      return true;
    }

    if (!(name.equals(c.name) && text.equals(c.text) && timestamp == c.timestamp)) {
      return false;
    }
    return true;
  }
}
