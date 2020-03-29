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
package it.infn.mw.iam.api.common;

import org.springframework.data.domain.Sort;

public class PagingUtils {

  private PagingUtils() {
    // empty constructor
  }

  public static OffsetPageable buildPageRequest(Integer count, Integer startIndex,
      int maxPageSize) {
    return buildPageRequest(count, startIndex, maxPageSize, null);
  }
  
  public static OffsetPageable buildPageRequest(Integer count, Integer startIndex,
      int maxPageSize, Sort sort) {

    int validCount = 0;
    int validStartIndex = 1;

    if (count == null) {
      validCount = maxPageSize;
    } else {
      validCount = count;
      if (count < 0) {
        validCount = 0;
      } else if (count > maxPageSize) {
        validCount = maxPageSize;
      }
    }

    if (startIndex == null) {
      validStartIndex = 1;

    } else {

      validStartIndex = startIndex;
      if (startIndex < 0) {
        validStartIndex = 1;
      }
    }
    
    return new OffsetPageable(validStartIndex - 1, validCount, sort);
  }
}
