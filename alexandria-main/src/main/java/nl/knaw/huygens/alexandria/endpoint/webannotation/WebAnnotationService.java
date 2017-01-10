package nl.knaw.huygens.alexandria.endpoint.webannotation;

/*
 * #%L
 * alexandria-main
 * =======
 * Copyright (C) 2015 - 2016 Huygens ING (KNAW)
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static nl.knaw.huygens.alexandria.api.w3c.WebAnnotationConstants.OA_HAS_SOURCE_IRI;
import static nl.knaw.huygens.alexandria.api.w3c.WebAnnotationConstants.OA_HAS_TARGET_IRI;
import static nl.knaw.huygens.alexandria.api.w3c.WebAnnotationConstants.WEBANNOTATION_TYPE;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import nl.knaw.huygens.alexandria.api.EndpointPaths;
import nl.knaw.huygens.alexandria.api.model.AlexandriaState;
import nl.knaw.huygens.alexandria.api.model.search.AlexandriaQuery;
import nl.knaw.huygens.alexandria.api.model.search.QueryField;
import nl.knaw.huygens.alexandria.api.model.w3c.WebAnnotationPrototype;
import nl.knaw.huygens.alexandria.config.AlexandriaConfiguration;
import nl.knaw.huygens.alexandria.exception.BadRequestException;
import nl.knaw.huygens.alexandria.jaxrs.ThreadContext;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotation;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotationBody;
import nl.knaw.huygens.alexandria.model.AlexandriaProvenance;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class WebAnnotationService {
  private static final Logger LOG = LoggerFactory.getLogger(WebAnnotationService.class);
  private final AlexandriaService service;
  private final AlexandriaConfiguration config;

  public WebAnnotationService(AlexandriaService service, AlexandriaConfiguration config) {
    this.service = service;
    this.config = config;
  }

  public WebAnnotation validateAndStore(WebAnnotationPrototype prototype) {
    try {
      //NOTE: Er wordt hier aan serialisatie en deserialisatie gedaan.
      //Schaalbaarheid (CPU): Mogelijk performance probleem.
      String json = new ObjectMapper().writeValueAsString(prototype);
      Map<String, Object> jsonObject = (Map<String, Object>) JsonUtils.fromString(json);

      String resourceRef = extractResourceRef(jsonObject);
      AlexandriaResource alexandriaResource = extractAlexandriaResource(resourceRef);
      AlexandriaAnnotation annotation = createWebAnnotation(json, alexandriaResource);

      jsonObject.put("@id", webAnnotationURI(annotation.getId()));
      String json2 = new ObjectMapper().writeValueAsString(jsonObject);
      return new WebAnnotation(annotation.getId())//
        .setJson(json2)//
        .setETag(String.valueOf(prototype.getModified().hashCode()));

    } catch (IOException | JsonLdError e) {
      throw new BadRequestException(e.getMessage());
    }

  }

  URI webAnnotationURI(UUID annotationUUID) {
    return UriBuilder.fromUri(config.getBaseURI())//
      .path(EndpointPaths.WEB_ANNOTATIONS)//
      .path(annotationUUID.toString())//
      .build();
  }

  private String extractResourceRef(Map<String, Object> jsonObject) throws JsonLdError {
    // Log.info("jsonObject={}", jsonObject);
    Map<Object, Object> context = new HashMap<>();
    JsonLdOptions options = new JsonLdOptions();
    Map<String, Object> compacted = JsonLdProcessor.compact(jsonObject, context, options);
    // Log.info("compacted={}", jsonObject);
    Map<String, Object> target = (Map<String, Object>) compacted.get(OA_HAS_TARGET_IRI);
    String resourceRef = "";
    if (target == null) {
      LOG.error("target==null!, compacted={}", compacted); // TODO!
    } else {
      if (target.containsKey("@id")) {
        resourceRef = (String) target.get("@id");
      } else if (target.containsKey(OA_HAS_SOURCE_IRI)) {
        Map<String, Object> source = (Map<String, Object>) target.get(OA_HAS_SOURCE_IRI);
        resourceRef = (String) source.get("@id");
      }
    }
    return resourceRef;
  }

  private AlexandriaResource extractAlexandriaResource(String resourceRef) {
    TentativeAlexandriaProvenance provenance = new TentativeAlexandriaProvenance(ThreadContext.getUserName(), Instant.now(), AlexandriaProvenance.DEFAULT_WHY);
    // first see if there's already a resource with this ref, if so, use this.
    // Performance (IO): er wordt altijd een query gedaan naar annotaties met een bepaalde resource ref.
    // Annotaties worden opgehaald etc. Terwijl de code alleen maar wil weten of er een
    // resource bestaat met een bepaalde resourceRef. Dat moet simpeler kunnen.
    // Performance (CPU): Bovendien zijn bepaalde delen van de AlexandriaQuery opgebouwd uit Strings die dan weer geparsed en verwerkt
    // moeten worden.
    // Performance (Caching) Een andere vraag is of niet heel veel annotaties in dezelfde batch
    // betrekking hebben opdezelfde resource of een beperkt aantal resources.
    // Een cache van bijvoorbeeld de laatste tien resources in een ThreadLocal zou hier al heel veel kunnen
    // helpen.
    UUID resourceUUID;
    AlexandriaQuery query = new AlexandriaQuery()//
      .setPageSize(2)//
      .setFind("annotation")//
      .setWhere("resource.ref:eq(\"" + resourceRef + "\")")//
      .setReturns(QueryField.resource_id.externalName());

    List<Map<String, Object>> results = service.execute(query).getResults();
    if (results.isEmpty()) {
      resourceUUID = UUID.randomUUID();
      // Performance (IO): service.createOrUpdate doet altijd een read voor een write.
      // Is het een idee om een service.createResource te maken waarin ook de randomUUID wordt aangemaakt?
      service.createOrUpdateResource(resourceUUID, resourceRef, provenance, AlexandriaState.CONFIRMED);
      // Performance (IO): hier staat geen return, wat betekent dat na een create alsnog altijd de read
      // onderaan de functie wordt uitgevoerd. Dat lijkt mij overbodig.
    } else {
      String resourceId = (String) results.get(0).get(QueryField.resource_id.externalName());
      resourceUUID = UUID.fromString(resourceId);
    }
    // Performance (IO): zie een paar regels naar boven: er wordt altijd een read uitgevoerd, ook na een create
    return service.readResource(resourceUUID).get();
  }

  private AlexandriaAnnotation createWebAnnotation(String json, AlexandriaResource alexandriaResource) {
    UUID annotationBodyUUID = UUID.randomUUID();
    // TODO: use information from prototype to create provenance
    TentativeAlexandriaProvenance annotationProvenance = new TentativeAlexandriaProvenance(ThreadContext.getUserName(), Instant.now(), AlexandriaProvenance.DEFAULT_WHY);
    AlexandriaAnnotationBody annotationBody = service.createAnnotationBody(annotationBodyUUID, WEBANNOTATION_TYPE, json, annotationProvenance);
    AlexandriaAnnotation annotation = service.annotate(alexandriaResource, annotationBody, annotationProvenance);
    service.confirmAnnotation(annotation.getId());
    return annotation;
  }

}
