# NiFi Processor For LDES Components

This component serves as a wrapping component for all LDES components. 
As of the first version, this component shall also contain the business logic for the LDES client.

## Build The Processors

To build the project run the following maven command:

```maven
mvn clean install
```

The output of this process will be a NiFi Archive NAR. 

This artifact can be then used inside of the `lib` folder of your NiFi installation.

## Using The Components 

The NiFi Archive will contain multiple LDES NiFi Processors. Below follows a short description how these can be used.

### LDES Client

The main goal for the LDES Client is to follow a Linked Data Event Stream and passing it through whilst keep it in sync.

#### Parameters 

* **Datasource url**: Url to the LDES datasource on which the client will follow.