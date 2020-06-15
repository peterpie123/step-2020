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

package com.google.sps;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.sps.data.CommentPersistHelper;
import com.google.sps.servlets.DataServlet;
import com.google.sps.servlets.ImageServlet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** */
@RunWith(JUnit4.class)
public class ServletTests {

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

}
