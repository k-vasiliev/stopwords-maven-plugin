package sample.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class StopListMojo extends AbstractMojo {

    @Parameter(property = "check.excludes")
    private List<String> excludes = new ArrayList<>();

    private List<String> excludeCanonical = new ArrayList<>();

    @Parameter
    private List<String> stopWords = new ArrayList<>();

    @Parameter(defaultValue = "WARN")
    String errorLevel;

    @Parameter(defaultValue = "false")
    private boolean ignoreCase;

    private boolean hasErrors = false;

    public void execute() throws MojoExecutionException {
        init();
        checkInDirectory(new File("."));

        if (hasErrors) {
            throw new MojoExecutionException("Stop-words found in source!");
        }
    }

    private void init() {
        initCanonicalPaths();
    }

    private void initCanonicalPaths() {
        for (String excludePath : excludes) {
            File file = new File(excludePath);
            if (!file.exists()) {
                continue;
            }

            try {
                excludeCanonical.add(file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkInDirectory(File directory) {
        if (!directory.canRead()) {
            handleNoAccess(directory);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (isFileExcluded(file)) {
                continue;
            }

            checkFile(file);
            if (file.isDirectory()) {
                checkInDirectory(file);
            }
        }
    }

    private void checkFile(File file) {
        if (!file.canRead()) {
            handleNoAccess(file);
            return;
        }

        checkFileName(file);

        if (!file.isDirectory()) {
            checkFileContent(file);
        }
    }

    private void checkFileName(File file) {
        String fileName = file.getName();
        if (ignoreCase) {
            fileName = fileName.toLowerCase();
        }

        for (String stopWord : stopWords) {
            if (ignoreCase) {
                stopWord = stopWord.toLowerCase();
            }
            if (fileName.contains(stopWord)) {
                String entityType = file.isDirectory() ? "Directory" : "File";
                handleStopWordDetected(
                        String.format("%s name contains stop-word '%s', path: %s", entityType, stopWord, file.getPath())
                );
            }
        }
    }

    private void checkFileContent(File file) {
        try {
            Scanner scanner = new Scanner(file);

            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;

                if (ignoreCase) {
                    line = line.toLowerCase();
                }

                for (String stopWord : stopWords) {
                    if (ignoreCase) {
                        stopWord = stopWord.toLowerCase();
                    }
                    if (line.contains(stopWord)) {
                        handleStopWordDetected(
                                String.format("File contains stop-word '%s', path: %s, line %s", stopWord, file.getPath(), lineNum)
                        );
                    }
                }
            }
        } catch (FileNotFoundException e) {
            getLog().error(String.format("File with path %s wasn't found", file.getPath()));
        }
    }

    private boolean isFileExcluded(File file) {
        try {
            String canonicalFilePath = file.getCanonicalPath();
            return excludeCanonical.contains(canonicalFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void handleStopWordDetected(String errorMsg) {
        if (errorLevel.equals("WARN")) {
            getLog().warn(errorMsg);
        } else if (errorLevel.equals("ERROR")) {
            getLog().error(errorMsg);
            hasErrors = true;
        }
    }

    private void handleNoAccess(File file) {
        getLog().warn("No access to read " + file.getAbsolutePath());
    }
}
