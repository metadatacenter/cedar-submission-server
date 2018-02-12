package org.metadatacenter.submission.ncbi.cairr;

import generated.Submission;
import generated.TypeProject;
import org.metadatacenter.submission.BioProject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.StringWriter;

/**
 * Convert a CAIRR BioProject element instance into a BioProject XML-based submission.
 */
public class CAIRRBioProjectElementInstance2BioProjectXMLConverter
{
  public String generateNCBIBioProjectSubmissionXML(BioProject cairrBioProjectElement)
    throws DatatypeConfigurationException, JAXBException
  {
    final generated.ObjectFactory ncbiObjectFactory = new generated.ObjectFactory();

    TypeProject ncbiBioProjectSubmission = ncbiObjectFactory.createTypeProject();

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
