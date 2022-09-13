# PubSub Service Messaging


## Warning
If you don't have a good reason to use this, then it's better to use plain HTTP, because
I wrote this library for faster and more convenient requests between specific game servers

You will also not be able to check if the server is alive, or it simply decided not to respond to your request.

In general, this library is extremely situational.

## Introduction
This simple library allows you to make requests between your server. It's a bit like the HTTP (request-response) model,
but requests are transported through a message broker, or if it's easier through the pub / sub system


Registration and request send very similar to feign

Resource Description
```java
public interface PlayerResource {
    @Request("player-info") // All requests inside one resource must be unique
    PlayerInfo getInfo(UUID playerUUID);
}
```
Resource Implementation
```java
@RequiredArgsConstructor
public class PlayerController implements PlayerResource {
    private final PlayerInfoRepository repository;
    public PlayerInfo getInfo(UUID playerUUID) throws PlayerNotFoudnException{
        return repository.getInfo(playerUUID);
    }
}
```
Register implementation
```java
PubSubMessagingService service;
PlayerController controller;

service.registerHandler(PlayerResource.class, controller);
```
Send request and get response
```java
PlayerResource client = service.createClient("serverId", PlayerResource.class);
try {
    PlayerInfo info = client.getInfo(playerUUID);
    log.info("Received player info: "+ info);
} catch(PubSubTimeout timeoutExp) {
    log.warn("Response not received in 5 second");
} catch(PlayerNotFoundException notFoundExp) {
    log.warn("Player not found")   
}
```

# Initialization

First of all, you need to connect the maven repository and library BOM

```kotlin
repositories {
    maven("https://nexus.spliterash.ru/repository/group/") {
        content { includeGroup("ru.spliterash") } // To just not stress for all other dependencies
    }
}

dependencies {
    // Not working on gradle lower 6
    implementation(platform("ru.spliterash:pub-sub-messaging-bom:1.0.0-SNAPSHOT"))
}
```

The library is written using dependency inversion, so you can build it as a constructor for your infrastructure

### Standalone application
If you have a server application, then the best choice is to use a module with redisson, it sends your messages through
redis. just add dependency, and initialize `PubSubMessagingService` with `RedissonPubSub`

```kotlin
dependencies {
    implementation("ru.spliterash:pub-sub-messaging-types-redisson")
}
```


### Multipaper
If you use multipaper, you can use this

PS. Dont forget shadow jar and relocate

```kotlin
dependencies {
    implementation("ru.spliterash:pub-sub-messaging-types-multipaper") {
        exclude("org.apache.logging.log4j")
    }
}
```

And after that just create service in your code
 
```java
MultiPaperPubSubServiceCreator.createService(...)
```
