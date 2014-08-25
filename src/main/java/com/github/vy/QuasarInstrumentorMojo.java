package com.github.vy;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Quasar Ahead-of-Time instrumentor Mojo.
 */
@Mojo(name = "quasar", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
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
     * Enable to display the name of each processed class and all suspendable method calles.
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
        QuasarWrapper quasar = new QuasarWrapper(
                this, check, verbose, debug, allowMonitors, allowBlocking);

        // Traverse the build directory and feed the class files to the instrumentor.
        Queue<File> files = new LinkedList<>(Collections.singleton(buildDirectory));
        while (!files.isEmpty()) {
            File file = files.poll();
            if (file.isDirectory()) {
                File[] dirFiles = file.listFiles();
                if (dirFiles != null)
                    Collections.addAll(files, dirFiles);
            }
            else if (file.getName().endsWith(".class")) quasar.checkClass(file);
        }

        // Let the instrumentor kick in.
        quasar.instrumentClasses();
    }

}
