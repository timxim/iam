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
package it.infn.mw.iam.test.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

import it.infn.mw.iam.core.IamDeliveryStatus;
import it.infn.mw.iam.core.time.TimeProvider;
import it.infn.mw.iam.notification.NotificationProperties;
import it.infn.mw.iam.notification.service.DefaultNotificationStoreService;
import it.infn.mw.iam.persistence.model.IamEmailNotification;
import it.infn.mw.iam.persistence.model.IamNotificationReceiver;
import it.infn.mw.iam.persistence.repository.IamEmailNotificationRepository;

@RunWith(MockitoJUnitRunner.class)
public class DefaultNotificationStoreServiceTests {

  public static final String TEST_0_EMAIL = "test0@test.example";
  public static final String TEST_1_EMAIL = "test1@test.example";
  public static final String TEST_EMAIL_SUBJECT = "Subject";
  public static final String TEST_EMAIL_BODY = "Body";

  public static final String IAM_MAIL_FROM = "iam@test.example";
  public static final String IAM_ADMIN_ADDRESS = "admin@test.example";

  @Mock
  IamEmailNotificationRepository notificationRepo;

  @Mock
  TimeProvider timeProvider;

  @Mock
  NotificationProperties properties;

  @InjectMocks
  DefaultNotificationStoreService service;

  @Captor
  ArgumentCaptor<Date> dateArgumentCaptor;

  @Captor
  ArgumentCaptor<IamDeliveryStatus> statusArgumentCaptor;

  @Captor
  ArgumentCaptor<Iterable<IamEmailNotification>> notificationCaptor;

  @Before
  public void setup() {

    when(properties.getMailFrom()).thenReturn(IAM_MAIL_FROM);
    when(properties.getAdminAddress()).thenReturn(IAM_ADMIN_ADDRESS);
    when(properties.getCleanupAge()).thenReturn(1);
  }

  @Test
  public void clearAllNotificationsClearsAllNotifications() {
    service.clearAllNotifications();
    verify(notificationRepo).deleteAll();
  }

  @Test
  public void countPendingNotificationsCallsTheRightRepo() {
    when(notificationRepo.countByDeliveryStatus(IamDeliveryStatus.PENDING)).thenReturn(15);
    assertThat(service.countPendingNotifications(), is(15));

  }

  @Test
  public void clearExpiredNotificationsClearsTheRightNotifications() {

    Date now = new Date();
    Date oneDayAfterNow = new Date(now.getTime() + TimeUnit.DAYS.toMillis(1));
    Date twoDaysAfterNow = new Date(now.getTime() + TimeUnit.DAYS.toMillis(2));
    String randomUuid = UUID.randomUUID().toString();


    IamEmailNotification notification = mock(IamEmailNotification.class);
    IamNotificationReceiver receiver = mock(IamNotificationReceiver.class);

    when(receiver.getIamEmailNotification()).thenReturn(notification);
    when(receiver.getEmailAddress()).thenReturn(TEST_0_EMAIL);

    when(notification.getBody()).thenReturn("Body");
    when(notification.getSubject()).thenReturn("Subject");
    when(notification.getDeliveryStatus()).thenReturn(IamDeliveryStatus.DELIVERED);
    when(notification.getCreationTime()).thenReturn(twoDaysAfterNow);
    when(notification.getLastUpdate()).thenReturn(oneDayAfterNow);
    when(notification.getUuid()).thenReturn(randomUuid);

    when(notificationRepo.findByStatusWithUpdateTime(Mockito.any(), Mockito.any()))
      .thenReturn(Arrays.asList(notification));

    when(timeProvider.currentTimeMillis()).thenReturn(twoDaysAfterNow.getTime());

    service.clearExpiredNotifications();

    verify(notificationRepo).findByStatusWithUpdateTime(statusArgumentCaptor.capture(),
        dateArgumentCaptor.capture());

    assertThat(statusArgumentCaptor.getValue(), is(IamDeliveryStatus.DELIVERED));
    assertThat(dateArgumentCaptor.getValue(), equalTo(oneDayAfterNow));

    verify(notificationRepo).delete(notificationCaptor.capture());

    List<IamEmailNotification> removedNotifications = Lists.newArrayList(notificationCaptor.getValue());
    assertThat(removedNotifications, hasSize(1));
    assertThat(removedNotifications, hasItem(notification));
    
  }
  
  @Test
  public void clearExpiredNotificationsDoesNotClearAnythingWhenThereAreNoExpiredNotifications() {

    Date now = new Date();
    Date oneDayAfterNow = new Date(now.getTime() + TimeUnit.DAYS.toMillis(1));
    Date twoDaysAfterNow = new Date(now.getTime() + TimeUnit.DAYS.toMillis(2));
    

    when(notificationRepo.findByStatusWithUpdateTime(Mockito.any(), Mockito.any()))
      .thenReturn(emptyList());

    when(timeProvider.currentTimeMillis()).thenReturn(twoDaysAfterNow.getTime());

    service.clearExpiredNotifications();

    verify(notificationRepo).findByStatusWithUpdateTime(statusArgumentCaptor.capture(),
        dateArgumentCaptor.capture());

    assertThat(statusArgumentCaptor.getValue(), is(IamDeliveryStatus.DELIVERED));
    assertThat(dateArgumentCaptor.getValue(), equalTo(oneDayAfterNow));

    verify(notificationRepo, never()).delete(notificationCaptor.capture());
    
    
  }


}
