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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.sps.data.Comment;
import com.google.sps.data.CommentAnalysis;
import com.google.sps.data.CommentPersistHelper;
import com.google.sps.servlets.DataServlet;
import com.google.sps.servlets.ImageServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** */
@RunWith(JUnit4.class)
public class ServletTest {

  /** Test that an image upload alias is successfully created */
  @Test
  public void testUploadImage() throws IOException {
    BlobstoreService service = mock(BlobstoreService.class);
    String imageUrl = "/upload-new-image";
    when(service.createUploadUrl(anyString())).thenReturn(imageUrl);

    ImageServlet imageServlet = new ImageServlet(service);

    HttpServletResponse response = mock(HttpServletResponse.class);
    PrintWriter writer = mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);

    imageServlet.doGet(null, response);
    verify(writer).println(imageUrl);
  }

  /** Test the image label class */
  @Test
  public void testImageLabel() {
    String description = "Test description";
    float score = -17;

    EntityAnnotation annotation = mock(EntityAnnotation.class);
    when(annotation.getDescription()).thenReturn(description);
    when(annotation.getScore()).thenReturn(score);

    CommentAnalysis.ImageLabel label = new CommentAnalysis.ImageLabel(annotation);

    Assert.assertEquals(description, label.getDescription());
    Assert.assertEquals(score, label.getScore(), .001);
  }

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
    Comment comment = mock(Comment.class);

    CommentPersistHelper helper = mock(CommentPersistHelper.class);
    AnalyzeServlet servlet = new AnalyzeServlet(helper);

    when(helper.getCommentById(id)).thenReturn(Optional.empty());

    verify(analysis, never()).analyzeText(comment);
    verify(analysis, never()).analyzeImage(comment);
  }

}
