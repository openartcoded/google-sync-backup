package tech.artcoded.sync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DriveQueriesTest {

  @Test
  void percentUsed() {
    assertThat(DriveQueries.percentUsed(75L, 100L)).hasValue(75.0);
    assertThat(DriveQueries.percentUsed(0L, 15L * 1024 * 1024 * 1024)).hasValue(0.0);
  }

  @Test
  void noLimitMeansNoPercentage() {
    assertThat(DriveQueries.percentUsed(10L, null)).isEmpty();
    assertThat(DriveQueries.percentUsed(10L, 0L)).isEmpty();
    assertThat(DriveQueries.percentUsed(null, 100L)).isEmpty();
  }

  @Test
  void ownedFilesNeverIncludeFolders() {
    assertThat(DriveQueries.OWNED_FILES)
        .contains("'me' in owners")
        .contains("trashed = false")
        .contains("mimeType != 'application/vnd.google-apps.folder'");
  }
}
