// Copyright 2020 Google LLC
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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.sps.data.CommentAnalysis;

/** Analyzes the posted image with GCloud vision */
@WebServlet("/analyze")
public class AnalyzeServlet extends HttpServlet {

  /** Query string for the image to be analyzed */
  private static final String IMAGE_URL = "url";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    CommentAnalysis analysis = new CommentAnalysis();
    
    analysis.analyzeImage(request.getParameter(IMAGE_URL));
    response.getWriter().println(analysis.toString());
  }
}