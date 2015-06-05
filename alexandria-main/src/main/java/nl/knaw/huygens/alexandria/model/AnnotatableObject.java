package nl.knaw.huygens.alexandria.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class AnnotatableObject implements Accountable {

  private AlexandriaState state = AlexandriaState.Default;
  private final Set<AlexandriaAnnotation> annotations = new HashSet<>();

  public Set<AlexandriaAnnotation> getAnnotations() {
    return Collections.unmodifiableSet(annotations);
  }

  public void addAnnotation(AlexandriaAnnotation annotation) {
    annotations.add(annotation);
  }

  public AlexandriaState getState() {
    return state;
  }

  public void setState(AlexandriaState state) {
    this.state = state;
  }

}