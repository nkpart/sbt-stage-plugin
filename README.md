Stage your Project
==================

Copies your projects jar dependencies into target/staged and 
writes target/start which can run a class configured with all
your jars on the classpath

This was extracted from Play Framework, I've made only minimal
changes to parameterize it.

Usage
-----

In `build.sbt`:

    // Add as plugin etc.

    StageKeys.stageMainAndArgs := "my.main.Class and-an-arg"

    Stage.defaultSettings

Now from SBT:

    > stage

SBT Settings
------------

* stageMainAndArgs = the main class to run (and any arguments)
* packageExcludes = anything not to stage 

Credits
-------

All of the code has come from the Play framework's SBT plugin. Thanks!

https://github.com/playframework/playframework/blob/master/framework/src/sbt-plugin/src/main/scala/PlayCommands.scala#L177

