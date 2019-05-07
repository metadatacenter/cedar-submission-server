package org.metadatacenter.submission.ncbi.pipelines.generic;

public class NcbiGenericConstants {

  /*** General CEDAR fields ***/
  public static final String VALUE_FIELD = "@value";
  public static final String ID_FIELD = "@id";

  /*** General NCBI template fields ***/
  public static final String DEFAULT_RELEASE_DATE = "12-12-2023";

  /*** BioProject section ***/
  public static final String BIOPROJECT_ELEMENT = "CEDAR-NCBI(BioProject)";
  public static final String CONTACT_INFO_FIELD = "Contact Information (data collection)";
  public static final String LAB_NAME_FIELD = "Lab Name";





  /*** BioSample section ***/

  /*** SRA section ***/

  // PRIVATE //

  /**
   * The caller references the constants using Constants.EMPTY_STRING,
   * and so on. Thus, the caller should be prevented from constructing objects of
   * this class, by declaring this private constructor.
   */
  private NcbiGenericConstants() {
    // This restricts instantiation
    throw new AssertionError();
  }

}
