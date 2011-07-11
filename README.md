# Instant Coffee

Instant Coffee is the result of my wanting to use CoffeeScript without having
to install all the node.js stuff. It consists of a few rake tasks that use
JCoffeeScript, so it does depend on Ruby and Java. If you're more likely to
have ruby and java installed than node.js, then you might find this useful.

At the moment it just consists of a Rakefile which you can download directly
[here](https://github.com/fredericksgary/instant-coffee/raw/master/Rakefile).
It can be used as your project's main Rakefile, or placed somewhere such as
lib/tasks and loaded.

You have to edit the first line of the Rakefile which declares which
directories have coffeescript sources and which directories they should be
compiled into. Once that's setup, the tasks are

    rake instant_coffee:build # compiles everything
    rake instant_coffee:watch # compiles everything and watches for changes

It will create a `tmp` directory into which it downloads the JCoffeeScript jar,
and in which it keeps cached versions of the compiled coffeescript.
