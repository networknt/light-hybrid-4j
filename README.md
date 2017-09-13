A hybrid between monolith and microservices to take advantages of both.

[Developer Chat](https://gitter.im/networknt/light-hybrid-4j) |
[Documentation](https://networknt.github.io/light-hybrid-4j) |
[Contribution Guide](CONTRIBUTING.md) |


[![Build Status](https://travis-ci.org/networknt/light-hybrid-4j.svg?branch=master)](https://travis-ci.org/networknt/light-hybrid-4j)

## Why Hybrid

### Switching to Microservices from Java EE is hard 

I've been working with my clients on microservies for several years and 
I feel the pain for Java EE developers to switch to microservice architecture. 
Almost everything they've learned in Java EE is anti-pattern in Microservices
and it is a big paradigm shift. It would be easy for them to move step by step
to fully adopt microservices.

### Most companies don't need Internet scale

For most companies, pure microservices might not be suitable as the
infrastructure and management overhead is too high initially. For most of
organizations, they don't need Internet scale and they are not in a size to adopt 
microservices before their application grows to the right size. For startup companies, 
it is wise to build something still monolithic but can be split up when the application
grows bigger. Most companies won't even need Internet scale for the entire life cycle 
of their application so they can enjoy the smaller foot print as well as low supporting 
cost with all the benefits of microserivces architecture.

### Restful API is not efficient

REST was introduced before Single Page Application on the browser and it is heavily rely
on the URI schema for client/server communication. In URI, all query parameters and path
parameters will be converted to string and then converted back to the right type based on
swagger specification. This is very time consuming and make the swagger spec. so complicated.

By using Hybrid framework, you just need to define the JSON schema or other binary protocol
IDL for your request and then validate the request again it. All the communication is based on 
JSON object or binary without data conversion on the server side. 

  
 


 

