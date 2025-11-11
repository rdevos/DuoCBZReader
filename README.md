
# how to run

(assuming java is installed)

## if sources are out of date:

run
    sbt assembly

this will create
    target/scala-3.3.6/duoCBZReader.jar

## when duoCBZReader.jar is built 

    java -jar target/scala-3.3.6/duoCBZReader.jar
or
    run.sh



# keys

- q: Quit

- cursor left: previous pages (or next pages in left to right mode)

- cursor right: next pages (or previous pages in left to right mode)

- cursor up: zoom in

- cursor up: zoom out

- plus: next right page

- minus: previous right page

when zoomed in, use mouse drag to scroll or use:

- 2: scroll down

- 4: scroll left

- 6: scroll right

- 8: scroll up
