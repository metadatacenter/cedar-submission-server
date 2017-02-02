# CEDAR Submission Server


The server will listen on port 9010.

To try an example CEDAR BioSample instance:

    curl -X POST \
      -H "Accept: application/json" \
      -H "Content-type: application/json" \
      -H "Authorization: apiKey <CedarUserApiKey>" \
      -d @${CEDAR_HOME}/cedar-docs/repositories/BioSample/AMIA2016DemoBioSampleInstance-Example.json \
      "http://localhost:9010/command/validate-biosample"

To try an example CEDAR AIRR instance:

    curl -X POST \
      -H "Accept: application/json" \
      -H "Content-type: application/json" \
      -H "Authorization: apiKey <CedarUserApiKey>" \
      -d @${CEDAR_HOME}/cedar-docs/repositories/AIRR/EAB2017DemoAIRRSampleInstance-Example.json \
      "http://localhost:9010/command/validate-biosample"

Here is a success response from the server:

```
{ 
  "isValid": true,
  "messages": []
}
```

Here is an error response from the server:

```
{ 
  "isValid": false,
  "messages": [ 
                "Empty Sample Identifier.", 
                "Empty attribute value for attribute 'biomaterial provider'." 
              ]
}
```

To validate that an example XML submission works against the BioSample REST service use the included
example as follows: 

    curl -X POST \
      -d @${CEDAR_HOME}/cedar-docs/repositories/BioSample/Human.1.0-Example.xml \
      "https://www.ncbi.nlm.nih.gov/projects/biosample/validate/"
      
