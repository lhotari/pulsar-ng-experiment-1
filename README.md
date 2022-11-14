# Pulsar NG Experiments


### Raft experiment with Ratis

Current status: As simple as possible experiment of using Ratis. The code is not production level and the goal is to slowly learn what Ratis is about and improve the experiment to cover the experiment stated in the [blog post](https://codingthestreams.com/pulsar/2022/10/24/rearchitecting-pulsar-part-2.html#experimenting-with-apache-ratis-and-the-sharding-model).

THIS IS JUST A DEAD SIMPLE SIMULATION. The simulation will be improved.


#### Running the example

Start 4 terminal windows and run these commands:

```bash
./gradlew :app:bootRun --args='--shardapp.peer_index=0'
```

```bash
./gradlew :app:bootRun --args='--shardapp.peer_index=1'
```

```bash
./gradlew :app:bootRun --args='--shardapp.peer_index=2'
```

```bash
./gradlew :app:bootRun -Papp=org.apache.pulsar.experiment.admin.app.AdminApp --args='--server.port=8080'
```

#### Adding topics

```bash
curl -X POST -d '{"name":"mytopic"}' -H 'Content-Type: application/json' http://localhost:8080/topics
```