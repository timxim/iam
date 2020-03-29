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
package it.infn.mw.iam.test.api.account.search.service;

import static it.infn.mw.iam.api.account.search.AccountSearchController.getSortByEmail;
import static it.infn.mw.iam.api.account.search.AccountSearchController.getSortByName;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.common.PagedResourceService;
import it.infn.mw.iam.persistence.model.IamAccount;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@Transactional
public class DefaultPagedAccountsServiceTests extends AccountServiceUtils {

  public final int ITEMS_PER_PAGE = 10;
  public final long TOTAL_TEST_ACCOUNTS = 253L;
  private final int LAST_PAGE_NUMBER = (int) Math.ceil(TOTAL_TEST_ACCOUNTS / ITEMS_PER_PAGE);
  private final int LAST_PAGE_SIZE = (int) (long) TOTAL_TEST_ACCOUNTS % ITEMS_PER_PAGE;
  private final int LAST_PAGE_OFFSET = (int) (long) Math.floorDiv(TOTAL_TEST_ACCOUNTS, ITEMS_PER_PAGE) * ITEMS_PER_PAGE;

  @Autowired
  private PagedResourceService<IamAccount> accountService;

  private Page<IamAccount> getPage(Pageable p) {

    Page<IamAccount> page = accountService.getPage(p);
    assertThat(page.getTotalElements(), equalTo(TOTAL_TEST_ACCOUNTS));
    assertThat(page.getNumber(), equalTo(p.getPageNumber()));
    int expectedSize = isLast(p) ? LAST_PAGE_SIZE : ITEMS_PER_PAGE;
    assertThat(page.getNumberOfElements(), equalTo(expectedSize));
    assertThat(page.getContent(), hasSize(expectedSize));
    return page;
  }

  private boolean isLast(Pageable p) {
    return p.getPageNumber() == LAST_PAGE_NUMBER;
  }

  @Test
  public void getAllPagesSortedByNameAsc() {

    Pageable op = new OffsetPageable(ITEMS_PER_PAGE, getSortByName(Sort.Direction.ASC));
    Pageable current = op.first();

    while (current.getOffset() <= TOTAL_TEST_ACCOUNTS) {
      Page<IamAccount> page = getPage(current);
      assertSortIsByNameAsc(page.getContent());
      current = current.next();
    }
  }

  @Test
  public void getAllPagesSortedByNameDesc() {

    Pageable op = new OffsetPageable(ITEMS_PER_PAGE, getSortByName(Sort.Direction.DESC));
    Pageable current = op.first();

    while (current.getOffset() <= TOTAL_TEST_ACCOUNTS) {
      Page<IamAccount> page = getPage(current);
      assertSortIsByNameDesc(page.getContent());
      current = current.next();
    }
  }

  @Test
  public void getAllPagesSortedByEmailAsc() {

    Pageable op = new OffsetPageable(ITEMS_PER_PAGE, getSortByEmail(Sort.Direction.ASC));
    Pageable current = op.first();

    while (current.getOffset() <= TOTAL_TEST_ACCOUNTS) {
      Page<IamAccount> page = getPage(current);
      assertSortIsByEmailAsc(page.getContent());
      current = current.next();
    }
  }

  @Test
  public void getAllPagesSortedByEmailDesc() {

    Pageable op = new OffsetPageable(ITEMS_PER_PAGE, getSortByEmail(Sort.Direction.DESC));
    Pageable current = op.first();

    while (current.getOffset() <= TOTAL_TEST_ACCOUNTS) {
      Page<IamAccount> page = getPage(current);
      assertSortIsByEmailDesc(page.getContent());
      current = current.next();
    }
  }

  @Test
  public void testGetFirstPageSortByNameAsc() {

    OffsetPageable op = new OffsetPageable(0, ITEMS_PER_PAGE, getSortByName(Sort.Direction.ASC));
    Page<IamAccount> page = accountService.getPage(op);
    assertSortIsByNameAsc(page.getContent());
  }

  @Test
  public void testGetFirstPageSortByNameDesc() {

    OffsetPageable op = new OffsetPageable(0, ITEMS_PER_PAGE, getSortByName(Sort.Direction.DESC));
    Page<IamAccount> page = accountService.getPage(op);
    assertSortIsByNameDesc(page.getContent());
  }

  @Test
  public void testGetLastPageSortByNameAsc() {

    OffsetPageable op = new OffsetPageable(LAST_PAGE_OFFSET, ITEMS_PER_PAGE, getSortByName(Sort.Direction.ASC));
    Page<IamAccount> page = accountService.getPage(op);
    assertSortIsByNameAsc(page.getContent());
  }


  @Test
  public void testGetLastPageSortByNameDesc() {

    OffsetPageable op = new OffsetPageable(LAST_PAGE_OFFSET, ITEMS_PER_PAGE, getSortByName(Sort.Direction.DESC));
    Page<IamAccount> page = accountService.getPage(op);
    assertSortIsByNameDesc(page.getContent());
  }

  @Test
  public void testFilterWithFullName() {

    OffsetPageable op = new OffsetPageable(0, ITEMS_PER_PAGE, getSortByName(Sort.Direction.ASC));
    Page<IamAccount> page = accountService.getPage(op, "Admin User");
    assertThat(page.getTotalElements(), equalTo(1L));
    assertThat(page.getNumber(), equalTo(0));
    assertThat(page.getNumberOfElements(), equalTo(1));
    assertThat(page.getContent(), hasSize(1));
    assertThat(page.getContent().get(0).getUsername(), equalTo("admin"));
  }

  @Test
  public void testFilterCountWithFullName() {

    long totalResults = accountService.count("Admin User");
    assertThat(totalResults, equalTo(1L));
  }
}
