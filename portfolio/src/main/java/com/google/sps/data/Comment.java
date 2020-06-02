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

/** Represents a single comment from a user. */
public class Comment implements Comparable<Comment> {
  final private String text;
  final private String name;
  final private long timestamp;

  public Comment(String text, String name) {
    this.text = text;
    this.name = name;
    this.timestamp = System.currentTimeMillis();
  }

  /** Create a calendar with a posted date of given milliseconds from the epoch */
  public Comment(String text, String name, long timestamp) {
    this.text = text;
    this.name = name;
    this.timestamp = timestamp;
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
