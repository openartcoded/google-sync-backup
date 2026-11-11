package tech.artcoded.sync;

import java.io.IOException;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CleanupDrive {
  private final DriveService driveService;

  public CleanupDrive(DriveService driveService) {
    this.driveService = driveService;
  }

  @Value("${application.cleanup.drive}")
  private boolean cleanupDrive;

  @PostConstruct
  public void cleanup() throws IOException {
    // cleanup when drive is full
    if (cleanupDrive) {
      var drive = this.driveService.getDrive();
      // every page (the first one only has 100 files), never the folders
      String pageToken = null;
      do {
        var files = drive.files().list().setQ(DriveQueries.OWNED_FILES)
            .setFields("nextPageToken, files(id, name)").setSupportsAllDrives(true)
            .setIncludeItemsFromAllDrives(true).setPageToken(pageToken).execute();
        for (var gFile : files.getFiles()) {
          try {
            log.info("deleting {} ({})", gFile.getName(), gFile.getId());
            drive.files().delete(gFile.getId()).setSupportsAllDrives(true).execute();
          } catch (Exception e) {
            log.error("error deleting {}", gFile.getId(), e);
          }
        }
        pageToken = files.getNextPageToken();
      } while (pageToken != null);
      drive.files().emptyTrash().execute();
      log.info("done");
    }
  }
}
