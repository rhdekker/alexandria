package nl.knaw.huygens.alexandria.service;

import java.util.UUID;

import nl.knaw.huygens.alexandria.exception.IllegalResourceException;
import nl.knaw.huygens.alexandria.exception.NotFoundException;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;

public interface ResourceService {
  AlexandriaResource createResource(UUID uuid) throws IllegalResourceException;

  AlexandriaResource readResource(UUID uuid) throws NotFoundException;

  AlexandriaResource updateResource(AlexandriaResource resource) throws IllegalResourceException;

  void deleteResource(UUID uuid) throws NotFoundException;
}