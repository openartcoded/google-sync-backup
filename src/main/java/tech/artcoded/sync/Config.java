package tech.artcoded.sync;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class Config {
  @Value("${drive.credentials}")
  private FileSystemResource fileCredentials;

  @Value("${application.idempotentFilePath}")
  private String idempotentFilePath;

  @Value("${drive.sharedDirectory}")
  private String sharedDirectory;
  @Value("${drive.application-name}")
  private String applicationName;

  @Bean("fileIdempotentRepository")
  public IdempotentRepository fileIdempotentRepository() {
    return FileIdempotentRepository.fileIdempotentRepository(new java.io.File(idempotentFilePath));
  }

  @Bean
  public DriveService driveService() throws GeneralSecurityException, IOException {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    GoogleCredentials credentials = GoogleCredentials.fromStream(fileCredentials.getInputStream())
      .createScoped(Collections.singleton(DriveScopes.DRIVE));
    Drive service = new Drive.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
      .setApplicationName(applicationName)
      .build();
    // all pages: the first one only has 100 files, the folder could be missing from it
    String directoryId = DriveService.findFolderId(service, sharedDirectory);
    return DriveService.builder()
      .drive(service)
      .sharedFolderId(directoryId)
      .build();
  }

}
