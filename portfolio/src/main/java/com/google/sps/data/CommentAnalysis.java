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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

/**
 * Represents analysis with GCloud of a particular comment. Right now just holds image labels, but
 * will eventually also analyze the sentiment of the comment text
 */
public class CommentAnalysis {
  private List<ImageLabel> imageLabels;

  /** Represents a single label for an image. Serves as a convenient wrapper for serialization */
  private static class ImageLabel {
    private final String description;
    private final float score;

    public ImageLabel(EntityAnnotation annotation) {
      description = annotation.getDescription();
      score = annotation.getScore();
    }

    public String getDescription() {
      return description;
    }

    public float getScore() {
      return score;
    }
  }

  public CommentAnalysis() {
    imageLabels = new ArrayList<>();
  }

  /**
   * Attaches image analysis, reading from the given comment. Performs no analysis if image does not
   * exist
   */
  public void analyzeImage(Comment comment) throws IOException {
    if (comment.getBlobKey() == null) {
      return;
    }
    byte[] imageBytes = getBlobBytes(comment.getBlobKey());
    List<EntityAnnotation> labels = getImageLabels(imageBytes);
    labels.stream().forEach(entity -> imageLabels.add(new ImageLabel(entity)));
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  /**
   * Retrieve binary data from blobstore at the given url
   */
  private static byte[] getBlobBytes(BlobKey blobKey) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    int fetchSize = BlobstoreService.MAX_BLOB_FETCH_SIZE;
    long currentByteIndex = 0;
    boolean continueReading = true;
    while (continueReading) {
      // End index is inclusive, so we have to subtract 1 to get fetchSize bytes
      byte[] b =
          blobstoreService.fetchData(blobKey, currentByteIndex, currentByteIndex + fetchSize - 1);
      outputBytes.write(b);

      // If we read fewer bytes than we requested, then we reached the end
      if (b.length < fetchSize) {
        continueReading = false;
      }

      currentByteIndex += fetchSize;
    }

    return outputBytes.toByteArray();
  }

  /**
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the given image
   */
  private static List<EntityAnnotation> getImageLabels(byte[] imageBytes) throws IOException {
    ByteString byteString = ByteString.copyFrom(imageBytes);
    Image image = Image.newBuilder().setContent(byteString).build();

    Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    requests.add(request);

    BatchAnnotateImagesResponse batchResponse;
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      batchResponse = client.batchAnnotateImages(requests);
    }
    List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
    AnnotateImageResponse imageResponse = imageResponses.get(0);

    if (imageResponse.hasError()) {
      System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
      return null;
    }

    return imageResponse.getLabelAnnotationsList();
  }
}
