package org.metadatacenter.submission.ncbi.pipelines.generic;

public class NcbiGenericConstants {

  /*** General CEDAR fields ***/
  public static final String VALUE_FIELD = "@value";
  public static final String ID_FIELD = "@id";

  /*** General NCBI template fields ***/
  //public static final String DEFAULT_RELEASE_DATE = "12-12-2023";

  /*** BioProject section ***/
  public static final String BIOPROJECT_ELEMENT = "CEDAR-NCBI(BioProject)";
  public static final String BIOPROJECT_CONTACT_INFO_FIELD = "Contact Information (data collection)";
  public static final String BIOPROJECT_STUDY_ID_FIELD = "Study ID";
  public static final String BIOPROJECT_STUDY_TITLE_FIELD = "Study Title";
  public static final String BIOPROJECT_STUDY_TYPE_FIELD = "Study Type";
  public static final String BIOPROJECT_FUNDING_AGENCY_FIELD = "Funding Agency";
  public static final String BIOPROJECT_LAB_NAME_FIELD = "Lab Name";

  /*** BioSample section ***/

  // Instance fields
  public static final String BIOSAMPLE_ELEMENT = "CEDAR-NCBI(BioSample)";
  public static final String BIOSAMPLE_SAMPLE_ID = "Sample ID";
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

  // Other BioSample fields
  public static final String BIOSAMPLE_SCHEMA_VERSION = "2.0";
  public static final String BIOSAMPLE_SUBMISSION_DESCRIPTOR = "CEDAR-NCBI Submission";
  public static final String BIOSAMPLE_ORGANISM = "Homo sapiens";
  public static final String BIOSAMPLE_PACKAGE = "Human.1.0";

  /*** SRA section ***/
  public static final String SRA_ELEMENT = "sequenceReadArchive";
  public static final String SRA_LIBRARY_ID_FIELD = "libraryID";
  public static final String SRA_LIBRARY_NAME_FIELD = "libraryName";
  public static final String SRA_LIBRARY_INSTRUMENT_FIELD = "instrumentModel";
  public static final String SRA_LIBRARY_STRATEGY_FIELD = "libraryStrategy";
  public static final String SRA_LIBRARY_SOURCE_FIELD = "librarySource";
  public static final String SRA_LIBRARY_SELECTION_FIELD = "librarySelection";
  public static final String SRA_LIBRARY_LAYOUT_FIELD = "libraryLayout";
  public static final String SRA_LIBRARY_CONSTRUCTION_PROTOCOL_FIELD = "libraryConstructionProtocol";
  public static final String SRA_DESIGN_DESCRIPTION_FIELD = "designDescription";
  public static final String SRA_TARGET_SUBSTRATE_FIELD = "targetSubstrate";
  public static final String SRA_TARGET_SUBSTRATE_QUALITY_FIELD = "targetSubstrateQuality";
  public static final String SRA_LIBRARY_GENERATION_METHOD_FIELD = "libraryGenerationMethod";
  public static final String SRA_LIBRARY_GENERATION_PROTOCOL_FIELD = "libraryGenerationProtocol";
  public static final String SRA_TARGET_LOCUS_PCR_FIELD = "targetLocusPCR";
  public static final String SRA_FORWARD_PCR_PRIMER_TARGET_LOCATION_FIELD = "forwardPCRPrimerTargetLocation";
  public static final String SRA_REVERSE_PCR_PRIMER_TARGET_LOCATION_FIELD = "reversePCRPrimerTargetLocation";
  public static final String SRA_WHOLE_VS_PARTIAL_SEQUENCES_FIELD = "wholeVsPartialSequences";
  public static final String SRA_COMPARISON_HEAVY_LIGHT_PAIRED_CHAINS_FIELD = "comparisonHeavyLightPairedChains";
  public static final String SRA_NG_TEMPLATE_FIELD = "nGTemplate";
  public static final String SRA_TOTAL_READS_PASSING_QC_FILTER_FIELD = "totalReadsPassingQCFilter";
  public static final String SRA_PROTOCOL_ID_FIELD = "protocolID";
  public static final String SRA_READ_LENGTH_FIELD = "readLength";
  public static final String SRA_SEQUENCING_PLATFORM_FIELD = "sequencingPlatform";
  public static final String SRA_DATE_OF_SEQUENCING_RUN_FIELD = "dateOfSequencingRun(ToIdentifyProblematicRuns)";
  public static final String SRA_SEQUENCING_FACILITY_FIELD = "sequencingFacility";
  public static final String SRA_BATCH_NUMBER_FIELD = "batchNumber";
  public static final String SRA_SEQUENCING_KIT_FIELD = "sequencingKit";
  public static final String SRA_FILE_TYPE_FIELD = "File Type";
  public static final String SRA_FILE_NAME_FIELD = "File Name";

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
