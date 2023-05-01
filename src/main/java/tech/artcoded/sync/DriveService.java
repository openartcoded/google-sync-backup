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
        .execute();
    return upload.getId();
  }

}
