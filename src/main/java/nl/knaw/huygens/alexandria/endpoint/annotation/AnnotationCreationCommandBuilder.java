package nl.knaw.huygens.alexandria.endpoint.annotation;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.exception.BadRequestException;
import nl.knaw.huygens.alexandria.service.AnnotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationCreationCommandBuilder {
  public static final String MISSING_TYPE_MESSAGE = "Annotation MUST have a type";
  public static final String NO_SUCH_ANNOTATION_FORMAT = "Supposedly existing annotation [%s] not found";
  public static final String MISSING_ANNOTATION_BODY_MESSAGE = "Missing or empty annotation request body";

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationCreationCommandBuilder.class);

  public static AnnotationCreationCommandBuilder servedBy(AnnotationService service) {
    return new AnnotationCreationCommandBuilder(service);
  }

  private final AnnotationService service;

  protected AnnotationCreationCommandBuilder(AnnotationService service) {
    this.service = requireNonNull(service, "AnnotationService MUST not be null");
  }

  public AnnotationCreationCommand build(AnnotationPrototype prototype) {
    Optional.ofNullable(prototype).orElseThrow(missingBodyException());

    validateId(prototype);
    validateType(prototype);
    validateValue(prototype);
    validateCreatedOn(prototype);
    validateAnnotations(prototype);

    LOG.trace("Done validating");

    return new AnnotationCreationCommand(prototype);
  }

  protected void validateId(AnnotationPrototype prototype) {
    LOG.trace("Validating id");
    return; // missing or empty Id means client does not override server generated value.
  }

  protected void validateType(AnnotationPrototype prototype) {
    LOG.trace("Validating type");
    prototype.getType().filter(s -> !s.isEmpty()).orElseThrow(missingTypeException());
  }

  protected void validateValue(AnnotationPrototype prototype) {
    LOG.trace("Validating value");
    return; // missing or empty value is ok (means annotation is a Tag)
  }

  protected void validateCreatedOn(AnnotationPrototype prototype) {
    LOG.trace("Validating createdOn");
    return; // missing or empty createdOn means client does not override server set value.
  }

  protected void validateAnnotations(AnnotationPrototype prototype) {
    LOG.trace("Validating annotations");
    prototype.getAnnotations().ifPresent(annotationParams -> stream(annotationParams) //
        .map(UUIDParam::getValue).forEach(this::validateAnnotationId));
  }

  protected <T> Stream<T> stream(Collection<T> c) {
    return c.parallelStream(); // override in case you prefer stream() over parallelStream()
  }

  protected void validateAnnotationId(UUID uuid) {
    LOG.trace("Validating annotation: [{}]", uuid);
    Optional.ofNullable(service.readAnnotation(uuid)).orElseThrow(noSuchAnnotationException(uuid));
  }

  protected Supplier<BadRequestException> missingBodyException() {
    return () -> badRequestException(MISSING_ANNOTATION_BODY_MESSAGE);
  }

  protected Supplier<BadRequestException> missingTypeException() {
    return () -> badRequestException(MISSING_TYPE_MESSAGE);
  }

  protected Supplier<BadRequestException> noSuchAnnotationException(UUID uuid) {
    return () -> badRequestException(String.format(NO_SUCH_ANNOTATION_FORMAT, uuid.toString()));
  }

  protected BadRequestException badRequestException(String message) {
    LOG.trace(message);
    return new BadRequestException(message);
  }

}