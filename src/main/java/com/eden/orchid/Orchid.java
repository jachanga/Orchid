package com.eden.orchid;

import com.caseyjbrooks.clog.Clog;
import com.eden.common.json.JSONElement;
import com.eden.common.util.EdenUtils;
import com.eden.orchid.generators.SiteGenerators;
import com.eden.orchid.options.SiteOptions;
import com.eden.orchid.programs.SitePrograms;
import com.eden.orchid.resources.OrchidResources;
import com.eden.orchid.resources.impl.OrchidFileResources;
import com.eden.orchid.utilities.AutoRegister;
import com.eden.orchid.utilities.RegistrationProvider;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: Create a self-start method so that it can be run from within a server. Think: always up-to-date documentation at the same url (www.myjavadocs.com/latest) running from JavaEE, Spring, even Ruby on Rails with JRuby!

/**
 * Orchid is a modern theme-ready Javadoc generator with a plugin-driven architecture. Orchid not only generates
 * documentation for your Java library, but creates your entire documentation site, and by being simple to plug in
 * new generators and templates, you can make your site exactly what you want it to be.
 * <p>
 * This class defines the process of site generation, which is shown below. It also holds the root of all global data
 * gathered during the generation process and provides access to the chosen theme.
 * <p>
 * <b>Generation Procedure:</b><br>
 * <ol>
 *    <li>Scan the classpath and register all plugins. Every component of the site is built as a plugin, and new plugins
 *    will be discovered simply by adding them to the classpath and giving the class the <code>@AutoRegister</code> annotation</li>
 *    <li>Parse all command-line options. Options are specified by implementing the Option interface, and are used in order
 *    of priority from highest to lowest. To get your Option to run earlier in the process, give it a higher priority. </li>
 *    <li>Perform a sanity check to make sure we have declared all necessary information. This includes a output directory
 *    (-d), a valid Theme (-theme), and anything the Theme required. This means ensuring that any Compilers the Theme
 *    depends on exist on the classpath, and also that any options required by the theme haev been provided.</li>
 *    <li>Index all generators. At this step, generators shouldn't be writing any files, but they need to provide the
 *    file paths of files they <i>will eventually write</i> so that other generators can link to what it generates.
 *    Generators are run in order of priority from highest to lowest.</li>
 *    <li>run through all generators and allow them to write their content to file. Files should be written into the
 *    output directory and should make use of data gathered from Options and during the indexing step to make sure
 *    everything in the site is discoverable at any point. The content of the files written should be compiled using
 *    the declared Theme, which will find the specific Compiler to use given the file's extension.</li>
 *    <li>Go to the Theme to generate the site's homepage. This is the index.html file that is the entry point to
 *    your site. </li>
 * </ol>
 *
 */
public final class Orchid {

// Doclet hackery to allow this to parse documentation as expected and not crash...
//----------------------------------------------------------------------------------------------------------------------

    /**
     * Get the number of arguments that a given option expects from the command line. This number includes the option
     * itself: for example '-d /output/javadoc' should return 2.
     *
     * @param option  the option to parse
     * @return  the number of arguments it expects from the command line
     */
    public static int optionLength(String option) {
        return SiteOptions.optionLength(option);
    }

    /**
     * NOTE: Without this method present and returning LanguageVersion.JAVA_1_5,
     *       Javadoc will not process generics because it assumes LanguageVersion.JAVA_1_1
     * @return language version (hard coded to LanguageVersion.JAVA_1_5)
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

// Main Doclet
//----------------------------------------------------------------------------------------------------------------------

    private static JSONObject root;

    private static Theme theme;

    private static RootDoc rootDoc;

    private static OrchidResources resources;

    /**
     * Start the site generation process from the command line
     *
     * @param args  the command-line args to use
     */
    public static void main(String[] args) {
        Clog.i("Using Orchid from Main method");

        String program;
        if(!args[0].startsWith("-")) {
            program = args[0];
        }
        else {
            program = SitePrograms.defaultProgram;
        }

        Orchid.providers = new ArrayList<>();
        Orchid.resources = new OrchidFileResources();
        Orchid.rootDoc = null;
        Orchid.root = new JSONObject();

        Map<String, String[]> optionsMap = new HashMap<>();

        for(String arg : args) {
            Clog.v("Arg: #{$1}", new Object[]{ arg });
            if(arg.startsWith("-")) {
                String[] argPieces = arg.split("\\s+");
                optionsMap.put(argPieces[0], argPieces);
            }
        }

        bootstrap(optionsMap);

        if(shouldContinue()) {
            kernel(program);
            System.exit(0);
        }
        else {
            System.exit(1);
        }
    }

    /**
     * Start the site generation process from the Javadoc tool
     *
     * @param rootDoc  the root of the project to generate sources for
     * @return Whether the generation was successful
     */
    public static boolean start(RootDoc rootDoc) {
        Clog.i("Using Orchid from Javadoc Start method");

        Orchid.providers = new ArrayList<>();
        Orchid.resources = new OrchidFileResources();
        Orchid.rootDoc = rootDoc;
        Orchid.root = new JSONObject();

        Map<String, String[]> optionsMap = new HashMap<>();
        for (String[] a : rootDoc.options()) {
            optionsMap.put(a[0], a);
        }

        bootstrap(optionsMap);

        if(shouldContinue()) {
            kernel(SitePrograms.defaultProgram);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Bootstraps the site generation process given a map of options
     *
     * @param optionsMap
     * @return whether the generation process was successful
     */
    public static void bootstrap(Map<String, String[]> optionsMap) {
        providerScan();
        pluginScan();
        optionsScan(optionsMap);
    }

    public static void kernel(String programName) {
        SitePrograms.runProgram(programName);
    }

    private static List<RegistrationProvider> providers;
    private static void providerScan() {
        FastClasspathScanner scanner = new FastClasspathScanner();
        scanner.matchClassesImplementing(RegistrationProvider.class, (matchingClass) -> {
            try {
                Clog.d("Initializing provider class: #{$1}", new Object[]{matchingClass.getSimpleName()});
                RegistrationProvider instance = matchingClass.newInstance();
                providers.add(instance);
            }
            catch (IllegalAccessException|InstantiationException e) {
                e.printStackTrace();
            }
        });
        scanner.scan();
    }

    /**
     * Step one: scan the classpath for all classes tagged with @AutoRegister and register them according to their type.
     *
     * If you need to register plugins that are not one of the classes or interfaces defined in Orchid Core, you can
     * register it within a static initializer. Every AutoRegister annotated class has an instance created by its no-arg
     * constructor, so you are guaranteed to run the static initializer of any AutoRegister annotated class.
     */
    private static void pluginScan() {
        FastClasspathScanner scanner = new FastClasspathScanner();
        scanner.matchClassesWithAnnotation(AutoRegister.class, (matchingClass) -> {
            try {
                Clog.d("AutoRegistering class: #{$1}", new Object[]{matchingClass.getSimpleName()});
                Object instance = matchingClass.newInstance();
                for(RegistrationProvider provider : providers) {
                    provider.register(instance);
                }
            }
            catch (IllegalAccessException|InstantiationException e) {
                e.printStackTrace();
            }
        });
        scanner.scan();
    }

    /**
     * Step two: parse all command-line args using the registered Options.
     */
    private static void optionsScan(Map<String, String[]> optionsMap) {
        root.put("options", new JSONObject());
        SiteOptions.parseOptions(optionsMap, root.getJSONObject("options"));
    }

    /**
     * Step three: perform a sanity-check to make sure all required site components have been set.
     */
    private static boolean shouldContinue() {
        boolean shouldContinue = true;

        if(EdenUtils.isEmpty(query("options.d"))) {
            Clog.e("You MUST define an output directory with the '-d' flag. It should be an absolute directory which will contain all generated files.");
            shouldContinue = false;
        }

        if(theme == null) {
            Clog.e("You MUST define a theme with the '-theme` flag. It should be the fully-qualified class name of the desired theme.");
            shouldContinue = false;
        }
        else {
            Clog.i("Using Theme class #{$1}", new Object[] {theme.getClass().getName()});

            if(!EdenUtils.isEmpty(theme.getMissingOptions())) {
                Clog.e("Your selected theme depends on the following command line options that could not be found: -#{$1 | join(', -') }.", new Object[] {theme.getMissingOptions()});
                shouldContinue = false;
            }
        }

        if(EdenUtils.isEmpty(query("options.resourcesDir"))) {
            Clog.w("You should consider defining source resources with the '-resourcesDir' flag to customize the final styling or add additional content to your Javadoc site. It should be the absolute path to a folder containing your custom resources.");
        }

        return shouldContinue;
    }

    /**
     * Step four: scan all registered generators and index all discovered components. No content should be written at
     * this point, we are just gathering the references to files that will be written, so that when we start writing
     * files we can be sure we are able to generate links to any other piece of generated content.
     */
    public static void indexingScan() {
        root.put("index", new JSONObject());
        SiteGenerators.startIndexing(root.getJSONObject("index"));
    }

    /**
     * Step five: scan all registered generators and generate the final output files. At this point, any file that will
     * be generated should be able to be linked to finding its location within the index.
     */
    public static void generationScan() {
        SiteGenerators.startGeneration();
    }

    /**
     * Step six: generate the final site homepage
     */
    public static void generateHomepage() {
        root.put("root", new JSONObject(root.toMap()));
        theme.generateHomepage(root);
    }

    /**
     * Query the gathered site data using a javascript-like syntax, or the native JSONObject query syntax. For example,
     * given a JSONObject initialized with this document:
     * <pre>
     * {
     *   "a": {
     *     "b": "c"
     *   }
     * }
     * </pre>
     * and this JSONPointer string:
     * <pre>
     * "/a/b"
     * </pre>
     * or this Javascript pointer string:
     * <pre>
     * "a.b"
     * </pre>
     * Then this method will return the String "c".
     * In the end, the Javascript syntax is converted to the corresponding JSONPointer syntax and queried.
     *
     * @param pointer  string that can be used to create a JSONPointer
     * @return  the item matched by the JSONPointer, otherwise null
     */
    public static JSONElement query(String pointer) {
        return new JSONElement(root).query(pointer);
    }

    /**
     * Gets the root JSONObject of all data gathered. In most cases, it is preferable to just query for the data you
     * need with the Orchid#query() metho.
     *
     * @return  the root JSONObject
     */
    public static JSONObject getRoot() {
        return root;
    }

    /**
     * Gets the currenty set theme
     *
     * @return  the currenty set theme
     */
    public static Theme getTheme() {
        return theme;
    }

    /**
     * Sets the theme to use for the site generation process.
     *
     * @param theme  the theme to set
     */
    public static void setTheme(Theme theme) {
        Orchid.theme = theme;
    }

    /**
     * Gets the project-specified Resources implementation.
     *
     * @return  the project's resources implementation
     */
    public static OrchidResources getResources() {
        return resources;
    }

    public static void setResources(OrchidResources resources) {
        Orchid.resources = resources;
    }

    /**
     * Set the root JSONObject used in the project.
     *
     * @param root  the JSONObject to set
     */
    public static void setRoot(JSONObject root) {
        Orchid.root = root;
    }

    /**
     * Set the RootDoc used to parse Javadoc comments, methods, fields, etc.
     *
     * @param rootDoc  the RootDoc to set
     */
    public static void setRootDoc(RootDoc rootDoc) {
        Orchid.rootDoc = rootDoc;
    }

    /**
     * Searches for a Class in the RootDoc given its fully-qualified classname.
     *
     * @param className  the fully-qualified name of a class to find
     * @return  its corresponding ClassDoc if it was found, null otherwise
     */
    public static ClassDoc findClass(String className) {
        return rootDoc.classNamed(className);
    }

    /**
     * Gets the RootDoc to parse Javadoc from.
     *
     * @return  the RootDoc passed into the start method
     */
    public static RootDoc getRootDoc() {
        return rootDoc;
    }
}


/*

Classes that would be good to inject as dependencies:

* Theme
* Orchid
* RootDoc
* OrchidResources

 */