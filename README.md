Stage your Project
==================

Copies your projects jar dependencies into target/staged and 
writes target/start which can run a class configured with all
your jars on the classpath

This was extracted from Play Framework, I've made only minimal
changes to parameterize it.

Settings
--------

* stage-main-and-args = the main class to run (and any arguments)
* package-excludes = any jars not to stage 


Usage
-----

In `build.sbt`:

    // Add as plugin etc.
    Stage.settingsWithMain("my.main.Class and-an-arg")

Now from SBT:

    > stage
