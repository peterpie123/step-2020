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

import com.google.cloud.language.v1.AnalyzeSentimentResponse;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Tests the CommentAnalysis class */
@RunWith(JUnit4.class)
public class CommentAnalysisTest {
  /** Test sentiment analysis */
  @Test
  public void testAnalyzeText() {
    CommentAnalysis analysis = new CommentAnalysis();
    float expectedSentiment = 1;

    LanguageServiceClient client = mock(LanguageServiceClient.class);
    AnalyzeSentimentResponse sentimentResponse = mock(AnalyzeSentimentResponse.class);
    Sentiment sentiment = mock(Sentiment.class);
    Comment comment = mock(Comment.class);

    when(comment.getText()).thenReturn("Comment text");
    when(client.analyzeSentiment(Mockito.any(Document.class))).thenReturn(sentimentResponse);
    when(sentimentResponse.getDocumentSentiment()).thenReturn(sentiment);
    when(sentiment.getScore()).thenReturn(expectedSentiment);

    analysis.analyzeText(comment, client);
    Assert.assertEquals(expectedSentiment, analysis.getTextSentiment(), .001);
  }


}
