package com.vlkan.maven.plugins.quasar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import co.paralleluniverse.fibers.instrument.Log;
import co.paralleluniverse.fibers.instrument.LogLevel;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;

/**
 * Quasar Ahead-of-Time instrumentor Mojo.
 */
@Mojo(name = "instrument", 
    defaultPhase = LifecyclePhase.PROCESS_CLASSES, 
    requiresDependencyResolution = ResolutionScope.COMPILE)
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
     * include base packages
     */
    @Parameter
    protected String[] inclusions;

    /**
     * Enable to allow the use known blocking calls like Thread.sleep, Object.wait etc.
     */
    @Parameter
    protected boolean allowBlocking = false;

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Instrumenting Quasar classes...");

        if (buildDirectory == null || !buildDirectory.isDirectory())
            throw new MojoExecutionException("Invalid build directory: " + buildDirectory);

        // Create a Quasar instrumentor.
        final QuasarInstrumentor instrumentor;
        final ClassLoader cl;
        try {
            List<String> elements = project.getTestClasspathElements();
            URL[] urls = new URL[elements.size()];
            for(int i=0;i<elements.size();i++){
                urls[i] = new File(elements.get(i)).toURI().toURL();
            }
            cl = new URLClassLoader(urls, getClass().getClassLoader());
            instrumentor = new QuasarInstrumentor(true);
        } catch (MalformedURLException | DependencyResolutionRequiredException error) {
            throw new AssertionError(error);
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
            public void error(String s, Throwable e) {
                getLog().error(s, e);
            }

        });

        // Traverse the build directory and feed the class files to the instrumentor.
        final Queue<File> files = new LinkedList<>(Collections.singleton(buildDirectory));
        Map<String, File> classes = new HashMap<>();
        while (!files.isEmpty()) {
            File file = files.poll();
            if (file.isDirectory()) {
                File[] dirFiles = file.listFiles();
                if (dirFiles != null)
                    Collections.addAll(files, dirFiles);
            } else if (file.getName().endsWith(".class")){
                String checkedClass = instrumentor.getMethodDatabase(cl).checkClass(file);
                if(checkedClass != null)
                    classes.put(checkedClass, file);
            }
        }

        instrumentClasses(instrumentor, cl, classes);
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

    private void instrumentClasses(QuasarInstrumentor instrumentor, ClassLoader cl, Map<String, File> classes) throws MojoExecutionException {
        logInfo("Instrumenting %d classes...", classes.size());
        for (Entry<String, File> entry : classes.entrySet())
            instrumentClass(instrumentor, cl, entry.getKey(), entry.getValue());
    }

    private void instrumentClass(QuasarInstrumentor instrumentor, ClassLoader cl, String name, File file) throws MojoExecutionException {
        boolean matchInclude = false;
        if (inclusions != null) {
          for (String s : inclusions) {
            if (name.startsWith(s.replace(".", "/"))) {
              matchInclude = true;
              break;
            }
          }
        }
        if (!matchInclude) {
          logInfo("class %s was excluded. ", name);
          return;
        }
        if (!instrumentor.shouldInstrument(name))
            return;
        try {
            try (FileInputStream fis = new FileInputStream(file)) {
                String className = name.replace('.', '/');
                byte[] newClass = instrumentor.instrumentClass(cl, className, fis);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(newClass);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Instrumenting file " + file, ex);
        }
    }

}
