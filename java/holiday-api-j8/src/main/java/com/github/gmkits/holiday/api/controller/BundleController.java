package com.github.gmkits.holiday.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
public class BundleController {

    @GetMapping(value = "/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getManifest() throws IOException {
        Resource resource = new ClassPathResource("manifest.json");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStream in = resource.getInputStream();
        try {
            String content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } finally {
            in.close();
        }
    }

    @GetMapping("/bundle/{region}/{year}")
    public ResponseEntity<byte[]> getBundle(
            @PathVariable String region,
            @PathVariable int year) throws IOException {
        String path = "bundles/" + region + "/" + year + ".hday";
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStream in = resource.getInputStream();
        try {
            byte[] bytes = StreamUtils.copyToByteArray(in);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + year + ".hday\"");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } finally {
            in.close();
        }
    }
}
