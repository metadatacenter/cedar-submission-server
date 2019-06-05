package org.metadatacenter.submission.ncbi.pipelines.cairr_old;

import generated.Submission;
import org.metadatacenter.submission.BioProjectForAIRRNCBI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.StringWriter;

/**
 * Convert a CAIRR BioProject element instance into a BioProject XML-based submission.
 *
 * An example BioProject submission can be found here:
 * https://www.ncbi.nlm.nih.gov/viewvc/v1/trunk/submit/public-docs/bioproject/samples/bp.submission.xml?view=markup
 */
public class CAIRRBioProjectElementInstance2BioProjectXMLConverter
{
  public String generateNCBIBioProjectSubmissionXML(BioProjectForAIRRNCBI cairrBioProjectElementInstance)
    throws DatatypeConfigurationException, JAXBException
  {
    final bioproject.ObjectFactory bioProjectObjectFactory = new bioproject.ObjectFactory();

    bioproject.TypeProject ncbiBioProjectSubmission = bioProjectObjectFactory.createTypeProject();

    // TODO

    // Generate XML from the NCBI BioProject submission instance
    StringWriter writer = new StringWriter();
    JAXBContext ctx = JAXBContext.newInstance(Submission.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.marshal(ncbiBioProjectSubmission, writer);

    return writer.toString();
  }
}
