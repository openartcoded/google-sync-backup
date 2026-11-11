package tech.artcoded.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/** mocks of the fluent drive api: every setter returns the request itself */
class DriveMocks {
  final Drive drive = mock(Drive.class);
  final Drive.Files files = mock(Drive.Files.class);
  final Drive.Files.List list = mock(Drive.Files.List.class);
  final Drive.Files.Delete delete = mock(Drive.Files.Delete.class);
  final Drive.Files.EmptyTrash emptyTrash = mock(Drive.Files.EmptyTrash.class);
  final Drive.About about = mock(Drive.About.class);
  final Drive.About.Get aboutGet = mock(Drive.About.Get.class);

  DriveMocks() throws IOException {
    when(drive.files()).thenReturn(files);
    when(files.list()).thenReturn(list);
    when(list.setQ(anyString())).thenReturn(list);
    when(list.setFields(anyString())).thenReturn(list);
    when(list.setOrderBy(anyString())).thenReturn(list);
    when(list.setPageSize(anyInt())).thenReturn(list);
    when(list.setPageToken(any())).thenReturn(list);
    when(list.setSupportsAllDrives(anyBoolean())).thenReturn(list);
    when(list.setIncludeItemsFromAllDrives(anyBoolean())).thenReturn(list);
    when(files.delete(anyString())).thenReturn(delete);
    when(delete.setSupportsAllDrives(anyBoolean())).thenReturn(delete);
    when(files.emptyTrash()).thenReturn(emptyTrash);
    when(drive.about()).thenReturn(about);
    when(about.get()).thenReturn(aboutGet);
    when(aboutGet.setFields(anyString())).thenReturn(aboutGet);
  }

  void quota(Long usage, Long limit) throws IOException {
    when(aboutGet.execute())
        .thenReturn(new About().setStorageQuota(new About.StorageQuota().setUsage(usage).setLimit(limit)));
  }

  static File file(String id, String name) {
    return new File().setId(id).setName(name);
  }

  static FileList page(String nextPageToken, File... files) {
    return new FileList().setFiles(List.of(files)).setNextPageToken(nextPageToken);
  }
}
