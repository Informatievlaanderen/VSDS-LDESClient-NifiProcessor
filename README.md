# NiFi Processor for LDES Components

This repository contains the LDES client code (LdesClient) and the nifi processor (LdesClientProcessor) that accompanies it.

## Build the Processors

To build the project run the following maven command:

```maven
mvn clean install
```

This will be the client jar and the processor NAR file (Nifi archive).
When the NAR file is placed in the `lib` folder of your NiFi installation, you can add the processor in your workflow.

## Using the Components 

The NiFi Archive will contain multiple LDES NiFi Processors. Below follows a short description how these can be used.

### LDES Client

The main goal for the LDES Client is to follow a Linked Data Event Stream and passing it through whilst keep it in sync.

#### Parameters

When running from a docker container, these arguments can be passed through your .env file.
They should be replaced with proper values in the the nifi workflow file that contains your process group.

* **TREE_DIRECTION**: makes the direction in which to follow the LDES configurable. Currently not implemented.
* **DATA_SOURCE_URL**: the URL of the LDES to follow.
* **DATA_SOURCE_FORMAT**: the data format of the LDES. This value must be recognizable by the RDFLanguages parser.
* **DATA_DESTINATION_FORMAT**: the data format to use when sending out LDES members. This value must be recognizable by the RDFLanguages parser.