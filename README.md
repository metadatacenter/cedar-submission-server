# CEDAR BioSample Server

To run the server

mvn exec:java

To try an example CEDAR BioSample instance:

curl -X POST -H "Accept: application/json" -H "Content-type: application/json" \
     -d @./examples/AMIA2016DemoBioSampleInstance-Example.json \
     "http://localhost:9010/validate"

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

 curl -X POST -d @./examples/Human.1.0-Example.xml "https://www.ncbi.nlm.nih.gov/projects/biosample/validate/"

