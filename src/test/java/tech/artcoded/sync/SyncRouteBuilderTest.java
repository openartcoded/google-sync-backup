package tech.artcoded.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.artcoded.sync.DriveMocks.file;
import static tech.artcoded.sync.DriveMocks.page;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncRouteBuilderTest {
  static final String NOTIFICATIONS = SyncRouteBuilder.NOTIFICATION_ENDPOINT;

  @TempDir
  Path dir;

  CamelContext context;

  @AfterEach
  void stop() {
    if (context != null) {
      context.stop();
    }
  }

  /** the real routes, with jms replaced by seda (no broker) and an in memory idempotent repository */
  private CamelContext start(DriveService driveService) throws Exception {
    context = new DefaultCamelContext();
    var props = new Properties();
    props.setProperty("application.pathToSync", dir.toString());
    context.getPropertiesComponent().setInitialProperties(props);
    context.getRegistry().bind("fileIdempotentRepository", MemoryIdempotentRepository.memoryIdempotentRepository());
    context.addComponent("jms", new SedaComponent());
    context.addRoutes(new SyncRouteBuilder(driveService, true));
    context.start();
    return context;
  }

  @Test
  void aNewFileIsUploadedOnceAndNotified() throws Exception {
    var driveService = mock(DriveService.class);
    when(driveService.upload(any(), eq("dump.zip"), any(InputStream.class))).thenReturn("drive-id");
    Files.writeString(dir.resolve("dump.zip"), "data");

    start(driveService);

    var notification = context.createConsumerTemplate().receive(NOTIFICATIONS, 10_000);
    assertThat(notification).isNotNull();
    assertThat(notification.getMessage().getHeader(SyncRouteBuilder.CORRELATION_ID)).isEqualTo("drive-id");
    assertThat(notification.getMessage().getHeader(SyncRouteBuilder.HEADER_TYPE))
        .isEqualTo(SyncRouteBuilder.SYNC_DATA_DRIVE);
    assertThat(notification.getMessage().getHeader(SyncRouteBuilder.HEADER_TITLE, String.class))
        .contains("dump.zip");

    // noop + idempotent: the file stays there but is not uploaded again on the next polls
    Thread.sleep(2_000);
    verify(driveService, times(1)).upload(any(), eq("dump.zip"), any(InputStream.class));
  }

  @Test
  void aFailedUploadSendsAnErrorNotification() throws Exception {
    var driveService = mock(DriveService.class);
    when(driveService.upload(any(), anyString(), any(InputStream.class))).thenThrow(new RuntimeException("quota"));
    Files.writeString(dir.resolve("dump.zip"), "data");

    start(driveService);

    var notification = context.createConsumerTemplate().receive(NOTIFICATIONS, 10_000);
    assertThat(notification).isNotNull();
    assertThat(notification.getMessage().getHeader(SyncRouteBuilder.CORRELATION_ID)).isEqualTo("GOOGLE_DRIVE_ERROR");
    assertThat(notification.getMessage().getHeader(SyncRouteBuilder.HEADER_TITLE, String.class)).contains("quota");
  }

  @Test
  void scheduledDeletionRemovesTheTwoOldestFilesAbove70Percent() throws Exception {
    var mocks = new DriveMocks();
    mocks.quota(80L, 100L);
    when(mocks.list.execute()).thenReturn(page(null, file("old-1", "a.zip"), file("old-2", "b.zip")));
    var builder = new SyncRouteBuilder(DriveService.builder().drive(mocks.drive).build(), false);

    builder.scheduledDeletion();

    verify(mocks.list).setQ(DriveQueries.OWNED_FILES);
    verify(mocks.list).setOrderBy("createdTime");
    verify(mocks.list).setPageSize(SyncRouteBuilder.FILES_DELETED_PER_RUN);
    verify(mocks.files).delete("old-1");
    verify(mocks.files).delete("old-2");
    verify(mocks.emptyTrash).execute();
  }

  @Test
  void scheduledDeletionDoesNothingBelow70Percent() throws Exception {
    var mocks = new DriveMocks();
    mocks.quota(50L, 100L);
    var builder = new SyncRouteBuilder(DriveService.builder().drive(mocks.drive).build(), false);

    builder.scheduledDeletion();

    verify(mocks.files, never()).list();
    verify(mocks.files, never()).delete(anyString());
  }

  @Test
  void scheduledDeletionDoesNothingWithoutLimit() throws Exception {
    var mocks = new DriveMocks();
    mocks.quota(50L, null);
    var builder = new SyncRouteBuilder(DriveService.builder().drive(mocks.drive).build(), false);

    builder.scheduledDeletion();

    verify(mocks.files, never()).delete(anyString());
    verify(mocks.files, never()).list();
  }
}
