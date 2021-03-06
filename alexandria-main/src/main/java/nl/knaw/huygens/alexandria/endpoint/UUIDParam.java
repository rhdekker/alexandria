package nl.knaw.huygens.alexandria.endpoint;

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

import java.util.UUID;

public class UUIDParam extends AbstractParam<UUID> {
  public UUIDParam(String param) {
    super(param);
  }

  @Override
  protected UUID parse(String param) {
    return UUID.fromString(param);
  }

  @Override
  protected String getErrorMessage(String param, Throwable e) {
    return String.format("Malformed UUID: %s (%s)", param, e.getMessage());
  }
}
