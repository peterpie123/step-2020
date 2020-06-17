// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.fakeHandlers;

import com.google.sps.data.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fake database for storing comments */
public class FakeCommentStore {
  private static FakeCommentStore instance;

  private List<Comment> comments;

  private FakeCommentStore() {
    comments = new ArrayList<>();
  }

  public static FakeCommentStore getInstance() {
    if (instance == null) {
      instance = new FakeCommentStore();
    }
    return instance;
  }

  public void addComment(Comment c) {
    comments.add(c);
  }

  public Optional<Comment> getCommentById(long id) {
    return comments.stream().filter(c -> c.getId() == id).findFirst();
  }
}
