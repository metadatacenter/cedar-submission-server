package org.metadatacenter.submission.ncbi.pipelines.generic;

import biosample.TypeAttribute;
import biosample.TypeBioSample;
import biosample.TypeBioSampleIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import common.sp.*;
import generated.ObjectFactory;
import generated.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

/**
 * Convert a CEDAR JSON Schema-based NCBI2CEDAR instance to a BioProject/BioSample/SRA XML-based submission.
 */
public class NcbiGenericTemplateInstance2XMLConverter {
  final static Logger log = LoggerFactory.getLogger(NcbiGenericTemplateInstance2XMLConverter.class);

  private static final String CEDAR_NAMESPACE = "CEDAR";

  private List<String> sraIds = new ArrayList<>();

  private final ObjectFactory submissionObjectFactory = new ObjectFactory();
  private final common.sp.ObjectFactory ncbiCommonObjectFactory = new common.sp.ObjectFactory();
  private final biosample.ObjectFactory bioSampleObjectFactory = new biosample.ObjectFactory();
  //private final bioproject.ObjectFactory bioProjectObjectFactory = new bioproject.ObjectFactory();

  private final String instanceDateFormat = "yyyy-MM-dd";
  private final String xmlDateFormat = "yyyy-MM-dd'-'hh:mm";

  public String convertTemplateInstanceToXML(JsonNode instance) throws JAXBException, DatatypeConfigurationException, ParseException {

    Submission ncbiSubmission = submissionObjectFactory.createSubmission();

    // TODO: Release date
    String releaseDate = null;

    // Submission description
    Submission.Description submissionDescription = createSubmissionDescription(instance);
    ncbiSubmission.setDescription(submissionDescription);

    /*** BioProject ***/

    JsonNode bioProject = NcbiGenericUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);

    // BioProject ID
    String bioProjectID = null;
    Optional<String> studyId = NcbiGenericUtil.getTemplateFieldValue(bioProject, BIOPROJECT_STUDY_ID_FIELD, true, true);
    if (studyId.isPresent()) {
      bioProjectID = studyId.get();
    }

    /*** BioSample ***/
    JsonNode biosamples = NcbiGenericUtil.getTemplateElementNode(instance, BIOSAMPLE_ELEMENT);

    int customSampleId = 0;

    for (JsonNode bioSample : biosamples) {

      // Start <BioSample> section
      TypeBioSample ncbiBioSample = bioSampleObjectFactory.createTypeBioSample();
      ncbiBioSample.setSchemaVersion(BIOSAMPLE_SCHEMA_VERSION);

      // Sample ID
      Optional<String> sampleId = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_ID, false, false);
      if (sampleId.isPresent()) {
        ncbiBioSample.setSampleId(createBioSampleIdentifier(sampleId.get()));
      }
      // TODO: Delete this 'else'. Sample ID will be required. It should be part of the template and have a value
      else {
        ncbiBioSample.setSampleId(createBioSampleIdentifier(Integer.toString(customSampleId++)));
      }

      // Descriptor
      ncbiBioSample.setDescriptor(createDescriptor(BIOSAMPLE_SUBMISSION_DESCRIPTOR, BIOSAMPLE_SUBMISSION_DESCRIPTOR));

      // Organism
      ncbiBioSample.setOrganism(createOrganism(BIOSAMPLE_ORGANISM));

      // Package
      ncbiBioSample.setPackage(BIOSAMPLE_PACKAGE);

      // Attributes
      ncbiBioSample.setAttributes(createBioSampleAttributes(bioSample, releaseDate));

      // XmlContent
      // Development Note: The original NCBI submission doesn't include the BioSample element, so it
      // is required to modify the submission.xsd file (See submission.xsd:441)
      TypeInlineData.XmlContent xmlContent = submissionObjectFactory.createTypeInlineDataXmlContent();
      xmlContent.setBioSample(ncbiBioSample);

      // Data
      Submission.Action.AddData.Data bioSampleData = submissionObjectFactory.createSubmissionActionAddDataData();
      bioSampleData.setContentType("XML");
      bioSampleData.setXmlContent(xmlContent);

      // Identifier
      TypeIdentifier actionIdentifier = ncbiCommonObjectFactory.createTypeIdentifier();
      TypeSPUID bioSampleSpuid = ncbiCommonObjectFactory.createTypeSPUID();
      bioSampleSpuid.setSpuidNamespace(CEDAR_NAMESPACE);
      bioSampleSpuid.setValue(createNewActionId());
      actionIdentifier.setSPUID(bioSampleSpuid);

      // Action/AddData
      Submission.Action.AddData bioSampleSubmissionActionAddData = submissionObjectFactory.createSubmissionActionAddData();
      bioSampleSubmissionActionAddData.setTargetDb(TypeTargetDb.BIO_SAMPLE);
      bioSampleSubmissionActionAddData.setData(bioSampleData);
      bioSampleSubmissionActionAddData.setIdentifier(actionIdentifier);

      // Action
      Submission.Action bioSampleAction = submissionObjectFactory.createSubmissionAction();
      bioSampleAction.setAddData(bioSampleSubmissionActionAddData);
      ncbiSubmission.getAction().add(bioSampleAction);

    }

    /*** SRA ***/
    JsonNode sras = NcbiGenericUtil.getTemplateElementNode(instance, SRA_ELEMENT);

    // Retrieve the SRAs from the instance
    for (JsonNode sra : sras) {

      Submission.Action.AddFiles sraAddFiles = submissionObjectFactory.createSubmissionActionAddFiles();
      sraAddFiles.setTargetDb(TypeTargetDb.SRA);


      // TODO
      // File type and file names
//      Optional<String> fileType = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_FILE_TYPE_FIELD);
//
//      if (fileType.isPresent()) {
//
//        if (sra.hasNonNull(SRA_FILE_NAME_FIELD) && sra.get(SRA_FILE_NAME_FIELD).size() > 0) {
//
//          Iterator fileNameFieldsIt = sra.get(SRA_FILE_NAME_FIELD).iterator();
//
//          while (fileNameFieldsIt.hasNext()) {
//
//            String fileNameField = fileNameFieldsIt.next().toString();
//
//            Optional<String> fileName = NcbiGenericUtil.getTemplateFieldValue(sra, fileNameField);
//
//            if (fileName.isPresent()) {
//
//              Submission.Action.AddFiles.File sraFile = submissionObjectFactory.createSubmissionActionAddFilesFile();
//              sraFile.setFilePath(fileName.get());
//              sraFile.setDataType(fileType.get());
//              sraAddFiles.getFile().add(sraFile);
//
//            } else {
//              throw new IllegalArgumentException("File name field not present: " + fileNameField);
//            }
//          }
//        } else {
//          // In this case there is a file type but no file names, so we throw an exception
//          throw new IllegalArgumentException("Missing array of file names");
//        }
//      } else {
//        // do nothing
//      }

      // Reference to BioSample ID

//      if (sequenceReadArchive.getSampleID() != null && sequenceReadArchive.getSampleID().getValue() != null) {
//        String bioSampleID = sequenceReadArchive.getSampleID().getValue();
//        TypeFileAttributeRefId bioSampleAttributeRefId = submissionObjectFactory.createTypeFileAttributeRefId();
//        bioSampleAttributeRefId.setName("BioSample");
//        TypeRefId refId = ncbiCommonObjectFactory.createTypeRefId();
//        TypeSPUID spuid = ncbiCommonObjectFactory.createTypeSPUID();
//        spuid.setSpuidNamespace(CEDAR_NAMESPACE);
//        spuid.setValue(bioSampleID);
//        refId.setSPUID(spuid);
//        bioSampleAttributeRefId.setRefId(refId);
//        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(bioSampleAttributeRefId);
//      }

      // Reference to BioProject ID

      if (!bioProjectID.isEmpty()) {
        TypeFileAttributeRefId bioProjectAttributeRefId = submissionObjectFactory.createTypeFileAttributeRefId();
        bioProjectAttributeRefId.setName("BioProject");
        TypeRefId refId = ncbiCommonObjectFactory.createTypeRefId();
        TypePrimaryId primaryId = ncbiCommonObjectFactory.createTypePrimaryId();
        primaryId.setDb("BioProject");
        primaryId.setValue(bioProjectID);
        refId.setPrimaryId(primaryId);
        bioProjectAttributeRefId.setRefId(refId);
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(bioProjectAttributeRefId);
      }

      // Library ID
      Optional<String> libraryId = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_ID_FIELD, true, true);
      if (libraryId.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_ID");
        fileAttribute.setValue(libraryId.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Name
      Optional<String> libraryName = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_NAME_FIELD, true, true);
      if (libraryName.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_name");
        fileAttribute.setValue(libraryName.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Instrument
      Optional<String> libraryInstrument = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_INSTRUMENT_FIELD, true, true);
      if (libraryInstrument.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_instrument");
        fileAttribute.setValue(libraryInstrument.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Strategy
      Optional<String> libraryStrategy = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_STRATEGY_FIELD, true, true);
      if (libraryStrategy.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_strategy");
        fileAttribute.setValue(libraryStrategy.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Source
      Optional<String> librarySource = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_SOURCE_FIELD, true, true);
      if (librarySource.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_source");
        fileAttribute.setValue(librarySource.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Selection
      Optional<String> librarySelection = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_SELECTION_FIELD, true, true);
      if (librarySelection.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_selection");
        fileAttribute.setValue(librarySelection.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Layout
      Optional<String> libraryLayout = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_LAYOUT_FIELD);
      if (libraryLayout.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_layout");
        fileAttribute.setValue(libraryLayout.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Construction Protocol
      Optional<String> libraryConstructionProtocol = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_CONSTRUCTION_PROTOCOL_FIELD);
      if (libraryConstructionProtocol.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_construction_protocol");
        fileAttribute.setValue(libraryConstructionProtocol.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Design Description
      Optional<String> designDescription = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_DESIGN_DESCRIPTION_FIELD);
      if (designDescription.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("design_description");
        fileAttribute.setValue(designDescription.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Target Substrate
      Optional<String> targetSubstrate = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_TARGET_SUBSTRATE_FIELD);
      if (targetSubstrate.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("target_substrate");
        fileAttribute.setValue(targetSubstrate.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Target Substrate Quality
      Optional<String> targetSubstrateQuality = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_TARGET_SUBSTRATE_QUALITY_FIELD);
      if (targetSubstrateQuality.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("target_substrate_quality");
        fileAttribute.setValue(targetSubstrateQuality.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Generation Method
      Optional<String> libraryGenerationMethod = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_GENERATION_METHOD_FIELD);
      if (libraryGenerationMethod.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_generation_method");
        fileAttribute.setValue(libraryGenerationMethod.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Generation Protocol
      Optional<String> libraryGenerationProtocol = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_LIBRARY_GENERATION_PROTOCOL_FIELD);
      if (libraryGenerationProtocol.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_generation_protocol");
        fileAttribute.setValue(libraryGenerationProtocol.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Target Locus PCR
      Optional<String> targetLocusPcr = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_TARGET_LOCUS_PCR_FIELD);
      if (targetLocusPcr.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("target_locus_pcr");
        fileAttribute.setValue(targetLocusPcr.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Forward PCR Primer Target Location
      Optional<String> forwardPcrPrimerTargetLocation = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_FORWARD_PCR_PRIMER_TARGET_LOCATION_FIELD);
      if (forwardPcrPrimerTargetLocation.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("forward_pcr_primer_target_location");
        fileAttribute.setValue(forwardPcrPrimerTargetLocation.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Reverse PCR Primer Target Location
      Optional<String> reversePcrPrimerTargetLocation = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_REVERSE_PCR_PRIMER_TARGET_LOCATION_FIELD);
      if (reversePcrPrimerTargetLocation.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("reverse_pcr_primer_target_location");
        fileAttribute.setValue(reversePcrPrimerTargetLocation.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Whole vs Partial Sequences
      Optional<String> wholeVsPartialSequences = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_WHOLE_VS_PARTIAL_SEQUENCES_FIELD);
      if (wholeVsPartialSequences.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("whole_vs_partial_sequences");
        fileAttribute.setValue(wholeVsPartialSequences.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }
      // Comparison Heavy Light Paired Chains
      Optional<String> comparisonHeavyLightPairedChains = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_COMPARISON_HEAVY_LIGHT_PAIRED_CHAINS_FIELD);
      if (comparisonHeavyLightPairedChains.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("comparison_heavy_light_paired_chains");
        fileAttribute.setValue(comparisonHeavyLightPairedChains.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }
      // NG Template
      Optional<String> ngTemplate = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_NG_TEMPLATE_FIELD);
      if (ngTemplate.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("ng_template");
        fileAttribute.setValue(ngTemplate.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Total Reads Passing QC Filter
      Optional<String> totalReadsPassingQcFilter = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_TOTAL_READS_PASSING_QC_FILTER_FIELD);
      if (totalReadsPassingQcFilter.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("total_reads_passing_qc_filter");
        fileAttribute.setValue(totalReadsPassingQcFilter.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Protocol ID
      Optional<String> protocolId = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_PROTOCOL_ID_FIELD);
      if (protocolId.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("protocol_ID");
        fileAttribute.setValue(protocolId.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Read Length
      Optional<String> readLength = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_READ_LENGTH_FIELD);
      if (readLength.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("read_length");
        fileAttribute.setValue(readLength.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Sequencing Platform
      Optional<String> sequencingPlatform = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_SEQUENCING_PLATFORM_FIELD);
      if (sequencingPlatform.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("sequencing_platform");
        fileAttribute.setValue(sequencingPlatform.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Date of Sequencing Run
      Optional<String> dateOfSequencingRun = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_DATE_OF_SEQUENCING_RUN_FIELD);
      if (dateOfSequencingRun.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("date_of_sequencing_run");
        fileAttribute.setValue(createXMLGregorianCalendar(dateOfSequencingRun.get(), instanceDateFormat).toString());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Sequencing Facility
      Optional<String> sequencingFacility = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_SEQUENCING_FACILITY_FIELD);
      if (sequencingFacility.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("sequencing_facility");
        fileAttribute.setValue(sequencingFacility.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Batch Number
      Optional<String> batchNumber = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_BATCH_NUMBER_FIELD);
      if (batchNumber.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("batch_number");
        fileAttribute.setValue(batchNumber.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Sequencing Kit
      Optional<String> sequencingKit = NcbiGenericUtil.getTemplateFieldValue(sra, SRA_SEQUENCING_KIT_FIELD);
      if (sequencingKit.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("sequencing_kit");
        fileAttribute.setValue(sequencingKit.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // TODO: check if needed
      // Release Status
      if (releaseDate != null && !releaseDate.isEmpty()) {
        TypeReleaseStatus typeReleaseStatus = submissionObjectFactory.createTypeReleaseStatus();
        TypeReleaseStatus.SetReleaseDate filesReleaseDate = submissionObjectFactory.createTypeReleaseStatusSetReleaseDate();
        filesReleaseDate.setReleaseDate(createXMLGregorianCalendar(releaseDate, instanceDateFormat));
        typeReleaseStatus.setSetReleaseDate(filesReleaseDate);
        sraAddFiles.setStatus(typeReleaseStatus);
      }

      // End of AIRR SRA Elements

      TypeSPUID sraSampleSpuid = ncbiCommonObjectFactory.createTypeSPUID();
      sraSampleSpuid.setSpuidNamespace(CEDAR_NAMESPACE);
      sraSampleSpuid.setValue(createNewSraId());

      TypeIdentifier sraIdentifier = ncbiCommonObjectFactory.createTypeIdentifier();
      sraIdentifier.setSPUID(sraSampleSpuid);

      sraAddFiles.setIdentifier(sraIdentifier);

      // Action
      Submission.Action sraAction = submissionObjectFactory.createSubmissionAction();
      sraAction.setAddFiles(sraAddFiles);

      ncbiSubmission.getAction().add(sraAction);

    }

    // Generate XML from the submission instance
    StringWriter writer = new StringWriter();
    JAXBContext ctx = JAXBContext.newInstance(Submission.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.marshal(ncbiSubmission, writer);

    return writer.toString();
  }

  private TypeOrganism createOrganism(String organismName) {
    TypeOrganism sampleOrganism = ncbiCommonObjectFactory.createTypeOrganism();
    sampleOrganism.setOrganismName(organismName);
    return sampleOrganism;
  }

  private TypeDescriptor createDescriptor(String title, String description) {
    TypeDescriptor sampleDescriptor = ncbiCommonObjectFactory.createTypeDescriptor();
    JAXBElement descriptionElement = new JAXBElement(new QName("p"), String.class, description);
    TypeBlock sampleDescription = ncbiCommonObjectFactory.createTypeBlock();
    sampleDescription.getPOrUlOrOl().add(descriptionElement);
    sampleDescriptor.setTitle(title);
    sampleDescriptor.setDescription(sampleDescription);
    return sampleDescriptor;
  }

  private TypeBioSampleIdentifier createBioSampleIdentifier(String bioSampleID) {
    TypeBioSampleIdentifier sampleID = bioSampleObjectFactory.createTypeBioSampleIdentifier();
    TypeBioSampleIdentifier.SPUID spuid = bioSampleObjectFactory.createTypeBioSampleIdentifierSPUID();
    spuid.setSpuidNamespace(CEDAR_NAMESPACE);
    spuid.setValue(bioSampleID);
    sampleID.getSPUID().add(spuid);
    return sampleID;
  }

  /*
   * Object construction for the submission <Description> section
   */
  private Submission.Description createSubmissionDescription(JsonNode instance) throws
      DatatypeConfigurationException, ParseException {

    final JsonNode bioProjectNode = NcbiGenericUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);

    Submission.Description submissionDescription = submissionObjectFactory.createSubmissionDescription();

    TypeContactInfo contactInfo = ncbiCommonObjectFactory.createTypeContactInfo();
    if (NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_CONTACT_INFO_FIELD).isPresent()) {
      contactInfo.setEmail(NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_CONTACT_INFO_FIELD).get());
    }
    else {
      throw new IllegalArgumentException("Missing required value: " + BIOPROJECT_CONTACT_INFO_FIELD);
    }

    TypeOrganization.Name organizationName = submissionObjectFactory.createTypeOrganizationName();
    if (NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_LAB_NAME_FIELD).isPresent()) {
      organizationName.setValue(NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_LAB_NAME_FIELD).get());
    }
    else {
      throw new IllegalArgumentException("Missing required value: " + BIOPROJECT_LAB_NAME_FIELD);
    }

    TypeAccount contactSubmitter = submissionObjectFactory.createTypeAccount();
    if (NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_CONTACT_INFO_FIELD).isPresent()) {
      contactSubmitter.setUserName(NcbiGenericUtil.getTemplateFieldValue(bioProjectNode, BIOPROJECT_CONTACT_INFO_FIELD).get());
    }
    else {
      throw new IllegalArgumentException("Missing required value: " + BIOPROJECT_CONTACT_INFO_FIELD);
    }

    TypeOrganization contactOrganization = submissionObjectFactory.createTypeOrganization();
    contactOrganization.setType("lab");
    contactOrganization.setRole("owner"); // TODO
    contactOrganization.setName(organizationName);
    contactOrganization.getContact().add(contactInfo);

    submissionDescription.setComment("CEDAR to NCBI Submission");
    submissionDescription.setSubmitter(contactSubmitter);
    submissionDescription.getOrganization().add(contactOrganization);

    // TODO: Release date
//    Submission.Description.Hold submissionDescriptionHold = submissionObjectFactory.createSubmissionDescriptionHold();
//    //String releaseDate = cairrInstance.getSubmissionsReleaseDate().getValue();
//    String releaseDate = DEFAULT_RELEASE_DATE;
//    if (releaseDate != null && !releaseDate.isEmpty()) {
//      submissionDescriptionHold.setReleaseDate(createXMLGregorianCalendar(releaseDate, instanceDateFormat));
//      submissionDescription.setHold(submissionDescriptionHold);
//    }

    return submissionDescription;
  }

  private TypeBioSample.Attributes createBioSampleAttributes(JsonNode bioSample, String releaseDate) throws ParseException {
    // Attributes
    TypeBioSample.Attributes bioSampleAttributes = bioSampleObjectFactory.createTypeBioSampleAttributes();

    // Organism
    Optional<String> organism = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ORGANISM_FIELD);
    if (organism.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("organism", organism.get()));
    }

    // Isolate
    Optional<String> isolate = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ISOLATE_FIELD);
    if (isolate.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("isolate", isolate.get()));
    }

    // Age
    Optional<String> age = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_AGE_FIELD);
    if (age.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("age", age.get()));
    }

    // Biomaterial Provider
    Optional<String> biomaterialProvider = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD);
    if (biomaterialProvider.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("biomaterial_provider", biomaterialProvider.get()));
    }

    // Sex
    Optional<String> sex = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SEX_FIELD);
    if (sex.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sex", sex.get()));
    }

    // Tissue
    Optional<String> tissue = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_TISSUE_FIELD);
    if (tissue.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("tissue", tissue.get()));
    }

    // Cell line
    Optional<String> cellLine = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_LINE_FIELD);
    if (cellLine.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_line", cellLine.get()));
    }

    // Cell type
    Optional<String> cellType = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_TYPE_FIELD);
    if (cellType.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_type", cellType.get()));
    }

    // Cell subtype
    Optional<String> cellSubtype = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_SUBTYPE_FIELD);
    if (cellSubtype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_subtype", cellSubtype.get()));
    }

    // Culture collection
    Optional<String> cultureCollection = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CULTURE_COLLECTION_FIELD);
    if (cultureCollection.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("culture_collection", cultureCollection.get()));
    }

    // Development stage
    Optional<String> developmentStage = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DEVELOPMENT_STAGE_FIELD);
    if (developmentStage.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("development_stage", developmentStage.get()));
    }

    // Disease
    Optional<String> disease = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DISEASE_FIELD);
    if (disease.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("disease", disease.get()));
    }

    // Disease stage
    Optional<String> diseaseStage = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DISEASE_STAGE_FIELD);
    if (diseaseStage.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("disease_stage", diseaseStage.get()));
    }

    // Ethnicity
    Optional<String> ethnicity = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ETHNICITY_FIELD);
    if (ethnicity.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("ethnicity", ethnicity.get()));
    }

    // Health state
    Optional<String> healthState = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_HEALTH_STATE_FIELD);
    if (healthState.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("health_state", healthState.get()));
    }

    // Karyotype
    Optional<String> karyotype = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_KARYOTYPE_FIELD);
    if (karyotype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("karyotype", karyotype.get()));
    }

    // Phenotype
    Optional<String> phenotype = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_PHENOTYPE_FIELD);
    if (phenotype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("phenotype", phenotype.get()));
    }

    // Population
    Optional<String> population = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_POPULATION_FIELD);
    if (population.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("population", population.get()));
    }

    // Race
    Optional<String> race = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_RACE_FIELD);
    if (race.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("race", race.get()));
    }

    // Sample type
    Optional<String> sampleType = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_TYPE_FIELD);
    if (sampleType.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sample_type", sampleType.get()));
    }

    // Treatment
    Optional<String> treatment = NcbiGenericUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_TREATMENT_FIELD);
    if (treatment.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("treatment", treatment.get()));
    }

    // TODO: check if needed
    // Release Date
    if (releaseDate != null && !releaseDate.isEmpty()) {  // Use the top-level release date
      String xmlReleaseDate = convertDateFormat(releaseDate, instanceDateFormat, xmlDateFormat);
      bioSampleAttributes.getAttribute().add(createAttribute("projected_release_date", xmlReleaseDate));
    }

    // Custom attribute to specify that the submission was done using CEDAR
    bioSampleAttributes.getAttribute().add(createAttribute("submission_tool", "CEDAR"));

    return bioSampleAttributes;
  }

  private XMLGregorianCalendar createXMLGregorianCalendar(String date, String format) throws DatatypeConfigurationException, ParseException {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new SimpleDateFormat(format).parse(date));
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
  }

  private String convertDateFormat(String date, String sourceFormat, String targetFormat) throws ParseException {
    DateFormat source = new SimpleDateFormat(sourceFormat);
    DateFormat target = new SimpleDateFormat(targetFormat);
    Date d = source.parse(date);
    return target.format(d);
  }

  private String createNewSraId() {
    String id = "SRA-" + UUID.randomUUID();
    sraIds.add(id);
    return id;
  }

  private String createNewActionId() {
    String id = "Action-" + UUID.randomUUID();
    sraIds.add(id);
    return id;
  }

  private TypeAttribute createAttribute(String attributeName, String attributeValue) {
    TypeAttribute attribute = bioSampleObjectFactory.createTypeAttribute();
    attribute.setAttributeName(attributeName);
    attribute.setValue(attributeValue);

    return attribute;
  }

}
