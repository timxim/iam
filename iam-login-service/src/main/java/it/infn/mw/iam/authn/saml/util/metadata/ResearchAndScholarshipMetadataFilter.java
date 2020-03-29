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
package it.infn.mw.iam.authn.saml.util.metadata;

import static java.util.Arrays.asList;

public class ResearchAndScholarshipMetadataFilter extends AttributeValueMetadataFilter {

  public static final String ENTITY_CATEGORY_ATTRIBUTE_NAME =
      "http://macedir.org/entity-category-support";

  public static final String R_S_ATTRIBUTE_VALUE =
      "http://refeds.org/category/research-and-scholarship";

  public ResearchAndScholarshipMetadataFilter() {
    super(ENTITY_CATEGORY_ATTRIBUTE_NAME, asList(R_S_ATTRIBUTE_VALUE));

  }

}
