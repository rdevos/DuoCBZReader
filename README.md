# A lightweight Scala-based CBZ reader for comic books. 

## Features

Read a cbz comic in single or two-page mode, or two different comics side by side for comparison.


## how to run

you will need a java JDK and a development environment that can compile scala,
e.g. IntelliJ with the scala plugin

## if sources are out of date:

run the sbt task
    sbt assembly

this will create
    target/scala-3.7.0/duoCBZReader.jar

## when duoCBZReader.jar is built 

    java -jar target/scala-3.7.0/duoCBZReader.jar
or
    run.sh

## to build a standalone app for macos

cd to scripts and run buildmacos.sh


