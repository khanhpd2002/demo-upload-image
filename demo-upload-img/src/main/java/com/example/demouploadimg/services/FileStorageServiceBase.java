package com.example.demouploadimg.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.activation.MimetypesFileTypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceBase {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceBase() throws Exception {
        this.fileStorageLocation = Paths.get("./uploads");
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new Exception(
                    "Could not create the directory where the uploaded files will be stored.\n" + ex.getMessage());
        }
    }

    public HttpHeaders loadHttpHeaders(Resource resource) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, new MimetypesFileTypeMap().getContentType(resource.getFile()));
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
        return headers;
    }

    public String storeFile(MultipartFile file) throws Exception {
        // Normalize file name
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new Exception("File name invalid");
            }

            Path targetLocation = this.fileStorageLocation.resolve(originalFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return originalFileName;
        } catch (IOException ex) {
            throw new Exception("IOException");
        }
    }

    public Resource loadFileAsResource(String fileName) throws Exception {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new Exception("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new Exception("File not found " + fileName + "\n" + ex.getMessage());
        }
    }
    public String getAbsolutePath(String relativePath) {
        return Paths.get(this.fileStorageLocation.normalize().toString(), relativePath).normalize().toFile()
                .getAbsolutePath();
    }
}
