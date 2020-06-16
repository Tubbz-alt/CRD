package org.hl7.davinci.endpoint.controllers;

import org.hl7.davinci.endpoint.Application;
import org.hl7.davinci.endpoint.config.YamlConfig;
import org.hl7.davinci.endpoint.database.*;
import org.hl7.davinci.endpoint.files.FileResource;
import org.hl7.davinci.endpoint.files.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Arrays;

import java.io.IOException;
import java.util.logging.Logger;



/**
 * Provides the REST interface that can be interacted with at [base]/api/data.
 */
@RestController
public class DataController {
  private static Logger logger = Logger.getLogger(Application.class.getName());


  @Autowired
  private RequestRepository requestRepository;

  @Autowired
  private YamlConfig myConfig;

  @Autowired
  private FileStore fileStore;


  /**
   * Basic constructor to initialize both data repositories.
   * @param requestRepository the database for request logging
   */
  @Autowired
  public DataController(RequestRepository requestRepository) {
    this.requestRepository = requestRepository;

  }

  @GetMapping(value = "/api/requests")
  @CrossOrigin
  public Iterable<RequestLog> showAllLogs() {
    // logger.info("showAllLogs: GET /api/requests");

    boolean[] timelineTrue = new boolean[5];
    Arrays.fill(timelineTrue, Boolean.TRUE);
    boolean[] timelineFalse = new boolean[5];
    Arrays.fill(timelineFalse, Boolean.FALSE);

    Iterable<RequestLog> list = requestRepository.findAll();
    return list;
  }

  @GetMapping(value = "/api/data")
  @CrossOrigin
  public Iterable<RuleMapping> showAll() {
    logger.info("showAll: GET /api/data");
    return fileStore.findAllRules();
  }

  private ResponseEntity<Resource> processFileResource(FileResource fileResource) {
    if (fileResource == null) {
      logger.warning("file / fhir resource not found, return error (404)");
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
        .contentType(MediaType.parseMediaType("application/octet-stream"))
        .body(fileResource.getResource());
  }

  /**
   * Retrieve a file from the File Store.
   * @param topic (case sensitive)
   * @param fhirVersion (converted to uppercase)
   * @param fileName (case sensitive)
   * @param noconvert
   * @return
   * @throws IOException
   */
  @GetMapping(path = "/files/{topic}/{fhirVersion}/{fileName}")
  public ResponseEntity<Resource> getFile(@PathVariable String topic, @PathVariable String fhirVersion, @PathVariable String fileName, @RequestParam(required = false) boolean noconvert) throws IOException {
    fhirVersion = fhirVersion.toUpperCase();
    logger.info("GET /files/" + topic + "/" + fhirVersion + "/" + fileName);

    FileResource fileResource = fileStore.getFile(topic, fileName, fhirVersion, !noconvert);
    return processFileResource(fileResource);
  }

  /**
   * Reload the entire File Store.
   * @return
   */
  @PostMapping(path = "/reload")
  public RedirectView reload(@RequestParam String vsac_username, @RequestParam String vsac_password) {
    logger.info("reload rule file index");

    if (vsac_username != null && vsac_password != null) {
      fileStore.reinitializeVSACLoader(vsac_username, vsac_password);
    } else {
      fileStore.reinitializeVSACLoader();
    }

    fileStore.reload();
    String newUrl = "/data";

    return new RedirectView(newUrl);
  }

}
