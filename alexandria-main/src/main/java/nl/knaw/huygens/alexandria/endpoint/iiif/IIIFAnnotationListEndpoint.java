package nl.knaw.huygens.alexandria.endpoint.iiif;

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

import static nl.knaw.huygens.alexandria.api.w3c.WebAnnotationConstants.JSONLD_MEDIATYPE;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import com.github.jsonldjava.utils.JsonUtils;
import com.google.common.collect.Maps;

import nl.knaw.huygens.alexandria.api.model.iiif.IIIFAnnotationList;
import nl.knaw.huygens.alexandria.config.AlexandriaConfiguration;
import nl.knaw.huygens.alexandria.endpoint.webannotation.WebAnnotation;
import nl.knaw.huygens.alexandria.endpoint.webannotation.WebAnnotationService;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class IIIFAnnotationListEndpoint extends AbstractIIIFEndpoint {

  private String name;
  private String identifier;
  private WebAnnotationService webAnnotationService;

  public IIIFAnnotationListEndpoint(String identifier, String name, AlexandriaService service, AlexandriaConfiguration config, URI id) {
    super(id);
    this.identifier = identifier;
    this.name = name;
    this.webAnnotationService = new WebAnnotationService(service, config);
  }

  @GET
  public Response get() {
    return notImplemented(dummySequence());
  }

  @POST
  @Consumes(JSONLD_MEDIATYPE)
  // Schaalbaarheid (memory): de hele lijst van annotaties wordt in het geheugen geladen voordat er aan
  // verwerking begonnen wordt. 1000 annotaties = 1000 objecten, 10.000 annotaties = 10.000 objecten in geheugen etc.
  // Jax-rs en JSON hebben ook streaming mogelijkheden. Dat leidt wel tot iets complexere code.
  public Response postAnnotationList(IIIFAnnotationList annotationList) {
    Map<String, Object> otherProperties = annotationList.getOtherProperties();
    String context = (String) otherProperties.get("@context");
    Map<String, Object> processedList = Maps.newHashMap(otherProperties);
    // Schaalbaarheid (memory): de hele lijst van resultaten wordt in het geheugen gehouden.
    // Jax-RS heeft ook streaming API's.
    List<Map<String, Object>> resources = new ArrayList<>(annotationList.getResources().size());
    annotationList.getResources().forEach(prototype -> {
      prototype.setCreated(Instant.now().toString());
      prototype.getVariablePart().put("@context", context);
      // performance (IO):
      // De validateAndStore method doet altijd eerst een query naar annotaties en dan een read
      // van een resource. Soms ook een write. Zie voor details de comments in de bijbehorende class.
      WebAnnotation webAnnotation = webAnnotationService.validateAndStore(prototype);
      try {
        Map<String, Object> map = (Map<String, Object>) JsonUtils.fromString(webAnnotation.json());
        resources.add(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
    processedList.put("resources", resources);
    return ok(processedList);
  }

  private Map<String, Object> dummySequence() {
    Map<String, Object> dummy = baseMap();
    return dummy;
  }

  @Override
  String getType() {
    return "sc:AnnotationList";
  }

}
