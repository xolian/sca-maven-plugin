/*
 * Copyright 2016 Micro Focus or one of its affiliates.
 */
package com.fortify.sca.plugins.maven.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {
    public static boolean isFile(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return isFile(file);
    }

    public static boolean isFile(File file) {
        if (file == null) {
            return false;
        } else {
            return file.exists() && file.isFile();
        }
    }

    public static boolean isDirectory(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return isDirectory(file);
    }

    public static boolean isDirectory(File file) {
        if (file == null) {
            return false;
        } else {
            return file.exists() && file.isDirectory();
        }
    }

    public static boolean isAbsolute(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return isAbsolute(file);
    }

    public static boolean isAbsolute(File file) {
        if (file == null) {
            return false;
        } else {
            return file.isAbsolute();
        }
    }

    public static void writeTextFile(File out, String str) throws IOException {
        FileWriter writer = null;
        try {
            File parent = out.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException("Cannot create file: " + out.getAbsolutePath());
            }
            writer = new FileWriter(out);
            writer.write(str);
            writer.flush();
        } finally {
            FileUtil.closeQuietly(writer);
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        int readBytes = 0;
        byte[] buf = new byte[1024];
        while ((readBytes = inputStream.read(buf)) != -1) {
            outputStream.write(buf, 0, readBytes);
        }
    }

    public static boolean matches(File file, File pattern) {
        String filePath = replaceSeparatorToSlash(file.getAbsolutePath());
        String filePattern = replaceSeparatorToSlash(pattern.getAbsolutePath());
        filePattern = StringUtil.createRegexFromFilePattern(filePattern);

        Pattern p = Pattern.compile(filePattern);
        Matcher m = p.matcher(filePath);

        return m.matches();
    }

    public static boolean delete(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return delete(file);
    }

    public static boolean delete(File file) {
        if (file == null) {
            return false;
        } else {
            return file.delete();
        }
    }

    public static boolean containsFile(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return containsFile(file);
    }

    public static boolean containsFile(File file) {
        if (FileUtil.isFile(file)) {
            return true;
        } else if (FileUtil.isDirectory(file)) {
            List<String> filePaths = getAllFilePathsInDirectory(file);
            return filePaths.size() > 0;
        } else {
            return false;
        }
    }

    public static boolean containsClass(String filePath) {
        if (StringUtil.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return containsClass(file);
    }

    public static boolean containsClass(File file) {
        if (FileUtil.isFile(file) && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
            return true;
        } else if (FileUtil.isDirectory(file)) {
            for (String filePath : getAllFilePathsInDirectory(file)) {
                if (filePath.endsWith(".class")) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static List<String> getAllFilePathsInDirectory(String dirPath) {
        if (StringUtil.isEmpty(dirPath)) {
            return new ArrayList<String>();
        }
        File dir = new File(dirPath);
        return getAllFilePathsInDirectory(dir);
    }

    public static List<String> getAllFilePathsInDirectory(File dir) {
        List<String> fileList = new ArrayList<String>();

        if (FileUtil.isDirectory(dir)) {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                }
                if (file.isDirectory()) {
                    List<String> filePaths = getAllFilePathsInDirectory(file);
                    fileList.addAll(filePaths);
                }
            }
        }
        return fileList;
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    public static String normalizeFilePath(String filePath) {
        if (!StringUtil.isEmpty(filePath)) {
            filePath = replaceSeparatorToSlash(filePath);
            filePath = filePath.replace("//", "/"); // just in case
            filePath = StringUtil.encloseQuotes(filePath);
        }
        return filePath;
    }

    public static String replaceSeparatorToSlash(String filePath) {
        if (!StringUtil.isEmpty(filePath)) {
            filePath = filePath.replace("\\", "/");
        }
        return filePath;
    }
}
