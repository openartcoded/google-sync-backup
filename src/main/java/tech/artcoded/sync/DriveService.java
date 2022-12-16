package tech.artcoded.sync;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveService {
  private Drive drive;
  private String sharedFolderId;

  public String upload(String contentType, String filename, byte[] file) throws IOException {
    File fileMetadata = new File();
    fileMetadata.setParents(List.of(sharedFolderId));
    fileMetadata.setName(filename);
    ByteArrayContent mediaContent = new ByteArrayContent(contentType, file);

    File upload = drive.files().create(fileMetadata, mediaContent)
      .setFields("id, parents")
      .execute();
    return upload.getId();
  }

}
