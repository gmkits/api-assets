package com.github.gmkits.apiassets.calendar.service.controller;

import com.github.gmkits.apiassets.calendar.service.ApiException;
import com.github.gmkits.apiassets.calendar.service.ValidatedAssetStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/** 下载与查询引擎完全相同的已验证离线资产。 */
@RestController
@RequestMapping("/v1/calendar/assets")
public final class AssetController {
    private static final Pattern REGION = Pattern.compile("[A-Z]{2}(?:-[A-Z0-9]{1,8})*");
    private final ValidatedAssetStore assets;

    public AssetController(ValidatedAssetStore assets) {
        this.assets = assets;
    }

    @GetMapping(value = "/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> manifest(HttpServletRequest request) {
        return response("manifest.json", null, MediaType.APPLICATION_JSON, request);
    }

    @GetMapping(value = "/calendar.cdat", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> calendar(HttpServletRequest request) {
        return response("calendar/calendar.cdat", "calendar.cdat",
                MediaType.APPLICATION_OCTET_STREAM, request);
    }

    @GetMapping(value = "/holidays/{region}/{year}.hday",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> holiday(@PathVariable String region, @PathVariable int year,
                                          HttpServletRequest request) {
        if (!REGION.matcher(region).matches() || year < 1 || year > 9999) {
            throw ApiException.badRequest("不支持的地区或年份: " + region + "/" + year);
        }
        return response("holidays/bundles/" + region + "/" + year + ".hday",
                region + "-" + year + ".hday", MediaType.APPLICATION_OCTET_STREAM, request);
    }

    private ResponseEntity<byte[]> response(String path, String filename, MediaType type,
                                             HttpServletRequest request) {
        ValidatedAssetStore.Asset asset = assets.requireAsset(path);
        String etag = '"' + asset.sha256() + '"';
        if (etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header("X-Checksum-SHA256", asset.sha256())
                    .build();
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(type)
                .contentLength(asset.size())
                .eTag(etag)
                .header("X-Checksum-SHA256", asset.sha256());
        if (filename != null) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"");
        }
        return builder.body(asset.bytes());
    }
}
