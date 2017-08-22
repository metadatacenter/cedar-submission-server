package org.metadatacenter.submission;

public class Constants {

  /* Execution settings */
  public static boolean NCBI_AIRR_SUBMIT = true; // if false, the FTP submission is ignored
  public static boolean NCBI_AIRR_UPLOAD_SUBMIT_READY_FILE = true;

  /* NCBI-AIRR */
  public static String NCBI_AIRR_LOCAL_FOLDER_NAME = "ncbi-airr-upload";
  public static int NCBI_AIRR_SIMULATION_MODE_TIMEOUT = 30000;
  public static String SUBMISSION_XML_FILE_NAME = "submission.xml";
  public static String NCBI_AIRR_REPORT_REGEX = "(report)\\.([0-9]+)\\.(xml)";
  public static String NCBI_AIRR_XSLT_PATH = "xslt/ncbi_airr_report.xsl";
  public static String NCBI_AIRR_TEST_SUBMISSION_PATH = "submit/Test/2017-07-24T20-48-57.829Z_test"; // used for testing

  // PRIVATE //

  /**
   * The caller references the constants using Constants.EMPTY_STRING,
   * and so on. Thus, the caller should be prevented from constructing objects of
   * this class, by declaring this private constructor.
   */
  private Constants() {
    // This restricts instantiation
    throw new AssertionError();
  }
}