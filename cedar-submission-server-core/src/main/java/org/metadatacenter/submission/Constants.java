package org.metadatacenter.submission;

public class Constants
{

  /* Execution settings */
  public static boolean NCBI_AIRR_SUBMIT = false; // if false, the FTP submission is ignored
  public static boolean NCBI_AIRR_UPLOAD_SUBMIT_READY_FILE = false;

  /* Constants */
  public static String NCBI_AIRR_LOCAL_FOLDER_NAME = "ncbi-airr-upload";
  public static int NCBI_AIRR_SIMULATION_MODE_TIMEOUT = 30000;
  public static String SUBMISSION_XML_FILE_NAME = "submission.xml";

  // PRIVATE //

  /**
   The caller references the constants using Constants.EMPTY_STRING,
   and so on. Thus, the caller should be prevented from constructing objects of
   this class, by declaring this private constructor.
   */
  private Constants()
  {
    // This restricts instantiation
    throw new AssertionError();
  }
}