package nl.knaw.huygens.alexandria.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotation;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotationBody;
import nl.knaw.huygens.alexandria.model.AlexandriaProvenance;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.model.TentativeAlexandriaProvenance;

public interface AlexandriaService {
  /**
   *
   * @param uuid
   * @param ref
   * @param provenance
   * @return true if the resource was created, false if it was updated
   */
  boolean createOrUpdateResource(UUID uuid, String ref, TentativeAlexandriaProvenance provenance);

  AlexandriaResource readResource(UUID uuid);

  boolean createAnnotationBody(UUID uuid, Optional<String> type, String value, TentativeAlexandriaProvenance provenance);

  AlexandriaAnnotationBody findAnnotationBodyWithTypeAndValue(Optional<String> type, String value);

  AlexandriaAnnotationBody readAnnotationBody(UUID uuid);

  AlexandriaAnnotation annotate(AlexandriaResource resource, AlexandriaAnnotationBody annotationbody, TentativeAlexandriaProvenance provenance);

  AlexandriaAnnotation annotate(AlexandriaAnnotation annotation, AlexandriaAnnotationBody annotationbody, TentativeAlexandriaProvenance provenance);

  AlexandriaAnnotation readAnnotation(UUID uuid);

  Set<AlexandriaResource> readSubResources(UUID uuid);

  AlexandriaProvenance readProvenance(UUID value);

}
