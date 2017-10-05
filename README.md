[Quasar](http://docs.paralleluniverse.co/quasar/) is a Java library that
provides high-performance lightweight threads, Go-like channels, Erlang-like
actors, and other asynchronous programming tools. Quasar fibers rely on bytecode
instrumentation. This can be done at classloading time via a Java Agent, or at
compilation time. This project ships a Maven plugin for the ahead-of-time Quasar
instrumentation of the compiled class files.

Usage
=====

Configure the plugin to instrument the code:

    <plugin>
        <groupId>com.vlkan</groupId>
        <artifactId>quasar-maven-plugin</artifactId>
        <version>0.7.5</version>
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

Contributors
============

- [Stéphane Épardaud](https://github.com/FroMage)

License
=======

This project is licensed under
[The BSD 3-Clause License](http://opensource.org/licenses/BSD-3-Clause).
