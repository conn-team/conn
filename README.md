# Conn [![Build Status](https://travis-ci.org/conn-team/conn.svg?branch=master)](https://travis-ci.org/conn-team/conn)

*Student project*

Another instant messenger with end-to-end encryption.

![Screenshot](screenshot.png)

## Requirements

- Oracle Java 8-10
- OpenJDK with OpenJFX has not been tested

## Compiling

    ./generate-protos.sh    # Generate protobuf classes
    mvn clean install       # Compile everything

## Basic usage

    ./generate-ssl-keys.sh  # Generate keystore for testing
    ./start-server.sh       # Run server
    ./start-client.sh       # Run client with localhost as target server

## Legal

- Project code is licensed under the [MIT license](https://opensource.org/licenses/MIT)

- [Twemoji](https://github.com/twitter/twemoji) by Twitter are licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

- [Message alert tone](https://freesound.org/people/RICHERlandTV/sounds/351540/) by RICHERlandTV is licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/)
