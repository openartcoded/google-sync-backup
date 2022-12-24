package tech.artcoded.sync;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

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
      .endDoTry()
    ;

    from("file:{{application.pathToSync}}?noop=true&idempotent=true&idempotentRepository=#fileIdempotentRepository")
      .routeId("SyncRoute::Entrypoint")
      .log("receiving file '${headers.%s}', will sync it to drive".formatted(Exchange.FILE_NAME))
      .setProperty(HEADER_TITLE, simple("'${headers.%s}', has been uploaded to drive".formatted(Exchange.FILE_NAME)))
      .setProperty(HEADER_TYPE, constant(SYNC_DATA_DRIVE))
      .convertBodyTo(byte[].class)
      .bean(() -> this, "sync")
      .setHeader(CORRELATION_ID, body())
      .setHeader(HEADER_TITLE, exchangeProperty(HEADER_TITLE))
      .setHeader(HEADER_TYPE, exchangeProperty(HEADER_TYPE))
      .to(ExchangePattern.InOnly, NOTIFICATION_ENDPOINT)
    ;
  }

  String sync(@Body byte[] file,
              @Header(Exchange.FILE_NAME) String fileName,
              @Header(Exchange.FILE_CONTENT_TYPE) String contentType) throws IOException {
    return driveService.upload(contentType, fileName, file);
  }
}
