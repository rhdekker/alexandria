package nl.knaw.huygens.alexandria.endpoint.resource;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import nl.knaw.huygens.alexandria.endpoint.resource.TextImportTask.Status;

@Singleton
public class TaskStatusMap {
  private static Map<UUID, TextImportTask.Status> map = Maps.newHashMap();

  public void put(UUID id, Status status) {
    removeExpiredTasks();
    map.put(id, status);
  }

  public Optional<TextImportTask.Status> get(UUID id) {
    removeExpiredTasks();
    return Optional.ofNullable(map.get(id));
  }

  public void removeExpiredTasks() {
    List<UUID> expiredEntries = map.keySet().stream()//
        .filter(uuid -> map.get(uuid).isExpired())//
        .collect(toList());
    expiredEntries.forEach(key -> map.remove(key));
  }

}
