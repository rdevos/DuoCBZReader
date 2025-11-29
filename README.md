
# how to run

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

cd to scripts and run jlink.sh


# Menus

## Open menu

- Open: open up to two files

## Layout menu

if two files are open

- Side by Side: to display the two files side by side 

- Alternating: to switch between files using the shift key

## Size menu
 
- Fit Image: if no zoom is applied, the image will fit to the panel

- Fit Width: If no zoom is applied, the image will use all the available width

## Options menu

- ShowPage Numbers: if enabled, page numbers are shown in the lower left of the panel

- Right To Left: enables/disables Right To Left mode, if enabled

  - the primary file will be on the left,

  - wide images will be split right hand half first
  - cursor left will advance pages, cursor right goes backwards.

# Keys and Mouse

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


