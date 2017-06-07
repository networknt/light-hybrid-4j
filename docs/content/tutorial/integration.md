---
date: 2016-10-12T17:06:30-04:00
title: Integration Test
---

## Integration Test

1. Copy hybrid serivce jar files into docker service folder

2. Go to light-eventuate-4j root folder, run docker-compose for event store:

  cd light-eventuate-4j
  docker-compose up

  The docker compose will start docker images for  eventuate event store:
     -- zookeeper
     --kafka
     --mysql

3. Run services:

  docker-compose -f docker-compose-service.yml up

  This command will start command and query side hybrid services


4. Test on command side to create and publish events:

  Use postmand, set post request:

  URL: http://localhost:8083/api/json

  content:
  {
    "host": "lightapi.net",
    "service":"todo",
    "action":"create",
    "version":"0.1.0",
    "title": " this is the test event from postman ",
    "completed": false,
    "order": 0
  }

5.    Test on query side to subscribe and process events:

  Use postmand, set post request:

  URL: http://localhost:8082/api/json

    content:

    {
      "host": "lightapi.net",
      "service":"todo",
      "action":"gettodos",
      "version":"0.1.0"

    }

    The event will be subscrible and processed by saving to local TODO table and will get by query side service and send back in the resposne:

{
  "message": [
    {
      "0000015c7e109e77-acde480011220000": {
        "title": " this is the test event from postman",
        "completed": false,
        "order": 0
      }
    }




# End