package org.metadatacenter.submission.ncbi.pipelines.cairr;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.ncbi.pipelines.IncompleteInstanceValidationContract;

import java.util.List;

import static org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrConstants.*;

/** The NCBI MiAIRR pipeline reports an incomplete instance rather than failing on it. */
class NcbiCairrIncompleteInstanceTest extends IncompleteInstanceValidationContract {

  private final NcbiCairrValidator validator = new NcbiCairrValidator();

  @Override
  protected Pipeline pipeline() {
    return new Pipeline(BIOPROJECT_ELEMENT, BIOPROJECT_FIELDS, BIOSAMPLE_ELEMENT, BIOSAMPLE_FIELDS,
        BIOSAMPLE_SAMPLE_ID_FIELD, SRA_ELEMENT, SRA_FIELDS, SRA_SAMPLE_ID_FIELD, SRA_FILE_NAME_FIELD);
  }

  @Override
  protected CEDARValidationResponse validateInstance(JsonNode instance) {
    return validator.validateInstance(instance);
  }

  @Override
  protected CEDARValidationResponse validateFilenames(JsonNode instance, List<String> userFileNames) {
    return validator.validateFilenames(instance, userFileNames);
  }
}
