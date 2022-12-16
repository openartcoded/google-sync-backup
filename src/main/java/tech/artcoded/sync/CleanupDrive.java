package tech.artcoded.sync;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CleanupDrive {
  private final DriveService driveService;

  public CleanupDrive(DriveService driveService) {
    this.driveService = driveService;
  }

  @Value("${application.cleanup.drive}")
  private Boolean cleanupDrive;

  @PostConstruct
  public void cleanup() throws IOException {
    // cleanup when drive is full
    if (cleanupDrive) {
        var drive = this.driveService.getDrive();
        // var files = drive.files().list().execute();
        // for(var fileId: files.keySet()) {
        //   drive.files().delete(fileId).execute();
        // }
        drive.files().emptyTrash().execute();
    }
  }
}
