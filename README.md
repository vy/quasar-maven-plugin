**Warning!** Due to lack of time and [Quasar developers' unwillingness to
distribute this plug-in in the official Quasar release](https://groups.google.com/d/msg/quasar-pulsar-user/GzvktONJkpY/t1_MmdcLAgAJ),
**I AM NOT MAINTAINING THIS PLUG-IN ANYMORE.** (Further, I am not even sure if
Quasar itself is maintained at all.) That being said, I would be more than happy
to handover the project to someone else stepping up.

[![Build Status](https://secure.travis-ci.org/vy/quasar-maven-plugin.svg)](http://travis-ci.org/vy/quasar-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.vlkan/quasar-maven-plugin.svg)](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22quasar-maven-plugin%22)

[Quasar](http://docs.paralleluniverse.co/quasar/) is a Java library that
provides high-performance lightweight threads, Go-like channels, Erlang-like
actors, and other asynchronous programming tools. Quasar fibers rely on bytecode
instrumentation. This can be done at classloading time via a Java Agent, or at
compilation time. This project ships a Maven plugin for the ahead-of-time Quasar
instrumentation of the compiled class files.

Usage
=====

Configure the plugin to instrument the code:

```xml
<plugin>
    <groupId>com.vlkan</groupId>
    <artifactId>quasar-maven-plugin</artifactId>
    <version>0.7.9</version>
    <configuration>
        <check>true</check>
        <debug>true</debug>
        <verbose>true</verbose>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>instrument</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Contributors
============

- [Stéphane Épardaud](https://github.com/FroMage)

License
=======

Copyright &copy; 2014-2017 [Volkan Yazıcı](http://vlkan.com/)

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
THE POSSIBILITY OF SUCH DAMAGE.
