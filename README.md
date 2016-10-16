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

1. Clone the project fro github
2. Find required ROM's (See configuration below)
3. run gradlew clean build / ./gradlew clean build
5. run gradlew makedist / ./gradlew makedist
6. Move to the /dest directory
7. Configure ROM's into EmulatorInfo.json
8. run java -jar NascomEmulator-0.1.0.jar
9. Enjoy that 4MHz goodness !

###Configuration:

The emulator works by emulating the 80-BUS,  not any specific computer built on this bus. By use of
configuration files, any 80-BUS based system can be constructed.  This is done by supplying a class file
for each card type required and a configuration file to describe the required machine. A pre-built configuration is available
via the root of the project. The emulator reads the emulatorInfo.json file on start up. 

The emulator.json file consists of an array of configurations, one for each card.  The information per card consists of:
 
 1. name - the name of the card to be shown in diagnostics and the UI
 2. clazz - the java class file containing the cards custom code
 3. order - which bus slow the card resides in. The CPU / bus master must be the first card in order
 4. properties - name / value pairs for card unique configuration

The default emulatorInfo.json configures a single 4MHz Nascom II with a 32K RAM 'A' card, as shown below.
```
[
  {
    "name": "Nascom 2",
    "clazz": "com.codingrodent.emulator.cards.nascom2cpu.Nascom2CPUCard",
    "order": "0",
    "properties": {
      "VideoROM": "resources/hexdumpImages/ROM/system/CharacterGraphicsROM.nas",
      "OperatingSystem": "resources/hexdumpImages/ROM/system/NAS_SYS_3.nas",
      "8KROM": "resources/hexdumpImages/ROM/system/ROM_BASIC.nas",
      "VideoRAMAddress": "0800",
      "ScratchpadAddress": "0C00",
      "StartAddress": "0000",
      "BankAEnabled": "false",
      "BankBEnabled": "false",
      "nup": "false"
    }
  },
  {
    "name": "Nascom RAM 'A'",
    "clazz": "com.codingrodent.emulator.cards.nascommemory.Nascom32KRAMA",
    "order": "1",
    "properties": {
      "StartAddress": "1000",
      "Size": "32K",
      "ROMEnabled": "true",
      "ROM": "resources/hexdumpImages/ROM/system/NAS_DOS_14.nas",
      "ROMAddress": "D000",
      "EPROMType": "2708"
    }
  }
]
```


### Software

No program ROM images or program files are hosted with the emulator.  These need to be acquired by either making copies of already owned material in the correct 
format for the emulator or by downloading them from various 3rd party sites, e.g. [The Nascom Home Page](http://www.nascomhomepage.com/)

To load ROM's these need to be formatted in the .nas format. Each line represents 8 bytes as follows:

```
<Address> <8 bytes of data> <checksum>

00E0 1C 22 63 55 49 55 63 22 F9
```

The file is terminated with a single period '.'

To run the emulator, the minimum software required is an operating system. When downloaded, the emulator.json file requires to be edited to point to it. 


### Running on a Raspberry Pi (RPi)

![RPi](https://github.com/codesqueak/Nascom/img/nascom_pi.png)


### Known problem areas:

1. CPU throttling is erratic, does not always work.
2. Code needs a serious tidy up / rewrite in some places
3. ...

Let me know of any odd things that happen!





