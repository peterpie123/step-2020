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

package com.google.sps.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.vision.v1.EntityAnnotation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests the ImageLabel class */
@RunWith(JUnit4.class)
public class ImageLabelTest {
  /** Test the image label static inner class */
  @Test
  public void testImageLabel() {
    String description = "Test description";
    float score = -17;

    EntityAnnotation annotation = mock(EntityAnnotation.class);
    when(annotation.getDescription()).thenReturn(description);
    when(annotation.getScore()).thenReturn(score);

    ImageLabel label = new ImageLabel(annotation);

    Assert.assertEquals(description, label.getDescription());
    Assert.assertEquals(score, label.getScore(), .001);
  }
}
