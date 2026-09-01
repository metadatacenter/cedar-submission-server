package org.metadatacenter.submission.config;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.config.ImmPortConfig;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.AbstractCedarConfigTest;

public class CedarConfigSubmissionTest extends AbstractCedarConfigTest {

  @Override
  protected SystemComponent getSystemComponent() {
    return SystemComponent.SERVER_SUBMISSION;
  }

  /**
   * The NCBI FTP account and the ImmPort credentials are the submission server's alone. The
   * submittable template identifiers in the same section are not: the resource and worker servers
   * read them, through the workspace and search libraries, and this server never does.
   */
  @Override
  protected void assertServerSpecificConfig(CedarConfig config) {
    FTPConfig ftp = config.getSubmissionConfig().getNcbi().getSra().getFtp();
    assertResolved("submission.ncbi.sra.ftp.host", ftp.getHost());
    assertResolved("submission.ncbi.sra.ftp.user", ftp.getUser());
    assertResolved("submission.ncbi.sra.ftp.submissionDirectory", ftp.getSubmissionDirectory());

    ImmPortConfig immPort = config.getSubmissionConfig().getImmPort();
    assertResolved("submission.immPort.authentication.user", immPort.getAuthentication().getUser());
  }

}
