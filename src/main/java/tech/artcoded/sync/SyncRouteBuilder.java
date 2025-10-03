package tech.artcoded.sync;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;

@Component
public class SyncRouteBuilder extends RouteBuilder {
  public static final String HEADER_TITLE = "NotificationTitle";
  public static final String HEADER_TYPE = "NotificationType";
  public static final String SYNC_DATA_DRIVE = "SYNC_DATA_DRIVE";
  public static final String CORRELATION_ID = "CorrelationId";
  public static final String NOTIFICATION_ENDPOINT = "jms:topic:notification";
  private final DriveService driveService;

  public SyncRouteBuilder(DriveService driveService) {
    this.driveService = driveService;
  }

  @Override
  public void configure() throws Exception {
    onException(Exception.class)
        .handled(true)
        .transform().simple("Exception occurred due: ${exception.message}")
        .log("${body}")
        .doTry()
        .setHeader(CORRELATION_ID, body())
        .setHeader(HEADER_TITLE, exchangeProperty(HEADER_TITLE))
        .setHeader(HEADER_TYPE, exchangeProperty(HEADER_TYPE))
        .to(ExchangePattern.InOnly, NOTIFICATION_ENDPOINT)
        .endDoTry();

    from("timer://foo?fixedRate=true&period=7200000")
        .routeId("SyncRoute::ScheduledDeletion")
        .log("running scheduled deletion on google drive...")
        .bean(() -> this, "scheduledDeletion");

    from("file:{{application.pathToSync}}?noop=true&idempotent=true&idempotentRepository=#fileIdempotentRepository")
        .routeId("SyncRoute::Entrypoint")
        .log("receiving file '${headers.%s}', will sync it to drive".formatted(Exchange.FILE_NAME))
        .setProperty(HEADER_TITLE, simple("'${headers.%s}', has been uploaded to drive".formatted(Exchange.FILE_NAME)))
        .setProperty(HEADER_TYPE, constant(SYNC_DATA_DRIVE))
        .bean(() -> this, "sync")
        .setHeader(CORRELATION_ID, body())
        .setHeader(HEADER_TITLE, exchangeProperty(HEADER_TITLE))
        .setHeader(HEADER_TYPE, exchangeProperty(HEADER_TYPE))
        .to(ExchangePattern.InOnly, NOTIFICATION_ENDPOINT);
  }

  @SneakyThrows
  void scheduledDeletion() {
    var drive = this.driveService.getDrive();
    var files = drive.files().list().execute();
    var quota = drive.about().get().setFields("storageQuota").execute().getStorageQuota();

    double percentUsed = (quota.getUsage() * 100.0) / quota.getLimit();
    var message = "Storage used: %.2f%% (%d / %d bytes)%n".formatted(percentUsed, quota.getUsage(), quota.getLimit());
    log.info(message);
    if (percentUsed > 70.) {
      log.info("reaching max space, cleanup a few things.");
      var gFiles = files.getFiles();
      for (var gFile : gFiles.stream()
          .sorted((f1, f2) -> Long.compare(
              f1.getCreatedTime().getValue(),
              f2.getCreatedTime().getValue()))
          .limit(2).toList()) {
        try {
          drive.files().delete(gFile.getId()).execute();
          log.info("deleted {}", gFile.getId());
        } catch (Exception e) {
          log.error("error {}", e.getCause());
        }
      }
      drive.files().emptyTrash().execute();
      log.info("done");

    } else {
      log.info("all good, nothing to do.");
    }

  }

  String sync(@Body File file,
      @Header(Exchange.FILE_NAME) String fileName,
      @Header(Exchange.FILE_CONTENT_TYPE) String contentType) throws IOException {
    try (var is = FileUtils.openInputStream(file)) {
      return driveService.upload(contentType, fileName, is);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
