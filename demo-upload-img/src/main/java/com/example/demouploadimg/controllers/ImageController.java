package com.example.demouploadimg.controllers;

import com.example.demouploadimg.services.FileStorageServiceBase;
import org.apache.tomcat.util.file.ConfigurationSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    FileStorageServiceBase fileStorageServiceBase;
    private static final String UPLOAD_DIR = "./uploads";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestPart("image") MultipartFile image) throws Exception {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("No image file provided");
        }

        String fileName = fileStorageServiceBase.storeFile(image);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
        return new ResponseEntity<>("Upload image success" ,HttpStatus.OK);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Object> getImage(@PathVariable String filename, HttpServletRequest request) throws Exception {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR, filename);
            Resource resource = fileStorageServiceBase.loadFileAsResource(filename);
            // Try to determine file's content type
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/getFile/**")
    public ResponseEntity<Object> getImageDemo(HttpServletRequest request) throws Exception {
        return this.fetchInfo(request);
    }

    private ResponseEntity<Object> fetchInfo(HttpServletRequest request) throws Exception, IOException {
        String relativeUrl = this.extractRelativeUrl(request);
        Resource resource = this.fileStorageServiceBase.loadFileAsResource(relativeUrl);
        HttpHeaders httpHeaders = this.fileStorageServiceBase.loadHttpHeaders(resource);
        return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
    }

    private String extractRelativeUrl(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE); // /files/relativeUrl
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE); // /files/**
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path); // relativeUrl
    }
}
