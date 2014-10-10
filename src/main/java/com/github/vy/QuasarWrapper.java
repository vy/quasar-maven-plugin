package com.github.vy;

import co.paralleluniverse.fibers.instrument.Log;
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class QuasarWrapper {

    protected final ClassLoader cl;
    protected final SuspendableClassifier classifier;
    protected final MethodDatabase db;
    protected final AbstractMojo mojo;
    protected final QuasarInstrumentor instrumentor;

    public QuasarWrapper(
            final AbstractMojo mojo,
            boolean check,
            boolean verbose,
            boolean debug,
            boolean allowMonitors,
            boolean allowBlocking) {
        this.cl = getClass().getClassLoader();
        this.classifier = new SimpleSuspendableClassifier(cl);
        this.db = new MethodDatabase(cl, classifier);
        this.mojo = mojo;
        this.instrumentor = new QuasarInstrumentor(true, cl, classifier);
        instrumentor.setCheck(check);
        instrumentor.setVerbose(verbose);
        instrumentor.setDebug(debug);
        instrumentor.setAllowBlocking(allowBlocking);
        instrumentor.setAllowMonitors(allowMonitors);
        this.instrumentor.setLog(new Log() {

            @Override
            public void log(LogLevel level, String msg, Object... args) {
                final String message = String.format(msg, args);
                switch (level) {
                    case DEBUG:
                        mojo.getLog().debug(message);
                        break;
                    case INFO:
                        mojo.getLog().info(message);
                        break;
                    case WARNING:
                        mojo.getLog().warn(message);
                        break;
                    default:
                        mojo.getLog().error(message);
                        break;
                }
            }

            @Override
            public void error(String s, Exception e) {
                mojo.getLog().error(s, e);
            }

        });
    }

    private void logInfo(String fmt, Object... args) {
        mojo.getLog().info(String.format(fmt, args));
    }

    public void checkClass(File file) {
        logInfo("Checking class: %s", file.toPath());
        instrumentor.checkClass(file);
    }

    public void instrumentClasses() throws MojoExecutionException {
        logInfo("Instrumenting %d classes...", instrumentor.getWorkList().size());
        for (MethodDatabase.WorkListEntry wle : instrumentor.getWorkList())
            instrumentClass(wle);
    }

    private void instrumentClass(MethodDatabase.WorkListEntry entry)
            throws MojoExecutionException {
        if (!instrumentor.shouldInstrument(entry.name)) return;
        try {
            try (FileInputStream fis = new FileInputStream(entry.file)) {
                String className = entry.name.replace('.', '/');
                byte[] newClass = instrumentor.instrumentClass(className, fis);
                try (FileOutputStream fos = new FileOutputStream(entry.file)) {
                    fos.write(newClass);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Instrumenting file " + entry.file, ex);
        }
    }

}
