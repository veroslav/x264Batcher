# x264Batcher
Run multiple x264 instances in parallell to improve video encoding speed. Queue multiple encodings.

## Overview

x264Batcher provides a way to run multiple x264 instances in parallell and thus improve overall video encoding speed. It does this by splitting one or more AviSynth script files that the video consists of, into multiple ones, which are then encoded in separate instances of x264 encoder.

The primary motivation behind x264Batcher has been the fact that running a single instance of x264 for video encoding doesn't utilize all of the available CPU on the target machine. By adjusting the number of x264 instances, the CPU utilization can be increased closer to 100%, thus increasing the encoding speed.

Even though there exists a multi-threaded version of x264, many people find it to be somewhat unstable, which sometimes results in crashes and waste of the encoding time.

x264Batcher was developed to address the issues above.

In x264Batcher there is also an option to queue multiple encoding jobs. When all of the encodings have completed, there is an option to shutdown the computer.

## Installation

Following are the requirements for running x264Batcher:

- Java Runtime Environment (JRE), version 1.8 or above
- Windows OS (should work on any Windows version on which an appropriate JRE version has been installed)
- x264.exe (used for video encoding)
- mkvmerge.exe (used for joining of encoded video parts)

Even though x264Batcher is a platform independant application and runs on both MAC and Linux OS:es, the x264.exe and mkvmerge.exe applications are not. Thus, there is not much point in supporting those platforms.

Download the latest release of x264Batcher from:

If Windows is associated with .jar (JavaArchive) files, simply double click on the file and the program should launch. Otherwise, use the command prompt and run the following commands:

```
cd path/to/downloaded/jar
java -jar x264Batcher.jar
```

## Configuration, Usage

Launching the x264Batcher will bring up the main application window. The contents are grouped in tabs based on functionallity. The tabs are as follows:

- Encoder (encoding progress info, paths to executables and instance limits)
- Jobs (overview of scheduled encoding jobs, add, re-order and delete jobs)
- Preset (stored x264 commands, different presets can be used based on the type of encoded video)
- Log (logger output that displays info about what the encoder is doing)
