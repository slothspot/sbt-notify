# sbt-notify

Simple sbt plugin to show notifications after compile/test tasks finished. Currently uses AppleScript and
Notification center so works only for OS X >= 10.9 and requires sbt version 0.13.8 or newer.

### Install:
Add following to your project/sbtnotify.sbt
```
addSbtPlugin("name.dmitrym" % "sbt-notify" % "0.1")
```

### Usage:
`nt` task executes `compile:compile` and shows notification with result.

`test:nt` task executes `test:test` and shows notification with result.


### TODO:

- Clean-up the code
- Add support for Linux

### License:

Code is under Apache 2.0 license.