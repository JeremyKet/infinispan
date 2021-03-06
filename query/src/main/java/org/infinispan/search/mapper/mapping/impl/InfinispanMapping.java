package org.infinispan.search.mapper.mapping.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.util.common.impl.Closer;
import org.infinispan.search.mapper.common.EntityReference;
import org.infinispan.search.mapper.mapping.EntityConverter;
import org.infinispan.search.mapper.mapping.SearchIndexedEntity;
import org.infinispan.search.mapper.mapping.SearchMapping;
import org.infinispan.search.mapper.scope.SearchScope;
import org.infinispan.search.mapper.scope.impl.SearchScopeImpl;
import org.infinispan.search.mapper.session.SearchSession;
import org.infinispan.search.mapper.session.impl.InfinispanSearchSession;
import org.infinispan.search.mapper.session.impl.InfinispanSearchSessionMappingContext;
import org.infinispan.search.mapper.work.SearchIndexer;
import org.infinispan.search.mapper.work.impl.SearchIndexerImpl;

public class InfinispanMapping extends AbstractPojoMappingImplementor<SearchMapping>
      implements SearchMapping, InfinispanSearchSessionMappingContext {

   private final InfinispanTypeContextContainer typeContextContainer;
   private final EntityLoader<EntityReference, ?> entityLoader;
   private final EntityConverter entityConverter;
   private final SearchSession mappingSession;
   private final SearchIndexer searchIndexer;

   private final Map<String, Class<?>> allIndexedTypes;

   private SearchIntegration integration;
   private boolean close = false;

   InfinispanMapping(PojoMappingDelegate mappingDelegate, InfinispanTypeContextContainer typeContextContainer,
                     EntityLoader<EntityReference, ?> entityLoader, EntityConverter entityConverter) {
      super(mappingDelegate);
      this.typeContextContainer = typeContextContainer;
      this.entityLoader = entityLoader;
      this.entityConverter = entityConverter;
      this.mappingSession = new InfinispanSearchSession(this, typeContextContainer);
      this.searchIndexer = new SearchIndexerImpl(mappingSession.createIndexer(), entityConverter, typeContextContainer);
      this.allIndexedTypes = collectAllIndexedTypes();
   }

   @Override
   public void close() {
      try (Closer<RuntimeException> closer = new Closer<>()) {
         closer.push(SearchIntegration::close, integration);
         close = true;
      }
   }

   @Override
   public <E> SearchScope<E> scope(Collection<? extends Class<? extends E>> targetedTypes) {
      return createScope(targetedTypes);
   }

   @Override
   public SearchScope<?> scopeAll() {
      return getSearchScope(typeContextContainer.allTypeIdentifiers());
   }

   @Override
   public SearchScope<?> scopeFromJavaClasses(Collection<Class<?>> javaClasses) {
      if (entityConverter == null || !javaClasses.contains(entityConverter.targetType())) {
         return createScope(javaClasses);
      }

      List<PojoRawTypeIdentifier<?>> targetedTypes = typeContextContainer.allTypeIdentifiers().stream()
            .filter(typeIdentifier -> typeIdentifier.javaClass()
                  .equals(entityConverter.convertedType()) || javaClasses.contains(typeIdentifier.javaClass()))
            .collect(Collectors.toList());

      return getSearchScope(targetedTypes);
   }

   @Override
   public SearchMapping toConcreteType() {
      return this;
   }

   @Override
   public <E> SearchScopeImpl<E> createScope(Collection<? extends Class<? extends E>> classes) {
      List<PojoRawTypeIdentifier<? extends E>> typeIdentifiers = new ArrayList<>(classes.size());
      for (Class<? extends E> clazz : classes) {
         typeIdentifiers.add(PojoRawTypeIdentifier.of(clazz));
      }

      return getSearchScope(typeIdentifiers);
   }

   @Override
   public <E> SearchScopeImpl<E> createScope(Class<E> expectedSuperType, Collection<String> entityNames) {
      List<PojoRawTypeIdentifier<? extends E>> typeIdentifiers = new ArrayList<>(entityNames.size());
      for (String entityName : entityNames) {
         typeIdentifiers.add(PojoRawTypeIdentifier.of(expectedSuperType, entityName));
      }

      return getSearchScope(typeIdentifiers);
   }

   @Override
   public boolean isClose() {
      return close;
   }

   @Override
   public SearchSession getMappingSession() {
      return mappingSession;
   }

   @Override
   public SearchIndexer getSearchIndexer() {
      return searchIndexer;
   }

   @Override
   public SearchIndexedEntity indexedEntity(Class<?> entityType) {
      return typeContextContainer.getIndexedByEntityType(entityType);
   }

   @Override
   public SearchIndexedEntity indexedEntity(String entityName) {
      return typeContextContainer.getIndexedByEntityName(entityName);
   }

   @Override
   public Map<String, Class<?>> allIndexedTypes() {
      return allIndexedTypes;
   }

   @Override
   public boolean isIndexedType(Object value) {
      if (value == null) {
         return false;
      }
      Class<?> c = value.getClass();
      return allIndexedTypes.containsValue(c);
   }

   @Override
   public FailureHandler getFailureHandler() {
      return delegate().failureHandler();
   }

   public void setIntegration(SearchIntegration integration) {
      this.integration = integration;
   }

   private Map<String, Class<?>> collectAllIndexedTypes() {
      Map<String, Class<?>> entities = typeContextContainer.getAllIndexed().stream()
            .collect(Collectors.toMap((a) -> a.javaClass().getName(), SearchIndexedEntity::javaClass));
      if (entityConverter != null) {
         entities.put(entityConverter.targetType().getName(), entityConverter.targetType());
      }
      return Collections.unmodifiableMap(entities);
   }

   private <E> SearchScopeImpl<E> getSearchScope(Collection<PojoRawTypeIdentifier<? extends E>> typeIdentifiers) {
      PojoScopeDelegate<EntityReference, E, PojoRawTypeIdentifier<? extends E>> pojoScopeDelegate =
            delegate().createPojoScope(this, typeIdentifiers,
                  // Store the type identifier as additional metadata
                  typeIdentifier -> typeIdentifier);
      return new SearchScopeImpl(this, pojoScopeDelegate, this.entityLoader);
   }
}
