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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.sps.data.Comment;
import com.google.sps.data.CommentAnalysis;
import com.google.sps.data.CommentPersistHelper;
import org.junit.Test;

public class AnalyzeServletTest {
  /** Test that a comment is analyzed when present */
  @Test
  public void testAnalyzePresent() throws IOException {
    long id = 1;

    CommentAnalysis analysis = mock(CommentAnalysis.class);
    Comment comment = mock(Comment.class);
    CommentPersistHelper helper = mock(CommentPersistHelper.class);
    AnalyzeServlet servlet = new AnalyzeServlet(helper);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    PrintWriter writer = mock(PrintWriter.class);

    when(request.getParameter("id")).thenReturn("" + id);
    when(helper.getCommentById(id)).thenReturn(Optional.of(comment));
    when(response.getWriter()).thenReturn(writer);

    servlet.doPost(request, response, analysis);

    verify(analysis).analyzeText(comment);
    verify(analysis).analyzeImage(comment);
    verify(writer).println(anyString());
  }

  /** Test that a comment isn't analyzed when not */
  @Test
  public void testAnalyzeNotPresent() throws IOException {
    long id = 1;

    CommentAnalysis analysis = mock(CommentAnalysis.class);


    CommentPersistHelper helper = mock(CommentPersistHelper.class);
    AnalyzeServlet servlet = new AnalyzeServlet(helper);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    PrintWriter writer = mock(PrintWriter.class);

    when(request.getParameter("id")).thenReturn("" + id);
    when(helper.getCommentById(id)).thenReturn(Optional.empty());

    servlet.doPost(request, response, analysis);

    verify(analysis, never()).analyzeText(any(Comment.class));
    verify(analysis, never()).analyzeImage(any(Comment.class));
    verify(response, never()).getWriter();
  }
}
