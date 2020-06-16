package com.google.sps.data;

import com.google.cloud.vision.v1.EntityAnnotation;

/** Represents a single label for an image. Serves as a convenient wrapper for serialization. */
public class ImageLabel {
  private final String description;
  /** How closely the AI believes this label applies. Range: [0,1]. */
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
