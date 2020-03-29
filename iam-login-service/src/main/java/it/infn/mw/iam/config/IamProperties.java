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
package it.infn.mw.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "iam")
public class IamProperties {

  public static class Logo {
    private String url = "resources/images/indigo-logo.png";
    private int dimension = 200;
    private int height = 200;
    private int width = 200;
    
    public String getUrl() {
      return url;
    }
    
    public void setUrl(String url) {
      this.url = url;
    }
    
    public int getDimension() {
      return dimension;
    }
    
    public void setDimension(int dimension) {
      this.dimension = dimension;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }
    
  }
  
  public static class LocalResources {
    
    private boolean enable = false;
    private String location;
    
    public boolean isEnable() {
      return enable;
    }

    public void setEnable(boolean enable) {
      this.enable = enable;
    }
    
    public String getLocation() {
      return location;
    }
    
    public void setLocation(String location) {
      this.location = location;
    }
  }
  
  private String baseUrl;
  
  private LocalResources localResources = new LocalResources();
  
  private Logo logo = new Logo();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }
  
  public LocalResources getLocalResources() {
    return localResources;
  }
  
  public void setLocalResources(LocalResources localResources) {
    this.localResources = localResources;
  }
  
  public Logo getLogo() {
    return logo;
  }
  
  public void setLogo(Logo logo) {
    this.logo = logo;
  }
}