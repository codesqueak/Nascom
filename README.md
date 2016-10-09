# Nascom 2 Emulator / 80-BUS Emulator

Alpha Release - This is very much a work in progress - be warned!

## Build

(Windows)
gradlew clean build test

(Linux)
./gradlew clean build test


## Using Jenkins

The project includes a Jenkins file to control a pipeline build.  
At present the available version of the Jacoco plugin (2.0.1 at time of writing) does not support a 'publisher'.  The build was tested using a hand built plugin from the master branch of the  [project](https://github.com/jenkinsci/jacoco-plugin)

### Instructions

1. Clone the project and load into your favourite IDE
2. Execute the Emeulator.clas in NasBusEmulator
3. Enjoy that 4MHz goodness !

###Configuration:

The emulator works by emulating the 80-BUS,  not any specific computer built on this bus. By use of
configuration files, any 80-BUS based system can be cosntructed.  This is done by supplying a class file
for each card type required and a configuration file to describe the required machine.  Full details 
will be distributed in a future release.  For now, a pre-built configuration is available
via the root of the project. The emulator reads the emulatorInfo.xml file on start up. 

The default emulatorInfo.xml configures a single 4MHz Nascom II with no extended memory.

### Known problem areas:

1. CPU throttling is erratic, doesn't always work.
2. The version of XPath used needs replacing (internal proprietary API)
3. ...

Let me know of any odd things that happen!





