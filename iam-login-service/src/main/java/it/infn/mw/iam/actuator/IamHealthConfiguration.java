/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2019
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
package it.infn.mw.iam.actuator;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.DataSourceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicator;
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamHealthConfiguration {

  @Autowired(required = false)
  private HealthAggregator healthAggregator = new OrderedHealthAggregator();

  @Autowired
  DataSource dataSource;

  @Autowired
  DiskSpaceHealthIndicatorProperties properties;

  @Bean
  public HealthEndpoint healthEndpoint() {
    Map<String, HealthIndicator> healthIndicators = new HashMap<>();

    healthIndicators.put("db", new DataSourceHealthIndicator(dataSource));
    healthIndicators.put("diskSpace", new DiskSpaceHealthIndicator(properties));

    return new HealthEndpoint(this.healthAggregator, healthIndicators);
  }

}
