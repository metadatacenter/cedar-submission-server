# CEDAR BioSample Server

To run the server

    mvn exec:java

The server will listen on port 9010. This port number can be changed in the `config.yml` file.

To try an example CEDAR BioSample instance:

    curl -X POST -H "Accept: application/json" -H "Content-type: application/json" \
         -d @./examples/AMIA2016DemoBioSampleInstance-Example.json \
         "http://localhost:9010/validate"

To try an example CEDAR AIRR instance:

    curl -X POST -H "Accept: application/json" -H "Content-type: application/json" \
         -d @./examples/EAB2017DemoAIRRSampleInstance-Example.json \
         "http://localhost:9010/validate_airr"

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

#### Building from Source

To build this server you must have the following items installed:

+ [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
+ A tool for checking out a [Git](http://git-scm.com/) repository
+ Apache's [Maven](http://maven.apache.org/index.html)

First download and build the CEDAR parent project:

    git clone https://github.com/metadatacenter/cedar-parent.git
    cd cedar-parent
    mvn clean install 
    cd ..
   
Get a copy of the latest server code:

    git clone https://github.com/metadatacenter/cedar-biosample-server.git

Change into the cedar-biosample-server directory:

    cd cedar-biosample-server

Then build it with Maven:

    mvn clean install

