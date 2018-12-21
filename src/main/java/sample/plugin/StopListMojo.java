package sample.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class StopListMojo extends AbstractMojo {

    @Parameter(property = "check.exclude")
    @Getter
    private String[] exclude = {};

    @Parameter(property = "check.test")
    @Getter
    private String test;

    @Getter
    @Parameter
    private List<StopListWordParam> stopWords = new ArrayList<>();


    private boolean hasErrors = false;

    public void execute() throws MojoExecutionException {
        checkInDirectory(new File("."));

        if (hasErrors) {
            throw new MojoExecutionException("Stop-words found in source!");
        }
    }

    private void checkInDirectory(File directory) {
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
        checkFileName(file);

        if (!file.isDirectory()) {
            checkFileContent(file);
        }
    }

    private void checkFileName(File file) {
        for (StopListWordParam stopListWordParam : stopWords) {
            String searchingWord = stopListWordParam.getStopWord();

            if (file.getName().contains(searchingWord)) {
                String entityType = file.isDirectory() ? "Directory" : "File";
                handleError(
                        stopListWordParam,
                        String.format("%s name contains stop-word '%s', path: %s", entityType, searchingWord, file.getPath())
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

                line = line.toLowerCase();
                for (StopListWordParam stopListWordParam : stopWords) {
                    String searchingWord = stopListWordParam.getStopWord();

                    if (line.contains(searchingWord.toLowerCase())) {
                        handleError(
                                stopListWordParam,
                                String.format("File contains stop-word '%s', path: %s, line %s", searchingWord, file.getPath(), lineNum)
                        );
                    }
                }
            }
        } catch (FileNotFoundException e) {
            getLog().error(String.format("File with path %s wasn't found", file.getPath()));
        }
    }

    private boolean isFileExcluded(File file) {
        List<String> excludeFolders = Arrays.asList(exclude);
        String filePath = file.getPath() + File.pathSeparator + file.getName();

        return excludeFolders.contains(file.getPath()) || excludeFolders.contains(filePath);
    }

    private void handleError(StopListWordParam wordParam, String errorMsg) {
        if (wordParam.getLevel().equals(StopListWordParam.ErrorLevel.WARN)) {
            getLog().warn(errorMsg);
        } else {
            getLog().error(errorMsg);
            hasErrors = true;
        }
    }
}
