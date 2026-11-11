package tech.artcoded.sync;

import java.util.OptionalDouble;

/** drive search queries and small helpers, kept apart so they can be tested without google */
final class DriveQueries {
  static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

  /** every folder visible to the service account */
  static final String FOLDERS = "mimeType = '%s' and trashed = false".formatted(FOLDER_MIME_TYPE);

  /** files owned by the service account, never folders (the shared folder must survive a cleanup) */
  static final String OWNED_FILES = "'me' in owners and trashed = false and mimeType != '%s'"
      .formatted(FOLDER_MIME_TYPE);

  private DriveQueries() {
  }

  /** percentage of the quota used, empty when the drive has no limit */
  static OptionalDouble percentUsed(Long usage, Long limit) {
    if (usage == null || limit == null || limit <= 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(usage * 100.0 / limit);
  }
}
