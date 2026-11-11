package tech.artcoded.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.artcoded.sync.DriveMocks.file;
import static tech.artcoded.sync.DriveMocks.page;

import org.junit.jupiter.api.Test;

class DriveServiceTest {

  @Test
  void findsTheFolderOnAnyPage() throws Exception {
    var mocks = new DriveMocks();
    when(mocks.list.execute()).thenReturn(
        page("page-2", file("1", "other"), file("2", "another")),
        page(null, file("3", "V2022.Artcoded-Dump")));

    assertThat(DriveService.findFolderId(mocks.drive, "v2022.artcoded-dump")).isEqualTo("3");
    // the query is set again for each page
    verify(mocks.list, times(2)).setQ(DriveQueries.FOLDERS);
    verify(mocks.list).setPageToken("page-2");
  }

  @Test
  void failsWhenTheFolderDoesNotExist() throws Exception {
    var mocks = new DriveMocks();
    when(mocks.list.execute()).thenReturn(page(null, file("1", "other")));

    assertThatThrownBy(() -> DriveService.findFolderId(mocks.drive, "missing"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing");
  }

  @Test
  void handlesAnEmptyResult() throws Exception {
    var mocks = new DriveMocks();
    when(mocks.list.execute()).thenReturn(new com.google.api.services.drive.model.FileList());

    assertThatThrownBy(() -> DriveService.findFolderId(mocks.drive, "missing"))
        .isInstanceOf(IllegalStateException.class);
  }
}
