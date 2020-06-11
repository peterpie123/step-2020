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

package com.google.sps.servlets;

import java.util.Optional;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.sps.data.Comment;
import com.google.sps.data.CommentAnalysis;
import com.google.sps.data.CommentPersistHelper;

/**
 * Analyzes the posted image with GCloud vision. Will eventually also analyze the comment sentiment.
 */
@WebServlet("/analyze")
public class AnalyzeServlet extends HttpServlet {

  /** Query string for the comment to be analyzed. */
  private static final String COMMENT_ID = "id";

  private static CommentPersistHelper commentStore;

  @Override
  public void init() {
    commentStore = CommentPersistHelper.getInstance();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    CommentAnalysis analysis = new CommentAnalysis();

    try {
      long commentId = Long.parseLong(request.getParameter(COMMENT_ID));
      Optional<Comment> comment = commentStore.getCommentById(commentId);
      // Avoid using ifPresent since lambdas don't play nice with exceptions
      if(comment.isPresent()) {
        analysis.analyzeImage(comment.get());
        response.getWriter().println(analysis.toString());
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          request.getParameter(COMMENT_ID) + " is an invalid Comment ID. Aborting analysis...", e);
    }
  }
}
