[Quasar](http://docs.paralleluniverse.co/quasar/) is a Java library that
provides high-performance lightweight threads, Go-like channels, Erlang-like
actors, and other asynchronous programming tools. Quasar fibers rely on bytecode
instrumentation. This can be done at classloading time via a Java Agent, or at
compilation time. This project ships a Maven plugin for the ahead-of-time Quasar
instrumentation of the compiled class files.

Usage
=====

Add the following Maven dependency to your POM file:

    <dependency>
        <groupId>com.github.vy</groupId>
        <artifactId>quasar-maven-plugin</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>

Configure the plugin to instrument the code:

    <plugin>
        <groupId>com.github.vy</groupId>
        <artifactId>quasar-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
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

License
=======

This project is licensed under
[The BSD 3-Clause License](http://opensource.org/licenses/BSD-3-Clause).
