package org.metadatacenter.submission.ncbi.pipelines;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public class NcbiPipelinesCommonUtil {

  public static Optional<JsonNode> getTemplateElementNode(JsonNode node, String elementName) {
    if (node.has(elementName)) {
      if (node.hasNonNull(elementName) && node.get(elementName).size() > 0) {
        return Optional.of(node.get(elementName));
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

}
