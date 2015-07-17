package com.simapi.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileContentReader {
    
    private static Log LOGGER = LogFactory.getLog(FileContentReader.class);
    
    private static final String[] SUPPORTED_FILE_EXTENSIONS = new String[] {
        ".json", ".xml"
    };
    
    private Map<String, String> fileContentMap = new ConcurrentHashMap<>();
    
    public void init(List<String> classpathFileNames) {
        if (null == classpathFileNames) {
            throw new IllegalArgumentException("classpath file names can't be null");
        }
        for (String fileName : classpathFileNames) {
            if (StringUtils.isBlank(fileName)) {
                throw new IllegalArgumentException("filename can't be blank");
            }

            int numberOfFilesFound = 0;
            for (String fullName : getFileNameWithExtension(fileName)) {
                try (InputStream in = new ClassPathResource(fullName).getInputStream()) {
                    String content = IOUtils.toString(in, "UTF-8");
                    
                    fileContentMap.put(fullName, content);
                    numberOfFilesFound++;
                }
                catch (IOException e) {}
            }
            if (numberOfFilesFound < 1) {
                LOGGER.error("no file loaded from classpath for prefix: " + fileName);
            }
        }
    }
    
    private List<String> getFileNameWithExtension(String fileName) {
        List<String> fileNamesWithExtension = new ArrayList<>();

        for (String extension : SUPPORTED_FILE_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                fileNamesWithExtension.add(fileName);
                break;
            }
        }
        for (String extension : SUPPORTED_FILE_EXTENSIONS) {
            fileNamesWithExtension.add(fileName + extension);
        }
        
        return fileNamesWithExtension;
    }
    
    public String getFileContent(String fileName) throws FileNotFoundException {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("filename can't be blank");
        }
        String content = fileContentMap.get(fileName);
        if (null == content) {
            throw new FileNotFoundException("no such file in memory: " + fileName);
        }
        
        return content;
    }
}
