/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.guice;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Key;
import io.druid.initialization.DruidModule;
import io.druid.segment.loading.DataSegmentKiller;
import io.druid.segment.loading.DataSegmentPusher;
import io.druid.segment.loading.LocalDataSegmentKiller;
import io.druid.segment.loading.LocalDataSegmentPuller;
import io.druid.segment.loading.LocalDataSegmentPusher;
import io.druid.segment.loading.LocalDataSegmentPusherConfig;
import io.druid.segment.loading.LocalLoadSpec;
import io.druid.segment.loading.SegmentLoaderLocalCacheManager;
import io.druid.segment.loading.SegmentLoader;

import java.util.List;

/**
 */
public class LocalDataStorageDruidModule implements DruidModule
{
  public static final String SCHEME = "local";

  @Override
  public void configure(Binder binder)
  {
    binder.bind(SegmentLoader.class).to(SegmentLoaderLocalCacheManager.class).in(LazySingleton.class);

    bindDeepStorageLocal(binder);

    PolyBind.createChoice(
        binder, "druid.storage.type", Key.get(DataSegmentPusher.class), Key.get(LocalDataSegmentPusher.class)
    );
  }

  private static void bindDeepStorageLocal(Binder binder)
  {
    Binders.dataSegmentPullerBinder(binder)
           .addBinding(SCHEME)
           .to(LocalDataSegmentPuller.class)
           .in(LazySingleton.class);

    PolyBind.optionBinder(binder, Key.get(DataSegmentKiller.class))
            .addBinding(SCHEME)
            .to(LocalDataSegmentKiller.class)
            .in(LazySingleton.class);

    PolyBind.optionBinder(binder, Key.get(DataSegmentPusher.class))
            .addBinding("local")
            .to(LocalDataSegmentPusher.class)
            .in(LazySingleton.class);
    JsonConfigProvider.bind(binder, "druid.storage", LocalDataSegmentPusherConfig.class);
  }

  @Override
  public List<? extends com.fasterxml.jackson.databind.Module> getJacksonModules()
  {
    return ImmutableList.of(
        new com.fasterxml.jackson.databind.Module()
        {
          @Override
          public String getModuleName()
          {
            return "DruidLocalStorage-" + System.identityHashCode(this);
          }

          @Override
          public Version version()
          {
            return Version.unknownVersion();
          }

          @Override
          public void setupModule(SetupContext context)
          {
            context.registerSubtypes(LocalLoadSpec.class);
          }
        }
    );
  }
}
