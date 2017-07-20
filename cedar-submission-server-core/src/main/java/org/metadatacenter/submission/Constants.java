package org.metadatacenter.submission;

public class Constants
{

  public static String NCBI_AIRR_TMP_FOLDER_NAME = "ncbi-airr-upload";
  public static boolean NCBI_AIRR_UPLOAD_SUBMIT_READY_FILE = false;
  public static boolean NCBI_AIRR_SIMULATION_MODE = false;
  public static int NCBI_AIRR_SIMULATION_MODE_TIMEOUT = 30000;

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