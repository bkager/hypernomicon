/*
 * Copyright 2015-2023 Jason Winning
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hypernomicon.util.filePath;

import org.hypernomicon.model.Exceptions.HyperDataException;
import org.hypernomicon.fileManager.FileManager;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.util.Util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FilePath implements Comparable<FilePath>
{
  private final InnerFilePath innerVal;

  public FilePath(File file)      { innerVal = new InnerFilePath(file); }
  public FilePath(Path path)      { innerVal = new InnerFilePath(path); }
  public FilePath(String pathStr) { innerVal = new InnerFilePath(pathStr); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public File toFile()                  { return innerVal.getFile(); }
  public Path toPath()                  { return innerVal.getPath(); }
  public URI toURI()                    { return nullSwitch(toFile(), null, File::toURI); }
  public boolean exists()               { return toFile().exists(); }
  public long size() throws IOException { return Files.size(toPath()); }
  public boolean isFile()               { return toFile().isFile();  }
  public boolean isDirectory()          { return toFile().isDirectory(); }
  public FilePath getParent()           { return new FilePath(toPath().getParent()); }
  public Instant lastModified()         { return Instant.ofEpochMilli(toFile().lastModified()); }

  /**
   * Gets the extension of a filename.
   * <p>
   * This method returns the textual part of the filename after the last dot.
   * There must be no directory separator after the dot.
   * <pre>
   * foo.txt      --&gt; "txt"
   * a/b/c.jpg    --&gt; "jpg"
   * a/b.txt/c    --&gt; ""
   * a/b/c        --&gt; ""
   * </pre>
   * <p>
   * The output will be the same irrespective of the machine that the code is running on.
   *
   * @return the extension of the file or an empty string if none exists or {@code null}
   * if the filename is {@code null}.
   */
  public String getExtensionOnly()      { return FilenameUtils.getExtension(toString()); }

  public boolean copyTo(FilePath destFilePath, boolean confirmOverwrite) throws IOException { return moveOrCopy(destFilePath, confirmOverwrite, false); }
  public boolean moveTo(FilePath destFilePath, boolean confirmOverwrite) throws IOException { return moveOrCopy(destFilePath, confirmOverwrite, true); }

  public boolean renameTo(String newNameStr) throws IOException { return moveOrCopy(getDirOnly().resolve(newNameStr), false, true); }

  public static boolean isEmpty(FilePath filePath) { return (filePath == null) || safeStr(filePath.toString()).isEmpty(); }

  @Override public int hashCode()            { return innerVal.hashCode(); }
  @Override public String toString()         { return innerVal.getPathStr(); }
  @Override public int compareTo(FilePath o) { return toPath().compareTo(o.toPath()); }

  /**
   * If this file is a directory, will return just the directory name. If it is not a directory, will return just the file name.
   */
  public FilePath getNameOnly() { return new FilePath(FilenameUtils.getName(toString())); }

  /**
   * If this file is a directory, will return the entire path. If it is not a directory, will return the parent directory.
   */
  public FilePath getDirOnly() { return isDirectory() ? this : new FilePath(FilenameUtils.getFullPathNoEndSeparator(toString())); }

  /**
   * this = base, parameter = relative, output = resolved
   */
  public FilePath resolve(FilePath relativeFilePath) { return new FilePath(toPath().resolve(relativeFilePath.toPath())); }

  /**
   * this = base, parameter = relative, output = resolved
   */
  public FilePath resolve(String relativeStr) { return new FilePath(toPath().resolve(Paths.get(relativeStr.trim()))); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  /**
   * this = base, parameter = resolved, output = relative
   */
  public FilePath relativize(FilePath resolvedFilePath)
  {
    try { return new FilePath(toPath().relativize(resolvedFilePath.toPath())); }
    catch (IllegalArgumentException e) { return null; }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void delete(boolean noExistOK) throws IOException
  {
    if (noExistOK && (exists() == false)) return;

    boolean startWatcher = folderTreeWatcher.stop();

    Files.delete(toPath());

    fileManagerDlg.setNeedRefresh();

    if (startWatcher)
      folderTreeWatcher.createNewWatcherAndStart();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean deleteReturnsBoolean(boolean noExistOK) { return deleteReturnsBoolean(noExistOK, null); }

  private boolean deleteReturnsBoolean(boolean noExistOK, StringBuilder errorSB)
  {
    try { delete(noExistOK); }
    catch (IOException e)
    {
      if (errorSB != null) assignSB(errorSB, e.getMessage());
      return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean deletePromptOnFail(boolean noExistOK)
  {
    StringBuilder errorSB = new StringBuilder();

    while (deleteReturnsBoolean(noExistOK, errorSB) == false)
    {
      String msgStr = errorSB.length() > 0 ?
        "Attempt to delete file failed: \"" + errorSB + System.lineSeparator() + System.lineSeparator() + "Try again?"
      :
        "Attempt to delete file failed: \"" + this + "\". Try again?";

      if (confirmDialog(msgStr) == false)
        return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean equals(Object other)
  {
    if ((other instanceof FilePath) == false) return false;

    return innerVal.equals(((FilePath)other).innerVal);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean moveOrCopy(FilePath destFilePath, boolean confirmOverwrite, boolean move) throws IOException
  {
    if (equals(destFilePath))
      throw new IOException("Source file is the same as the destination file.");

    boolean startWatcher = folderTreeWatcher.stop();

    try
    {
      if (destFilePath.exists() && confirmOverwrite)
      {
        if (confirmDialog("Destination file exists. Overwrite?") == false)
          return false;

        if (destFilePath.toFile().delete() == false)
          return falseWithErrorMessage("Unable to delete the file.");
      }

      if (move)
      {
        if (isFile() && getParent().equals(db.unenteredPath()))
          ui.notifyOfImport(this);

        Files.move(toPath(), destFilePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      else
        Files.copy(toPath(), destFilePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    finally
    {
      if (startWatcher)
        folderTreeWatcher.createNewWatcherAndStart();

      nullSwitch(fileManagerDlg, FileManager::setNeedRefresh);
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static boolean isFilenameValid(String fileName)
  {
    fileName = safeStr(fileName);
    if (ultraTrim(fileName).isEmpty()) return false;

    try
    {
      Path path = Paths.get(fileName);

      if (fileName.equals(path.normalize().toString()) == false)
        return false;

      Files.getLastModifiedTime(path);
    }
    catch (NoSuchFileException e)
    {
      return true;
    }
    catch (IOException e)
    {
      return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static String removeInvalidFileNameChars(String fileTitle)
  {
    return convertToEnglishChars(fileTitle).replace("?", "").replace(":", "").replace("*" , "").replace("<" , "").replace(">", "")
                                           .replace("|", "").replace("/", "").replace("\\", "").replace("\"", "").replace("'", "");
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  /**
   * Creates a directory by creating all nonexistent parent directories first.
   * Does not throw an exception if directory already exists.
   */
  public void createDirectories() throws IOException
  {
    Files.createDirectories(toPath());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  /**
   * Creates a new directory. Throws exception if directory already exists.
   */
  public void createDirectory() throws IOException
  {
    Files.createDirectory(toPath());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void deleteDirectory(boolean singleCall) throws IOException
  {
    FilePath filePath = getDirOnly();

    fileManagerDlg.setNeedRefresh();

    if (singleCall && SystemUtils.IS_OS_WINDOWS)
    {
      ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "RD /S /Q \"" + filePath + '"');
      Process proc = pb.redirectErrorStream(true).start();

      try
      {
        proc.waitFor();
      }
      catch (InterruptedException e)
      {
        throw new IOException(e);
      }

      try (InputStream is = proc.getInputStream())
      {
        String errStr = IOUtils.toString(is, StandardCharsets.UTF_8);

        if (errStr.length() > 0)
        {
          if (errStr.toLowerCase().contains("denied") || errStr.toLowerCase().contains("access"))
            errStr = errStr + "\n\nIt may work to restart " + appTitle + " and try again.";

          throw new IOException(errStr);
        }
      }

      return;
    }

    FileUtils.deleteDirectory(filePath.toFile());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void renameDirectory(FilePath destFilePath) throws IOException
  {
    FilePath srcFilePath = getDirOnly();

    fileManagerDlg.setNeedRefresh();

    if (SystemUtils.IS_OS_WINDOWS)
    {
      ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "ren \"" + srcFilePath + "\" \"" + destFilePath.getNameOnly() + '"');
      Process proc = pb.redirectErrorStream(true).start();

      try
      {
        proc.waitFor();
      }
      catch (InterruptedException e)
      {
        throw new IOException(e);
      }

      try (InputStream is = proc.getInputStream())
      {
        String errStr = IOUtils.toString(is, StandardCharsets.UTF_8);

        if (errStr.length() > 0)
        {
          if (errStr.toLowerCase().contains("denied"))
            errStr = errStr + "\n\nIt may work to restart " + appTitle + " and try again.";

          throw new IOException(errStr);
        }
      }

      return;
    }

    FileUtils.moveDirectory(srcFilePath.toFile(), destFilePath.toFile());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void addDirContentsToSet(FilePathSet set) throws HyperDataException
  {
    try
    {
      Files.walkFileTree(getDirOnly().toPath(), new SimpleFileVisitor<>()
      {
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        {
          set.add(new FilePath(file));
          return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFileFailed(Path file, IOException e)
        {
          return FileVisitResult.SKIP_SUBTREE;
        }

        @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        {
          set.add(new FilePath(dir));
          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (IOException e)
    {
      throw new HyperDataException(e);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean canObtainLock() throws IOException
  {
    if (exists() == false) return true;

    if (isDirectory())
    {
      try
      {
        FileUtils.touch(toFile());
      }
      catch (IOException e)
      {
        return false;
      }

      return true;
    }

    try (RandomAccessFile raFile = new RandomAccessFile(toFile(), "rw");
         FileChannel channel = raFile.getChannel(); FileLock lock = channel.tryLock())
    {
      if (lock == null)
        return false;
    }
    catch (FileNotFoundException e)
    {
      return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean anyOpenFilesInDir()
  {
    try
    {
      Files.walkFileTree(getDirOnly().toPath(), new SimpleFileVisitor<>()
      {
        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
        {
          if (new FilePath(file).canObtainLock() == false)
            throw new IOException("Unable to obtain lock for file: " + file.toString());

          return FileVisitResult.CONTINUE;
        }

        @Override public FileVisitResult visitFileFailed(Path file, IOException e)
        {
          return FileVisitResult.SKIP_SUBTREE;
        }

        @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
        {
          FileUtils.touch(dir.toFile());

          return FileVisitResult.CONTINUE;
        }
      });
    }
    catch (IOException e)
    {
      messageDialog(e.getMessage(), mtError);
      return true;
    }

    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean isSubpath(FilePath subFilePath)
  {
    while (equals(subFilePath) == false)
    {
      subFilePath = subFilePath.getParent();
      if (isEmpty(subFilePath)) return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public String toURLString()
  {
    String pathStr;

    try
    {
      pathStr = toURI().toURL().toExternalForm();
    }
    catch (MalformedURLException e)
    {
      return "";
    }

    if (pathStr.startsWith("file:/") && !pathStr.startsWith("file://"))
      pathStr = "file://" + pathStr.substring(5);

    return pathStr;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public boolean dirContainsAnyFiles(boolean checkSubdirs) throws IOException
  {
    if (checkSubdirs == false)
    {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(getDirOnly().toPath(), "**"))
      {
        if (findFirst(stream, entry -> new FilePath(entry).isDirectory()) != null)
          return true;
      }

      return false;
    }

    final MutableBoolean hasFiles = new MutableBoolean(false);

    Files.walkFileTree(getDirOnly().toPath(), new SimpleFileVisitor<>()
    {
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
      {
        if (new FilePath(file).isDirectory() == false)
          hasFiles.setTrue();

        return FileVisitResult.CONTINUE;
      }

      @Override public FileVisitResult visitFileFailed(Path file, IOException e)
      {
        return FileVisitResult.SKIP_SUBTREE;
      }

      @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
      {
        return FileVisitResult.CONTINUE;
      }
    });

    return hasFiles.booleanValue();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
