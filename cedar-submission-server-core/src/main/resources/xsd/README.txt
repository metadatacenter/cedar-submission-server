
This directory contains XML Schema descriptions of BioSample and SRA submissions.
These schemas were developed by NCBI. Note that these files are currently hand
modified to fix namespace issues and to simplify the schema descriptions.
The schemas and modificatios made to them are listed below.

SP.common.xsd: 

Common attributes used by NCBI submissions. Used as is.

BioSample.xsd:

Description of BioSample part of a submission. Modified to include SP.common.xsd from a local file.

SRASubmission:

Description of an SRA submission. Modifed to fix some namespace issues.
Modified to hard code BioSample.xsd as the submission data in the Action/AddData/Data/XMLContent element.
Also modified to replace AddFiles element definition with one in schema received from 
Ahmad at Yale (./examples/submission-com.xsd).

BioSampleValidatorResponse.xsd:

Describes response from NCBI BioSample validator.
