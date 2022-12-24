package tech.artcoded.sync;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
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
        FileList files = drive.files().list().execute();
        log.info("files being deleted: {}",files);
        List<File> gFiles = files.getFiles();
        for(var gFile: gFiles) {
          try {
            drive.files().delete(gFile.getId()).execute();
            log.info("deleted {}",gFile.getId());
          }catch(Exception e) {
            log.error("error {}", e.getCause());
          }
        }
        drive.files().emptyTrash().execute();
        log.info("done");
    }
  }
}
