package sample.plugin;

import org.apache.maven.plugin.logging.Log;

import java.io.File;

/**
 * Interface for plugin caching
 */
public interface StopListCache {

    /**
     * Adds file to cache
     *
     * @param file to add
     * @return is file was success added to cache
     */
    boolean addToCache(File file);

    /**
     * Sets apache plugin logger
     *
     * @param logger to set
     */
    void setLogger(Log logger);

    /**
     * Determinate is file was changed
     *
     * @param file to check
     * @return boolean result
     */
    boolean isFileChanged(File file);

    /**
     * Runs before plugin start to work
     */
    default void beforeClose() {
    }

    /**
     * Runs before plugin finish to work
     */
    default void beforeStart() {
    }
}
