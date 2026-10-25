A hybrid between monolith and microservices to take advantages of both.

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-hybrid-4j) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com/style/light-hybrid-4j/) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

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


### Jackson 3 development migration

This development line requires Jackson 3.2.3, json-schema-validator 3.0.8 and the
migrated yaml-rule 4.0.0-SNAPSHOT plus light-4j/http-client snapshots. Jackson types in
public APIs use `tools.jackson`; annotation packages remain unchanged. Existing
project versions are unchanged during implementation, and a coordinated
next-major release is required before publication.

JSON handling derives from light-4j's measured compatibility profile. Service
schema YAML now uses Jackson's SnakeYAML Engine parser, retaining the measured
legacy stream-read limits. SchemaHandler.parseYaml still declares IOException:
unchecked Jackson parse/stream failures are wrapped so existing checked-error
handlers continue to work. The startup loader also catches unchecked Jackson
serialization failures.

For typed service metadata such as skipAuth, use canonical true/false. The YAML
resolver changes yes/no/on/off, True/FALSE, Null/NULL, 012, 0x1F, 0b11, 1_000 and
~ scalar interpretation; non-canonical values can become strings and fail typed
use. Quote intended strings and use canonical numeric/null values. Existing
fixture scalars and both parsed service-spec maps match the Jackson 2 baseline;
this does not audit deployed service jars. Classic SnakeYAML pins are retained
until the final dependency cleanup phase.
