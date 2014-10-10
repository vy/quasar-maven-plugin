[Quasar](http://docs.paralleluniverse.co/quasar/) is a Java library that
provides high-performance lightweight threads, Go-like channels, Erlang-like
actors, and other asynchronous programming tools. Quasar fibers rely on bytecode
instrumentation. This can be done at classloading time via a Java Agent, or at
compilation time. This project ships a Maven plugin for the ahead-of-time Quasar
instrumentation of the compiled class files.
