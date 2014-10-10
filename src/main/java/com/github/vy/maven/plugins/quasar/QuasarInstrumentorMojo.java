package com.github.vy.maven.plugins.quasar;

import co.paralleluniverse.fibers.instrument.DefaultSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.Log;
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Quasar Ahead-of-Time instrumentor Mojo.
 */
@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class QuasarInstrumentorMojo extends AbstractMojo {
    /**
     * Build directory path.
     */
    @Parameter(property = "project.build.directory", required = true)
    protected File buildDirectory;

    /**
     * Enable to run the resulting code through a verifier.
     */
    @Parameter
    protected boolean check = false;

    /**
     * Enable to display the name of each processed class and all suspendable method calls.
     */
    @Parameter
    protected boolean verbose = false;

    /**
     * Enable to print internal debugging information.
     */
    @Parameter
    protected boolean debug = false;

    /**
     * Enable to allow the use of synchronized statements -- this is DANGEROUS!
     */
    @Parameter
    protected boolean allowMonitors = false;

    /**
     * Enable to allow the use known blocking calls like Thread.sleep, Object.wait etc.
     */
    @Parameter
    protected boolean allowBlocking = false;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Instrumenting Quasar classes...");

        if (buildDirectory == null || !buildDirectory.isDirectory())
            throw new MojoExecutionException("Invalid build directory: " + buildDirectory);

        // Create a Quasar instrumentor.
        final QuasarInstrumentor instrumentor;
        try {
            final ClassLoader cl = new URLClassLoader(new URL[]{buildDirectory.toURI().toURL()}, getClass().getClassLoader());
            instrumentor = new QuasarInstrumentor(true, cl, new DefaultSuspendableClassifier(cl));
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }

        instrumentor.setCheck(check);
        instrumentor.setVerbose(verbose);
        instrumentor.setDebug(debug);
        instrumentor.setAllowBlocking(allowBlocking);
        instrumentor.setAllowMonitors(allowMonitors);
        instrumentor.setLog(new Log() {

            @Override
            public void log(LogLevel level, String msg, Object... args) {
                final String message = String.format(msg, args);
                switch (level) {
                    case DEBUG:
                        getLog().debug(message);
                        break;
                    case INFO:
                        getLog().info(message);
                        break;
                    case WARNING:
                        getLog().warn(message);
                        break;
                    default:
                        getLog().error(message);
                        break;
                }
            }

            @Override
            public void error(String s, Exception e) {
                getLog().error(s, e);
            }

        });

        // Traverse the build directory and feed the class files to the instrumentor.
        final Queue<File> files = new LinkedList<>(Collections.singleton(buildDirectory));
        while (!files.isEmpty()) {
            File file = files.poll();
            if (file.isDirectory()) {
                File[] dirFiles = file.listFiles();
                if (dirFiles != null)
                    Collections.addAll(files, dirFiles);
            } else if (file.getName().endsWith(".class"))
                instrumentor.checkClass(file);
        }

        instrumentClasses(instrumentor);
    }

    protected void logDebug(String fmt, Object... args) {
        getLog().debug(String.format(fmt, args));
    }

    protected void logInfo(String fmt, Object... args) {
        getLog().info(String.format(fmt, args));
    }

    protected void logWarn(String fmt, Object... args) {
        getLog().warn(String.format(fmt, args));
    }

    protected void logError(String s, Exception e) {
        getLog().error(s, e);
    }

    public void instrumentClasses(QuasarInstrumentor instrumentor) throws MojoExecutionException {
        logInfo("Instrumenting %d classes...", instrumentor.getWorkList().size());
        for (MethodDatabase.WorkListEntry wle : instrumentor.getWorkList())
            instrumentClass(instrumentor, wle);
    }

    private void instrumentClass(QuasarInstrumentor instrumentor, MethodDatabase.WorkListEntry entry) throws MojoExecutionException {
        if (!instrumentor.shouldInstrument(entry.name))
            return;
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
