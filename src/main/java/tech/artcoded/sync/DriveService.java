package tech.artcoded.sync;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveService {
  private Drive drive;
  private String sharedFolderId;

  public String upload(String contentType, String filename, InputStream is) throws IOException {
    File fileMetadata = new File();
    fileMetadata.setParents(List.of(sharedFolderId));
    fileMetadata.setName(filename);
    InputStreamContent mediaContent = new InputStreamContent(contentType, is);

    File upload = drive.files().create(fileMetadata, mediaContent)
        .setFields("id, parents")
        // needed when the shared folder lives in a shared drive
        .setSupportsAllDrives(true)
        .execute();
    return upload.getId();
  }

  /** the id of the folder with that name (case insensitive), looking through every page of results */
  public static String findFolderId(Drive drive, String name) throws IOException {
    String pageToken = null;
    do {
      var result = drive.files().list()
          .setQ(DriveQueries.FOLDERS)
          .setFields("nextPageToken, files(id, name)")
          .setSupportsAllDrives(true)
          .setIncludeItemsFromAllDrives(true)
          .setPageToken(pageToken)
          .execute();
      if (result.getFiles() != null) {
        for (var f : result.getFiles()) {
          if (name.equalsIgnoreCase(f.getName())) {
            return f.getId();
          }
        }
      }
      pageToken = result.getNextPageToken();
    } while (pageToken != null);
    throw new IllegalStateException("shared directory '%s' not found, cannot start the service".formatted(name));
  }

}
