// src/main/java/com/example/Main.java
package com.example;

import org.apache.commons.cli.*;
import org.apache.tika.Tika;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {

        // Disable Commons Logging
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        // Disable Log4j
        System.setProperty("org.apache.log4j.Logger",
                "org.apache.log4j.spi.NullLogger");

        // Disable Java Util Logging
        System.setProperty("java.util.logging.config.file", "/dev/null");

        // For SLF4J (if used)
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "OFF");
        Options options = new Options();

        // Add refresh argument
        Option refreshOption = Option.builder("r")
                .longOpt("refresh")
                .desc("Refresh option")
                .build();

        // Add search argument
        Option searchOption = Option.builder("s")
                .longOpt("search")
                .desc("Search option")
                .hasArg()
                .argName("query")
                .build();

        // Add folder path argument
        Option folderOption = Option.builder("f")
                .longOpt("folder")
                .desc("Folder path")
                .hasArg()
                .argName("path")
                .build();

        options.addOption(refreshOption);
        options.addOption(searchOption);
        options.addOption(folderOption);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String folderPath = null;
            if (cmd.hasOption("folder") || cmd.hasOption("f")) {
                folderPath = cmd.getOptionValue("folder");
                System.out.println("Folder path: " + folderPath);
            }

            if (cmd.hasOption("refresh") || cmd.hasOption("r")) {
                System.out.println("Refresh option enabled");
                // Add your refresh logic here
                performRefresh(folderPath);
            }

            if (cmd.hasOption("search") || cmd.hasOption("s")) {
                String searchQuery = cmd.getOptionValue("search");
                System.out.println("Search option enabled with query: " + searchQuery);
                // Add your search logic here
                performSearch(searchQuery, folderPath);
            }

        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar your-app.jar", options);
        }
    }

    private static void performRefresh(String folderPath) {
        // Add your refresh logic here
        System.out.println("Performing refresh..." +
                (folderPath != null ? " in folder: " + folderPath : ""));

        if (folderPath != null) {
            List<File> docxAndPdfFiles = getRecursiveDocxAndPdfFiles(folderPath);
            System.out.println("Found " + docxAndPdfFiles.size() + " DOCX/PDF files:");

            // Process each file to extract content and create JSON
            for (File file : docxAndPdfFiles) {
                try {
                    String content = extractFileContent(file);
                    // System.out.println(content);
                    createJsonFile(file, content);
                    System.out.println("Processed: " + file.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("Error processing file " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void performSearch(String query, String folderPath) {
        // Add your search logic here
        System.out.println("Searching for: " + query +
                (folderPath != null ? " in folder: " + folderPath : ""));

        try {
            if (folderPath != null) {
                // Get all JSON files recursively
                List<File> jsonFiles = getRecursiveJsonFiles(folderPath);
                System.out.println("Found " + jsonFiles.size() + " JSON files:");

                int matches = 0;
                for (File file : jsonFiles) {
                    // Read JSON file content
                    String content = readJsonFileContent(file);
                    if (content != null && content.toLowerCase().contains(query.toLowerCase())) {
                        System.out.println(" " + file.getAbsolutePath());
                        matches++;
                    }
                }
                System.out.println("Found " + matches + " matching files.");
            } else {
                // Search in current directory
                Path currentDir = Paths.get("").toAbsolutePath();
                System.out.println("Searching in current directory: " + currentDir);

                List<File> jsonFiles = getRecursiveJsonFiles(currentDir.toString());
                int matches = 0;

                for (File file : jsonFiles) {
                    String content = readJsonFileContent(file);
                    if (content != null && content.toLowerCase().contains(query.toLowerCase())) {
                        System.out.println(" " + file.getAbsolutePath());
                        matches++;
                    }
                }
                System.out.println("Found " + matches + " matching files.");
            }
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
        }
    }

    /**
     * Get all JSON files recursively from a folder
     */
    private static List<File> getRecursiveJsonFiles(String folderPath) {
        List<File> jsonFiles = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return jsonFiles;
        }

        // Use FileVisitor to traverse directory tree
        try {
            Files.walkFileTree(Paths.get(folderPath), new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file,
                        java.nio.file.attribute.BasicFileAttributes attrs) {
                    if (file.toString().toLowerCase().endsWith(".json")) {
                        jsonFiles.add(file.toFile());
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error traversing directory: " + e.getMessage());
        }

        return jsonFiles;
    }

    /**
     * Read content from a JSON file
     */
    private static String readJsonFileContent(File jsonFile) {
        try {
            // Read entire file as string
            String content = Files.readString(jsonFile.toPath());

            // Extract content field using regex (simple approach)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"content\":\\s*\"([^\"]*)\"");
            java.util.regex.Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // If no content field found, return entire content
            return content;
        } catch (IOException e) {
            System.err.println("Error reading file " + jsonFile.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Recursively gets all DOCX and PDF files from the specified folder path
     * 
     * @param folderPath The root folder path to search in
     * @return List of File objects for DOCX and PDF files
     */
    public static List<File> getRecursiveDocxAndPdfFiles(String folderPath) {
        List<File> result = new ArrayList<>();
        File rootFolder = new File(folderPath);

        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            System.err.println("Invalid folder path: " + folderPath);
            return result;
        }

        File[] files = rootFolder.listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively search subdirectories
                result.addAll(getRecursiveDocxAndPdfFiles(file.getAbsolutePath()));
            } else if (file.isFile()) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".docx") || fileName.endsWith(".pdf")) {
                    result.add(file);
                }
            }
        }

        return result;
    }

    /**
     * Extracts text content from a DOCX or PDF file using Apache Tika
     * 
     * @param file The file to extract content from
     * @return Extracted text content as string
     * @throws Exception if extraction fails
     */
    private static String extractFileContent(File file) throws Exception {
        Tika tika = new Tika();
        return tika.parseToString(file);
    }

    /**
     * Creates a JSON file with the same name as the original file but with .json
     * extension
     * Only updates if the original file was modified more recently than the JSON
     * file
     * 
     * @param originalFile The original DOCX/PDF file
     * @param content      The extracted content to write to JSON file
     * @throws IOException if file creation fails
     */
    private static void createJsonFile(File originalFile, String content) throws IOException {
        // Create path for JSON file (same location with .json extension)
        String originalPath = originalFile.getAbsolutePath();
        String jsonPath = originalPath.substring(0, originalPath.lastIndexOf('.')) + ".json";

        File jsonFile = new File(jsonPath);

        // Check if JSON file already exists
        if (jsonFile.exists()) {
            // Compare modification times
            long originalModified = originalFile.lastModified();
            long jsonModified = jsonFile.lastModified();

            // Only update if original file is more recent
            if (originalModified <= jsonModified) {
                System.out.println("JSON file is up to date. Skipping update for: " + originalFile.getName());
                return;
            }
        }

        // Create JSON content structure
        String jsonContent = "{\n" +
                "  \"originalFileName\": \"" + originalFile.getName() + "\",\n" +
                "  \"originalFilePath\": \"" + originalFile.getAbsolutePath() + "\",\n" +
                "  \"content\": \"" + escapeJsonString(content) + "\"\n" +
                "}";

        // Write to JSON file
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(jsonPath))) {
            writer.write(jsonContent);
        }

        System.out.println("Created/updated JSON file: " + jsonFile.getName());
    }

    /**
     * Escapes special characters in JSON string values
     * 
     * @param input The string to escape
     * @return Escaped string safe for JSON
     */
    private static String escapeJsonString(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
