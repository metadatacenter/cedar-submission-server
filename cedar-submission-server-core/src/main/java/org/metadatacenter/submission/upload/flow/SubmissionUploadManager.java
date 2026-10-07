package org.metadatacenter.submission.upload.flow;

import jakarta.ws.rs.BadRequestException;
import org.metadatacenter.submission.exception.SubmissionInstanceNotFoundException;
import org.metadatacenter.util.upload.ChunkUploadStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Service-specific view of the shared chunk assembler; status is a detached snapshot. */
public class SubmissionUploadManager {
  private static final SubmissionUploadManager INSTANCE = new SubmissionUploadManager();
  private final ChunkUploadStore uploads = new ChunkUploadStore();
  private SubmissionUploadManager() { }
  public static SubmissionUploadManager getInstance() { return INSTANCE; }

  public String accept(FlowData data, String owner, String folder) throws IOException {
    return uploads.accept(owner, data.getSubmissionId(), data.getTotalFilesCount(), Path.of(folder),
        new ChunkUploadStore.Chunk(data.getFlowIdentifier(), data.getFlowFilename(), data.getFlowChunkNumber(),
            data.getFlowChunkSize(), data.getFlowCurrentChunkSize(), data.getFlowTotalSize(), data.getFlowTotalChunks(),
            FlowUploadUtil.isMetadataFile(data)), data.getFlowFileInputStream());
  }

  public boolean isSubmissionUploadComplete(String owner, String id) throws SubmissionInstanceNotFoundException {
    return required(owner, id).complete();
  }
  public boolean claimComplete(String owner, String id) { return uploads.claimComplete(owner, id); }
  public void releaseClaim(String owner, String id) { uploads.releaseClaim(owner, id); }
  public void removeSubmissionStatus(String owner, String id) { uploads.retire(owner, id); }

  private ChunkUploadStore.Status required(String owner, String id) throws SubmissionInstanceNotFoundException {
    var status = uploads.status(owner, id);
    if (status == null) throw new SubmissionInstanceNotFoundException("Upload not found: " + id);
    return status;
  }
  public List<String> getSubmissionFilePaths(String owner, String id) throws SubmissionInstanceNotFoundException {
    var status = required(owner, id);
    if (!status.complete()) throw new BadRequestException("The upload is not complete");
    return status.files().values().stream().map(ChunkUploadStore.FileStatus::path).toList();
  }
  public SubmissionUploadStatus getSubmissionsUploadStatus(String owner, String id) {
    var status = uploads.status(owner, id);
    if (status == null) return null;
    Map<String, FileUploadStatus> files = new HashMap<>();
    status.files().forEach((key, f) -> files.put(key,
        new FileUploadStatus(f.totalChunks(), f.uploadedChunks(), f.path(), f.metadata())));
    return new SubmissionUploadStatus(status.totalFiles(), status.uploadedFiles(), files, status.folder());
  }
}
