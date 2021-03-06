package net.hearthstats;

import net.hearthstats.config.Environment;
import net.hearthstats.config.OS;
import net.hearthstats.log.Log;
import net.hearthstats.updater.api.GitHubReleases;
import net.hearthstats.updater.api.model.Release;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.net.URI;


public final class Updater {
	private Updater() {} // never instantiated

  public static final String MANUAL_DOWNLOAD_URL = "http://hearthstats.net/uploader";

  private final static Logger debugLog = LoggerFactory.getLogger(Updater.class);

  private static Release cachedlatestRelease;


  public static Release getLatestRelease(Environment environment) {
    if (cachedlatestRelease == null) {
      try {
        debugLog.debug("Loading latest release information from GitHub");
        if (environment.os() == OS.OSX) {
          cachedlatestRelease = GitHubReleases.getLatestReleaseForOSX();
        } else if (environment.os() == OS.WINDOWS) {
          cachedlatestRelease = GitHubReleases.getLatestReleaseForWindows();
        }
        if (cachedlatestRelease == null) {
          Log.warn("Unable to check latest release of HearthStats Companion");
        }
      } catch (Exception e) {
        Log.warn("Unable to check latest release of HearthStats Companion due to error: " + e.getMessage(), e);
      }
    }
    return cachedlatestRelease;
  }


  public static void run(Environment environment, Release release) {
    Log.info("Extracting and running updater ...");

    String errorMessage = null;
    try {
      errorMessage = environment.performApplicationUpdate(release);
    } catch (Exception e) {
      debugLog.warn("Unable to run updater", e);
      if (errorMessage == null) {
        errorMessage = e.getMessage();
      }
    }

    if (errorMessage == null) {
      // There was no error, so the updater should be running now
      System.exit(0);

    } else {
      // There was an error
      Main.showMessageDialog(null, errorMessage + "\n\nYou will now be taken to the download page.");
      try {
        Desktop.getDesktop().browse(new URI(MANUAL_DOWNLOAD_URL));
      } catch (Exception e) {
        Main.showErrorDialog("Error launching browser with URL " + MANUAL_DOWNLOAD_URL, e);
      }
      Log.warn("Updater Error: " + errorMessage);
    }


  }


  public static void cleanUp(Environment environment) {
    removeFile(environment.extractionFolder(), "updater.jar");
    removeFile(environment.extractionFolder(), "update-*.zip");
	}


  /**
   * Deletes a file or files in the given path.
   * @param path The path where the file or files are located.
   * @param filename The file or files to delete. Accepts wildcards * and ?.
   */
	private static void removeFile(String path, String filename) {
    debugLog.debug("Deleting {}/{}", path, filename);
    File dir = new File(path);
    FileFilter fileFilter = new WildcardFileFilter(filename);
    File[] files = dir.listFiles(fileFilter);
    for (File file : files) {
      debugLog.info("Deleting file {}", file.getAbsolutePath());
      file.delete();
    }
	}

}
