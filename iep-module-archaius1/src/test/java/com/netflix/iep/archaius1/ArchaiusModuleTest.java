/*
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.iep.archaius1;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.archaius.Config;
import com.netflix.archaius.ConfigListener;
import com.netflix.archaius.annotations.OverrideLayer;
import com.netflix.archaius.annotations.RuntimeLayer;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.config.SettableConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.iep.archaius2.OverrideModule;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class ArchaiusModuleTest {

  private Module overrideModule = new AbstractModule() {
    @Override protected void configure() {
      bind(com.typesafe.config.Config.class).toInstance(ConfigFactory.parseString("a=b\nc=d"));

      DefaultSettableConfig dynamic = new DefaultSettableConfig();
      dynamic.setProperty("c", "dynamic");

      bind(DefaultSettableConfig.class).annotatedWith(OverrideLayer.class).toInstance(dynamic);
      bind(Config.class).annotatedWith(OverrideLayer.class).toInstance(dynamic);
    }
  };

  private Module testModule = Modules.override(new ArchaiusModule(), new OverrideModule()).with(overrideModule);

  @Before
  public void init() {
    ConfigurationManager.getConfigInstance().clear();
  }

  @Test
  public void getValues() {
    Configuration cfg = Guice.createInjector(testModule).getInstance(Configuration.class);
    Assert.assertEquals("b", cfg.getString("a"));
    Assert.assertEquals("dynamic", cfg.getString("c"));
  }

  @Test
  public void getValueRuntime() {
    Key<SettableConfig> key = Key.get(SettableConfig.class, RuntimeLayer.class);
    Injector injector = Guice.createInjector(testModule);
    SettableConfig runtime = injector.getInstance(key);
    Configuration root = injector.getInstance(Configuration.class);

    Assert.assertEquals("b", root.getString("a"));
    Assert.assertEquals("dynamic", root.getString("c"));

    runtime.setProperty("a", "runtime");
    runtime.setProperty("c", "runtime");
    Assert.assertEquals("runtime", root.getString("a"));
    Assert.assertEquals("runtime", root.getString("c"));

    runtime.clearProperty("a");
    Assert.assertEquals("b", root.getString("a"));
    Assert.assertEquals("runtime", root.getString("c"));
  }
}
