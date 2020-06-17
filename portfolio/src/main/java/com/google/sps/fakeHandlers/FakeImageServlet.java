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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Fake class only used only for testing purposes for returning dummy data  */
public class FakeImageServlet {
  private static String RETURN_URL = "/upload-image";

  /** Return a dummy response */
  public static void doGet(HttpServletRequest request, HttpServletResponse response) {
    response.setContentType("text/html");
    response.getWriter().println(RETURN_URL);
  }
}
