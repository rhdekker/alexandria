package nl.knaw.huygens.alexandria.config;

/*
 * #%L
 * alexandria-service
 * =======
 * Copyright (C) 2015 Huygens ING (KNAW)
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

import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.endpoint.annotation.AnnotationEntityBuilder;
import nl.knaw.huygens.alexandria.endpoint.resource.ResourceEntityBuilder;
import nl.knaw.huygens.alexandria.service.AlexandriaService;
import nl.knaw.huygens.alexandria.service.TinkerPopService;
import nl.knaw.huygens.alexandria.service.TitanService;

public class AlexandriaServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    // guice binds here
    Log.trace("configureServlets(): setting up Guice bindings");
    bindServiceTo(TitanService.class);
    // bindServiceTo(Neo4JService.class);
    bind(AnnotationEntityBuilder.class).in(Scopes.SINGLETON);
    bind(ResourceEntityBuilder.class).in(Scopes.SINGLETON);
    super.configureServlets();
  }

  private void bindServiceTo(Class<? extends TinkerPopService> tinkerpopServiceClass) {
    bind(AlexandriaService.class).to(tinkerpopServiceClass);
    bind(TinkerPopService.class).to(tinkerpopServiceClass);
  }
}
