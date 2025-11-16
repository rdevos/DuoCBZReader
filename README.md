
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

# menu

- Right To Left: enables/disables Right To Left mode, if enabled

  - the primary comic will be on the left,

  - wide images will be split right hand half first
  - cursor left will advance pages, cursor right goes backwards.

# keys

- q: Quit

- cursor left: previous pages (or next pages in left to right mode)

- cursor right: next pages (or previous pages in left to right mode)

- cursor up: zoom in

- cursor up: zoom out

- plus: next page of 2nd comic

- minus: previous page of 2nd comic

you can also navigate through pages using the mouse buttons: click mouse button 1 to advance, mouse button 3 to go back

when zoomed in, use mouse drag ot mouse wheel to scroll or use:

- 2: scroll down

- 4: scroll left

- 6: scroll right

- 8: scroll up


