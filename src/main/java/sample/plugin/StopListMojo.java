package sample.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Plugin for search and react on stop-words in project
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.COMPILE)
public class StopListMojo extends AbstractMojo {

    /**
     * List of files/folders where search will be excluded
     */
    @Parameter(property = "excludes")
    private List<String> excludes = new ArrayList<>();

    /**
     * List of cached canonical paths of excludes param
     */
    private List<String> excludeCanonical = new ArrayList<>();

    /**
     * List of stop-words to search for
     */
    @Parameter(property = "stopWords", required = true)
    private List<String> stopWords = new ArrayList<>();

    /**
     * Error level of plugin.
     * Acceptable values is "WARN" and "ERROR"
     * <p>
     * On "WARN" plugin is just log if stop-words was found
     * On "ERROR" plugin is breaking the build
     */
    @Parameter(defaultValue = "WARN", property = "errorLevel")
    String errorLevel;

    /**
     * Should or not case of stop words will be ignored
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreCase;

    /**
     * If errors was found build will be breaking
     */
    private boolean hasErrors = false;

    @Inject
    private StopListCache cache;

    @Override
    public void execute() throws MojoExecutionException {
        init();
        checkInDirectory(new File("."));

        beforeClose();
        if (hasErrors) {
            throw new MojoExecutionException("Stop-words found in source!");
        }
    }

    /**
     * Runs in start of plugin work
     */
    private void init() {
        initCanonicalPaths();
        cache.setLogger(getLog());
        cache.beforeStart();
    }

    /**
     * Runs before plugin finish work
     */
    private void beforeClose() {
        cache.beforeClose();
    }

    /**
     * Fill exclude canonical paths
     */
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

    /**
     * Recursive search in directory and files
     *
     * @param directory where search to start
     */
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

    /**
     * Checks file for stop-words
     *
     * @param file to check
     */
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

    /**
     * Check file name for stop-words
     *
     * @param file to check
     */
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

    /**
     * Check file content for stop-words
     *
     * @param file to check
     */
    private void checkFileContent(File file) {
        if (!cache.isFileChanged(file)) {
            getLog().debug("file wasn't change " + file.getAbsolutePath());
            return;
        }

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

            cache.addToCache(file);
        } catch (FileNotFoundException e) {
            getLog().error(String.format("File with path %s wasn't found", file.getPath()));
        }
    }

    /**
     * Checks is file should be excluded for checking
     *
     * @param file to check
     * @return boolean result
     */
    private boolean isFileExcluded(File file) {
        try {
            String canonicalFilePath = file.getCanonicalPath();
            return excludeCanonical.contains(canonicalFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handle stop word detecting
     *
     * @param errorMsg - error message to work with
     */
    private void handleStopWordDetected(String errorMsg) {
        if (errorLevel.equals("WARN")) {
            getLog().warn(errorMsg);
        } else if (errorLevel.equals("ERROR")) {
            getLog().error(errorMsg);
            hasErrors = true;
        }
    }

    /**
     * Handle no-access file situation
     *
     * @param file with no-access
     */
    private void handleNoAccess(File file) {
        getLog().warn("No access to read " + file.getAbsolutePath());
    }
}
