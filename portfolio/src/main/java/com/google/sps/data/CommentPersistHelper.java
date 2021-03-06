// Copyright 2019 Google LLC
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

import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.sps.config.Flags;

/**
 * Keeps track of persisted comments with ability to add/remove comments. This is a singleton.
 */
public class CommentPersistHelper {
  public enum SortMethod {
    ASCENDING, DESCENDING
  }

  private final DatastoreService datastore;
  private final List<Comment> comments;

  private static CommentPersistHelper instance;

  private CommentPersistHelper() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    comments = new ArrayList<>();
  }

  /**
   * Returns the currently running instance of CommentPersistHelper. Comments are pre-loaded.
   */
  public static CommentPersistHelper getInstance() {
    if (instance == null) {
      instance = new CommentPersistHelper();
      if (!Flags.IS_TEST) {
        instance.loadComments();
      }
    }
    return instance;
  }

  /** Loads comments from persist storage and adds to the comments list. */
  private void loadComments() {
    Query query = new Query("Comment").addSort(Comment.COMMENT_TIMESTAMP, SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);
    results.asList(FetchOptions.Builder.withDefaults()).forEach(entity -> {
      comments.add(Comment.fromEntity(entity));
    });
  }

  /**
   * Returns the BlobKey that points to the file uploaded by the user.
   */
  private static Optional<BlobKey> getBlobKey(HttpServletRequest request) {
    if (Flags.IS_TEST) {
      return Optional.empty();
    }

    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a BlobKey. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return Optional.empty();
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so the BlobKey is empty. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo == null || blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return Optional.empty();
    }

    return Optional.of(blobKey);
  }

  /** Adds a new comment from the given HTTP POST. */
  public void addComment(HttpServletRequest request) {
    Entity entity = new Entity("Comment");
    entity.setProperty(Comment.COMMENT_TEXT, request.getParameter(Comment.COMMENT_TEXT));
    entity.setProperty(Comment.COMMENT_NAME, request.getParameter(Comment.COMMENT_NAME));
    entity.setProperty(Comment.COMMENT_TIMESTAMP, System.currentTimeMillis());

    getUploadedFileUrl(request)
        .ifPresent(url -> entity.setProperty(Comment.COMMENT_PICTURE_URL, url));
    getBlobKey(request)
        .ifPresent(blobKey -> entity.setProperty(Comment.COMMENT_PICTURE_BLOBKEY, blobKey));

    if (!Flags.IS_TEST) {
      // Store the comment so it persists
      datastore.put(entity);
    }

    // Insert new comment at the beginning to preserve sort
    comments.add(0, Comment.fromEntity(entity));
  }

  /** Deletes the given comment permanently. */
  public void deleteComment(long id) {
    for (int i = 0; i < comments.size(); i++) {
      Comment comment = comments.get(i);
      if (comment.getId() == id) {
        // Remove the comment from persistent storage and the comments list
        comments.remove(i);

        if (!Flags.IS_TEST) {
          datastore.delete(comment.getKey());

          // Remove the comment's image, if it exists
          comment.getBlobKey().ifPresent(blobKey -> {
            BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
            blobstoreService.delete(blobKey);
          });
          break;
        }
      }
    }
  }

  /** Returns the given comment, found by its ID. */
  public Optional<Comment> getCommentById(long id) {
    try {
      return Optional.of(comments.stream().filter(c -> c.getId() == id).findFirst().get());
    } catch (NoSuchElementException e) {
      return Optional.empty();
    }
  }

  /**
   * Returns a URL that points to the uploaded file.
   */
  private Optional<String> getUploadedFileUrl(HttpServletRequest request) {
    if (Flags.IS_TEST) {
      return Optional.empty();
    }

    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a URL.
    // (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return Optional.empty();
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so we can't get a URL.
    // (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return Optional.empty();
    }

    // We could check the validity of the file here, e.g.
    // to make sure it's an image file
    // https://stackoverflow.com/q/10779564/873165

    // Use ImagesService to get a URL that points to the uploaded file.
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

    // To support running in Google Cloud Shell with AppEngine's devserver, we must
    // use the relative path to the image, rather than the path returned by imagesService which
    // contains a host.
    try {
      URL url = new URL(imagesService.getServingUrl(options));
      return Optional.of(url.getPath());
    } catch (MalformedURLException e) {
      // Return normally if servingUrl is already a relative path
      return Optional.of(imagesService.getServingUrl(options));
    }
  }

  /** Returns only comments with either name or content containing filter. */
  private List<Comment> filterList(String filter) {
    if (filter == null) {
      // Don't filter
      return comments;
    }

    // Return comments that contain the filter string
    return comments.stream().filter(c -> c.contains(filter)).collect(Collectors.toList());
  }

  /**
   * Stringifies the comments in the desired order, including pagination and filtering.
   */
  public String stringifyComments(int numberComments, SortMethod sort, int paginationFrom,
      String filter) {
    Gson gson = new Gson();
    List<Comment> filteredList = filterList(filter);
    List<Comment> send;
    // List that will either be reversed or not, depending on the sort
    List<Comment> readList = null;

    if (paginationFrom < 0) {
      paginationFrom = 0;
    }
    if (paginationFrom > filteredList.size()) {
      throw new IllegalArgumentException("Error: Cannot paginate starting at " + paginationFrom
          + " when there are only " + filteredList.size() + " comments!");
    }

    if (sort == SortMethod.ASCENDING) {
      readList = filteredList;
    } else if (sort == SortMethod.DESCENDING) {
      // Comments is already sorted, so just reverse
      readList = Lists.reverse(filteredList);
    }

    int paginationTo = numberComments + paginationFrom;
    // Make sure pagination doesn't go out of bounds
    if (paginationTo > filteredList.size()) {
      paginationTo = filteredList.size();
    }
    send = readList.subList(paginationFrom, paginationTo);

    return gson.toJson(send);
  }

  /** Returns the total number of comments that are stored. */
  public int getNumberComments() {
    return comments.size();
  }

  /** Returns the total number of comments that correspond to the given filter. */
  public int getNumberComments(String filter) {
    return filterList(filter).size();
  }
}
