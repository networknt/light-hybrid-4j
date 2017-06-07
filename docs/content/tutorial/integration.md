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


