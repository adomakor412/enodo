package com.headstartech.csv;

import com.google.common.io.Files;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.cli.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;


@SpringBootApplication
public class Application implements CommandLineRunner {

    private static Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder()
                .showBanner(false)
                .logStartupInfo(false)
                .sources(Application.class)
                .run(args);
    }

	public void run(String... args) throws IOException, IllegalAccessException, InstantiationException, ParseException {

        Options options = new Options();
        options.addOption("h", false, "Print this message");

        CommandLineParser parser = new ExtendedPosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            printHelp(options);
            System.exit(0);
        }

        File groovyScript = new File(args[0]);
        log.info("Loading Groovy script: file={}", groovyScript.getAbsolutePath());

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(groovyScript);
        Object groovyObject = clazz.newInstance();
        if(!(groovyObject instanceof CSVAnalyzer)) {
            log.info("Groovy class must implement {}", CSVAnalyzer.class.getSimpleName());
            return;
        }

        CSVAnalyzer analyzer = (CSVAnalyzer) groovyObject;

        List<File> inputFiles = collectFiles(args[1]);

        for(File inputFile : inputFiles) {
            processFile(inputFile, Charset.defaultCharset(), new CSVProcessor(',', analyzer));
        }

        File report = new File("/tmp/a.out");

        log.info("Writing report to {}", report.getAbsoluteFile());
        PrintWriter pw = new PrintWriter(report);
        analyzer.printResult(pw);
        pw.flush();
        pw.close();
	}

    /**
     * Posix parser ignoring unknown options to let Spring boot handle them.
     *
     */
    private static class ExtendedPosixParser extends PosixParser {

        @Override
        protected void processOption(final String arg, final ListIterator iter) throws ParseException {
            boolean hasOption = getOptions().hasOption(arg);

            if (hasOption) {
                super.processOption(arg, iter);
            }
        }

    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("java -jar csv-analyzer.jar", options);
    }

    private void processFile(File f, Charset charset, CSVProcessor processor) throws IOException {
        log.info("Processing {}", f.getAbsolutePath());
        Files.readLines(f, charset, processor);
    }

    private List<File> collectFiles(String path) {
        File f = new File(path);

        List<File> res = new ArrayList<File>();
        if(f.isDirectory()) {
            String[] files = f.list();
            for(String name : files) {
                res.add(new File(f, name));
            }
        } else {
            File parent = f.getParentFile();
            if(parent == null) {
                parent = new File(System.getProperty("user.dir"));
            }
            String[] matchingFilenames = parent.list(new WildcardFileFilter(f.getName()));
            for(String name : matchingFilenames) {
                res.add(new File(f.getParentFile(), name));
            }
        }

        Collections.sort(res);
        return res;
    }


}
