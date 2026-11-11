package tech.artcoded.sync;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.artcoded.sync.DriveMocks.file;
import static tech.artcoded.sync.DriveMocks.page;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CleanupDriveTest {

  private CleanupDrive cleanup(DriveMocks mocks, boolean enabled) {
    var cleanup = new CleanupDrive(DriveService.builder().drive(mocks.drive).sharedFolderId("folder").build());
    ReflectionTestUtils.setField(cleanup, "cleanupDrive", enabled);
    return cleanup;
  }

  @Test
  void deletesTheFilesOfEveryPage() throws IOException {
    var mocks = new DriveMocks();
    when(mocks.list.execute()).thenReturn(page("next", file("a", "a.zip")), page(null, file("b", "b.zip")));

    cleanup(mocks, true).cleanup();

    // the query is set again for each page
    verify(mocks.list, times(2)).setQ(DriveQueries.OWNED_FILES);
    verify(mocks.list).setPageToken("next");
    verify(mocks.files).delete("a");
    verify(mocks.files).delete("b");
    verify(mocks.emptyTrash).execute();
  }

  @Test
  void aFailedDeletionDoesNotStopTheCleanup() throws IOException {
    var mocks = new DriveMocks();
    when(mocks.list.execute()).thenReturn(page(null, file("a", "a.zip"), file("b", "b.zip")));
    // execute() returns Void: mockito sees it as a void method, it must be stubbed with doXxx()
    doThrow(new IOException("boom")).doAnswer(_ -> null).when(mocks.delete).execute();

    cleanup(mocks, true).cleanup();

    verify(mocks.files).delete("b");
    verify(mocks.emptyTrash).execute();
  }

  @Test
  void doesNothingWhenDisabled() throws IOException {
    var mocks = new DriveMocks();

    cleanup(mocks, false).cleanup();

    verify(mocks.files, never()).list();
    verify(mocks.files, never()).delete(anyString());
  }
}
