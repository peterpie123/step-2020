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
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.sps.config.Flags;
import com.google.common.collect.ImmutableList;

/**
 * Represents analysis with GCloud of a particular comment. Right now just holds image labels, but
 * will eventually also analyze the sentiment of the comment text.
 */
public class CommentAnalysis {
  private List<ImageLabel> imageLabels;
  /** The sentiment of the comment, ranging from -1 (very negative) to 1 (very positive) */
  private float sentimentScore;

  public CommentAnalysis() {
    imageLabels = new ArrayList<>();
  }

  /**
   * Attaches image analysis, reading from the given comment. Performs no analysis if image does not
   * exist.
   */
  public void analyzeImage(Comment comment) throws IOException {
    if (Flags.IS_TEST) {
      // Add dummy data
      imageLabels.add(new ImageLabel(comment.getName(), 1));
    } else {
      // Don't use ifPresent since exceptions don't behave well with lambda expressions
      if (comment.getBlobKey().isPresent()) {
        byte[] imageBytes = getBlobBytes(comment.getBlobKey().get());
        List<EntityAnnotation> labels = getImageLabels(imageBytes);
        labels.stream().forEach(entity -> imageLabels.add(new ImageLabel(entity)));
      }
    }
  }

  /** Same as analyzeText, just with a configurable GCloud api */
  void analyzeText(Comment comment, LanguageServiceClient client) {
    Document doc = Document.newBuilder().setContent(comment.getText())
        .setType(Document.Type.PLAIN_TEXT).build();
    Sentiment sentiment = client.analyzeSentiment(doc).getDocumentSentiment();
    this.sentimentScore = sentiment.getScore();
  }

  /** Attaches text analysis, reading from the given comment. */
  public void analyzeText(Comment comment) throws IOException {
    if (Flags.IS_TEST) {
      // Send dummy data
      this.sentimentScore = 4;
    } else {
      try (LanguageServiceClient languageServiceClient = LanguageServiceClient.create()) {
        analyzeText(comment, languageServiceClient);
      }
    }
  }

  public float getTextSentiment() {
    return sentimentScore;
  }

  public List<ImageLabel> getImageLabels() {
    return imageLabels;
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  /**
   * Retrieve binary data from blobstore at the given url.
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
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the given image.
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

    // Convert to immutable list and return
    return ImmutableList.copyOf(imageResponse.getLabelAnnotationsList());
  }
}
