package com.github.vy;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.instrument.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

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

    protected void logDebug(String fmt, Object... args) {
        mojo.getLog().debug(String.format(fmt, args));
    }

    protected void logInfo(String fmt, Object... args) {
        mojo.getLog().info(String.format(fmt, args));
    }

    protected void logWarn(String fmt, Object... args) {
        mojo.getLog().warn(String.format(fmt, args));
    }

    protected void logError(String s, Exception e) {
        mojo.getLog().error(s, e);
    }

    public void checkClass(File file) {
        instrumentor.checkClass(file);
    }

    public void instrumentClasses() throws MojoExecutionException {
        logInfo("Instrumenting %d classes...", instrumentor.getWorkList().size());
        for (MethodDatabase.WorkListEntry wle : instrumentor.getWorkList())
            instrumentClass(wle);
    }

    private void instrumentClass(MethodDatabase.WorkListEntry entry)
            throws MojoExecutionException {
        if (!shouldInstrument(entry.name)) return;
        try {
            try (FileInputStream fis = new FileInputStream(entry.file)) {
                String className = entry.name.replace('.', '/');
                byte[] newClass = instrumentClass(className, new ClassReader(fis));
                try (FileOutputStream fos = new FileOutputStream(entry.file)) {
                    fos.write(newClass);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Instrumenting file " + entry.file, ex);
        }
    }

    /**
     * Copied from Quasar sources for {@code shouldInstrument(String)}.
     * @see <a href="https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/fibers/instrument/Classes.java">co.paralleluniverse.fibers.instrument.Classes</a>
     */
    private static class Classes {
        static final String FIBER_CLASS_NAME = "co/paralleluniverse/fibers/Fiber";
        static final String STACK_NAME = "co/paralleluniverse/fibers/Stack";
    }

    /**
     * Copied from Quasar sources.
     * @see <a href="https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/fibers/instrument/QuasarInstrumentor.java">co.paralleluniverse.fibers.instrument.QuasarInstrumentor</a>
     */
    private static boolean shouldInstrument(String className) {
        className = className.replace('.', '/');
        return (!(className.startsWith("co/paralleluniverse/fibers/instrument/") && !Debug.isUnitTest()) &&
                !className.startsWith("org/objectweb/asm/") &&
                !className.startsWith("org/netbeans/lib/") &&
                !(className.equals(Classes.FIBER_CLASS_NAME) || className.startsWith(Classes.FIBER_CLASS_NAME + '$')) &&
                !className.equals(Classes.STACK_NAME) &&
                !MethodDatabase.isJavaCore(className));
    }

    /**
     * Copied from Quasar sources.
     * @see <a href="https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/fibers/instrument/DBClassWriter.java">co.paralleluniverse.fibers.instrument.DBClassWriter</a>
     */
    private static class DBClassWriter extends ClassWriter {

        private final MethodDatabase db;

        public DBClassWriter(MethodDatabase db, ClassReader classReader) {
            super(classReader, COMPUTE_FRAMES | COMPUTE_MAXS);
            this.db = db;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return db.getCommonSuperClass(type1, type2);
        }

    }

    /**
     * Copied from Quasar sources.
     * @see <a href="https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/fibers/instrument/QuasarInstrumentor.java">co.paralleluniverse.fibers.instrument.QuasarInstrumentor</a>
     */
    private byte[] instrumentClass(String className, ClassReader r) {
        logInfo("TRANSFORM: %s %s", className,
                (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");
        ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = new CheckClassAdapter(cw);
        InstrumentClass ic = new InstrumentClass(cv, db, false);
        byte[] transformed;
        try {
            r.accept(ic, ClassReader.SKIP_FRAMES);
            transformed = cw.toByteArray();
        } catch (Exception e) {
            if (ic.hasSuspendableMethods()) {
                logError("Unable to instrument class " + className, e);
                throw e;
            } else {
                if (!MethodDatabase.isProblematicClass(className))
                    logDebug("Unable to instrument class " + className);
                return null;
            }
        }
        return transformed;
    }

}
