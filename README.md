# Directive Consumer
This project is intended to be run locally on a macbook as part of the Shadowman Sprint demo. The operating system dependency currently exists due to the mechanism we use for simulating keystrokes ([cliclick](https://github.com/andykrohg/cliclick)).

## Kafka Consumer
We use the [Kakfa Camel Component](https://camel.apache.org/components/latest/kafka-component.html) to consume messages from a kafka cluster containing three topics:

### directive-red
A topic containing instructions for the avatar associated with Team Red Hat fielded from user inputs, whose messages are JSON in the following format:
```json
{
    "username":"<username>",
    "direction":"<up|down|left|right>"
}
```

### directive-white
Same as red above, but for the avatar associated with Team White Hat.

### game-over
When a message is received from this topic, the game ends. The message body is simply a string `red` or `white`, indicating which color won.

## Distributed Consensus Data
Commands are translated into keyboard inputs about once every 300 milliseconds. Inputs received within these intervals are placed into a buffer `List<Map<String,String>>`, the entries for which are created by Camel unmarshalling the JSON objects received on the `directive-red` and `directive-white` topics.

On the 300 millisecond interval, we use Decision Manager to determine a consensus of which input to send for each user (up, down, left, or right). Then we stream the inputs in the buffer to determine which of them were in agreement with the consensus, and which or them weren't. The result of this stream is pushed into a Data Grid cache of the form `Map<String, Integer>`, where the key is a person's username, and the value is their _score_. Each input earns a user +1 if it's in agreement with the consensus, and -1 if it's in disagreement.

## Ending the Game
When the first player reaches the goal, the game sends a message to the `game-over` topic with that player's color. Our gameOver route will set the static `gameOver` variable to `true`, so inputs are no longer processed. We then consult Data Grid to find the __MVP__ and __Biggest Troll__ for each team, which are the contributors from the audience with the highest and lowest scores, respectively.

## Configuration
### datagrid.properties
You'll need to update the addresses specified at __infinispan.client.hotrod.server_list__ and __infinispan.client.hotrod.sni_host_name__ to match the true location of your Data Grid instance.

You'll also need to download the client certificate from Data Grid and place it at `/tmp/certs/tls.crt`

### kafka.properties
You'll need to update the address specified at __kafka.brokers__ to match the true location of your kafka cluster.

You'll also need to download the client certificate from Kafka and place it at `tmp/certs/ca.crt`




