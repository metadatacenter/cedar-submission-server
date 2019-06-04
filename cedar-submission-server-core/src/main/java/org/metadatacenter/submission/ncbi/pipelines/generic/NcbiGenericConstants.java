package org.metadatacenter.submission.ncbi.pipelines.generic;

public class NcbiGenericConstants {

  /*** General CEDAR fields ***/
  public static final String VALUE_FIELD = "@value";
  public static final String ID_FIELD = "@id";

  /*** General NCBI template fields ***/
  public static final String CEDAR_NAMESPACE = "CEDAR";

  /*** Submission general information ***/
  public static final String SUBMISSION_RELEASE_DATE_FIELD = "Release Date";
  public static final String SUBMISSION_DESCRIPTOR = "CEDAR-NCBI Submission";

  /*** BioProject section ***/
  public static final String BIOPROJECT_ELEMENT = "NCBI BioProject for Human Tissue";
  public static final String BIOPROJECT_STUDY_ID_FIELD = "Study ID";
  public static final String BIOPROJECT_STUDY_TITLE_FIELD = "Study Title";
  public static final String BIOPROJECT_STUDY_TYPE_FIELD = "Study Type";
  public static final String BIOPROJECT_STUDY_CRITERIA_FIELD = "Study Criteria";
  public static final String BIOPROJECT_CONTACT_INFO_FIELD = "Contact Information";
  public static final String BIOPROJECT_CONTACT_EMAIL_FIELD = "Contact Email";
  public static final String BIOPROJECT_LAB_NAME_FIELD = "Lab Name";
  public static final String BIOPROJECT_LAB_ADDRESS_FIELD = "Lab Address";
  public static final String BIOPROJECT_RELEVANT_PUBLICATIONS_FIELD = "Relevant Publications";
  public static final String[] BIOPROJECT_REQUIRED = {BIOPROJECT_STUDY_ID_FIELD, BIOPROJECT_CONTACT_INFO_FIELD,
      BIOPROJECT_CONTACT_EMAIL_FIELD, BIOPROJECT_LAB_NAME_FIELD};

  /*** BioSample section ***/
  // Instance fields
  public static final String BIOSAMPLE_ELEMENT = "NCBI BioSample for Human Tissue";
  public static final String BIOSAMPLE_SAMPLE_NAME_FIELD = "Sample Name";
  public static final String BIOSAMPLE_SAMPLE_TITLE_FIELD = "Sample Title";
  public static final String BIOSAMPLE_ORGANISM_FIELD = "Organism";
  public static final String BIOSAMPLE_ISOLATE_FIELD = "Isolate";
  public static final String BIOSAMPLE_AGE_FIELD = "Age";
  public static final String BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD = "Biomaterial Provider";
  public static final String BIOSAMPLE_SEX_FIELD = "Sex";
  public static final String BIOSAMPLE_TISSUE_FIELD = "Tissue";
  public static final String BIOSAMPLE_CELL_LINE_FIELD = "Cell Line";
  public static final String BIOSAMPLE_CELL_TYPE_FIELD = "Cell Type";
  public static final String BIOSAMPLE_CELL_SUBTYPE_FIELD = "Cell Subtype";
  public static final String BIOSAMPLE_CULTURE_COLLECTION_FIELD = "Culture Collection";
  public static final String BIOSAMPLE_DEVELOPMENT_STAGE_FIELD = "Development Stage";
  public static final String BIOSAMPLE_DISEASE_FIELD = "Disease";
  public static final String BIOSAMPLE_DISEASE_STAGE_FIELD = "Disease Stage";
  public static final String BIOSAMPLE_ETHNICITY_FIELD = "Ethnicity";
  public static final String BIOSAMPLE_HEALTH_STATE_FIELD = "Health State";
  public static final String BIOSAMPLE_KARYOTYPE_FIELD = "Karyotype";
  public static final String BIOSAMPLE_PHENOTYPE_FIELD = "Phenotype";
  public static final String BIOSAMPLE_POPULATION_FIELD = "Population";
  public static final String BIOSAMPLE_RACE_FIELD = "Race";
  public static final String BIOSAMPLE_SAMPLE_TYPE_FIELD = "Sample Type";
  public static final String BIOSAMPLE_TREATMENT_FIELD = "Treatment";
  public static final String BIOSAMPLE_DESCRIPTION_FIELD = "Sample Description";
  public static final String[] BIOSAMPLE_REQUIRED = {BIOSAMPLE_SAMPLE_NAME_FIELD, BIOSAMPLE_ORGANISM_FIELD,
      BIOSAMPLE_ISOLATE_FIELD, BIOSAMPLE_AGE_FIELD, BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD, BIOSAMPLE_SEX_FIELD,
      BIOSAMPLE_TISSUE_FIELD};

  // Other BioSample fields
  public static final String BIOSAMPLE_SCHEMA_VERSION = "2.0";
  public static final String BIOSAMPLE_SUBMISSION_DESCRIPTOR = SUBMISSION_DESCRIPTOR;
  public static final String BIOSAMPLE_SUBMISSION_TOOL = "CEDAR";
  public static final String BIOSAMPLE_ORGANISM = "Homo sapiens";
  public static final String BIOSAMPLE_PACKAGE = "Human.1.0";

  /*** SRA section ***/
  public static final String SRA_ELEMENT = "NCBI SRA for Human Tissue";
  public static final String SRA_BIOSAMPLE_NAME_FIELD = "Sample Name";
  public static final String SRA_LIBRARY_ID_FIELD = "Library ID";
  public static final String SRA_TITLE_FIELD = "Title";
  public static final String SRA_LIBRARY_STRATEGY_FIELD = "Library Strategy";
  public static final String SRA_LIBRARY_SOURCE_FIELD = "Library Source";
  public static final String SRA_LIBRARY_SELECTION_FIELD = "Library Selection";
  public static final String SRA_LIBRARY_LAYOUT_FIELD = "Library Layout";
  public static final String SRA_PLATFORM_FIELD = "Platform";
  public static final String SRA_INSTRUMENT_MODEL_FIELD = "Instrument Model";
  public static final String SRA_DESIGN_DESCRIPTION_FIELD = "Design Description";
  public static final String SRA_FILE_TYPE_FIELD = "File Type";
  public static final String SRA_FILE_NAME_FIELD = "File Name";
  public static final String[] SRA_REQUIRED = {SRA_BIOSAMPLE_NAME_FIELD, SRA_LIBRARY_ID_FIELD,
      SRA_TITLE_FIELD, SRA_LIBRARY_STRATEGY_FIELD, SRA_LIBRARY_SOURCE_FIELD, SRA_LIBRARY_SELECTION_FIELD,
      SRA_LIBRARY_LAYOUT_FIELD, SRA_PLATFORM_FIELD, SRA_INSTRUMENT_MODEL_FIELD, SRA_DESIGN_DESCRIPTION_FIELD,
      SRA_FILE_TYPE_FIELD};

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
