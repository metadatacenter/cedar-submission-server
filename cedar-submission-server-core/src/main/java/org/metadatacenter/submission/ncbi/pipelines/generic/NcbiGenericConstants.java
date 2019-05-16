package org.metadatacenter.submission.ncbi.pipelines.generic;

public class NcbiGenericConstants {

  /*** General CEDAR fields ***/
  public static final String VALUE_FIELD = "@value";
  public static final String ID_FIELD = "@id";

  /*** General NCBI template fields ***/
  public static final String SUBMISSIONS_RELEASE_DATE_FIELD = "release_date";

  /*** BioProject section ***/
  public static final String BIOPROJECT_ELEMENT = "NCBI BioProject for Human Tissue1";
  public static final String BIOPROJECT_STUDY_ID_FIELD = "Study ID";
  public static final String BIOPROJECT_STUDY_TITLE_FIELD = "Study Title";
  public static final String BIOPROJECT_STUDY_TYPE_FIELD = "Study Type";
  public static final String BIOPROJECT_STUDY_CRITERIA_FIELD = "Study Criteria";
  public static final String BIOPROJECT_CONTACT_INFO_FIELD = "Contact Information (data collection)";
  public static final String BIOPROJECT_LAB_NAME_FIELD = "Lab Name";
  public static final String BIOPROJECT_LAB_ADDRESS_FIELD = "Lab Address";
  public static final String BIOPROJECT_RELEVANT_PUBLICATIONS_FIELD = "Relevant Publications";

  /*** BioSample section ***/
  // Instance fields
  public static final String BIOSAMPLE_ELEMENT = "NCBI BioSample for Human Tissue";
  public static final String BIOSAMPLE_SAMPLE_ID = "Sample ID"; // Note: this field is currently not part of the template
  public static final String BIOSAMPLE_ORGANISM_FIELD = "organism";
  public static final String BIOSAMPLE_ISOLATE_FIELD = "isolate";
  public static final String BIOSAMPLE_AGE_FIELD = "age";
  public static final String BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD = "biomaterial_provider";
  public static final String BIOSAMPLE_SEX_FIELD = "sex";
  public static final String BIOSAMPLE_TISSUE_FIELD = "tissue";
  public static final String BIOSAMPLE_CELL_LINE_FIELD = "cell_line";
  public static final String BIOSAMPLE_CELL_TYPE_FIELD = "cell_type";
  public static final String BIOSAMPLE_CELL_SUBTYPE_FIELD = "cell_subtype";
  public static final String BIOSAMPLE_CULTURE_COLLECTION_FIELD = "culture_collection";
  public static final String BIOSAMPLE_DEVELOPMENT_STAGE_FIELD = "dev_stage";
  public static final String BIOSAMPLE_DISEASE_FIELD = "disease";
  public static final String BIOSAMPLE_DISEASE_STAGE_FIELD = "disease_stage";
  public static final String BIOSAMPLE_ETHNICITY_FIELD = "ethnicity";
  public static final String BIOSAMPLE_HEALTH_STATE_FIELD = "health_state";
  public static final String BIOSAMPLE_KARYOTYPE_FIELD = "karyotype";
  public static final String BIOSAMPLE_PHENOTYPE_FIELD = "phenotype";
  public static final String BIOSAMPLE_POPULATION_FIELD = "population";
  public static final String BIOSAMPLE_RACE_FIELD = "race";
  public static final String BIOSAMPLE_SAMPLE_TYPE_FIELD = "sample_type";
  public static final String BIOSAMPLE_TREATMENT_FIELD = "treatment";

  // Other BioSample fields
  public static final String BIOSAMPLE_SCHEMA_VERSION = "2.0";
  public static final String BIOSAMPLE_SUBMISSION_DESCRIPTOR = "CEDAR-NCBI Submission";
  public static final String BIOSAMPLE_ORGANISM = "Homo sapiens";
  public static final String BIOSAMPLE_PACKAGE = "Human.1.0";

  /*** SRA section ***/
  public static final String SRA_ELEMENT = "NCBI SRA for Human Tissue";
  public static final String SRA_BIOSAMPLE_ACCESSION_FIELD = "biosample_accession";
  public static final String SRA_LIBRARY_ID_FIELD = "LIBRARY_NAME";
  public static final String SRA_TITLE_FIELD = "LIBRARY_NAME";
  public static final String SRA_LIBRARY_STRATEGY_FIELD = "LIBRARY_STRATEGY";
  public static final String SRA_LIBRARY_SOURCE_FIELD = "LIBRARY_SOURCE";
  public static final String SRA_LIBRARY_SELECTION_FIELD = "LIBRARY_SELECTION";
  public static final String SRA_LIBRARY_LAYOUT_FIELD = "LIBRARY_LAYOUT";
  public static final String SRA_PLATFORM_FIELD = "PlatformType";
  public static final String SRA_INSTRUMENT_MODEL_FIELD = "INSTRUMENT_MODEL";
  public static final String SRA_DESIGN_DESCRIPTION_FIELD = "DESIGN_DESCRIPTION";
  public static final String SRA_FILE_TYPE_FIELD = "File Type";
  public static final String SRA_FILE_NAME_FIELD = "Filename";

  /*** Other ***/
  public static final String NOT_COLLECTED_VALUE = "not collected";
  public static final String MISSING_VALUE = "missing";

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
