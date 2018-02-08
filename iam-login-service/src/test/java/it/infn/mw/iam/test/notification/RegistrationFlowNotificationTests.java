package it.infn.mw.iam.test.notification;

import static it.infn.mw.iam.test.util.AuthenticationUtils.adminAuthentication;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.account.password_reset.PasswordResetService;
import it.infn.mw.iam.notification.NotificationProperties;
import it.infn.mw.iam.persistence.model.IamEmailNotification;
import it.infn.mw.iam.persistence.repository.IamEmailNotificationRepository;
import it.infn.mw.iam.registration.PersistentUUIDTokenGenerator;
import it.infn.mw.iam.registration.RegistrationRequestDto;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.MockTimeProvider;
import it.infn.mw.iam.test.util.WithAnonymousUser;
import it.infn.mw.iam.test.util.notification.MockNotificationDelivery;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, NotificationTestConfig.class,
    CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithAnonymousUser
@TestPropertySource(properties = {"notification.disable=false"})
public class RegistrationFlowNotificationTests {

  @Autowired
  private NotificationProperties properties;

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;

  @Value("${iam.organisation.name}")
  private String organisationName;

  @Value("${iam.baseUrl}")
  private String baseUrl;

  @Autowired
  private IamEmailNotificationRepository notificationRepository;

  @Autowired
  private PasswordResetService passwordResetService;

  @Autowired
  private MockTimeProvider timeProvider;

  @Autowired
  private PersistentUUIDTokenGenerator generator;

  @Autowired
  private MockNotificationDelivery notificationDelivery;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mvc;

  @Before
  public void setUp() throws InterruptedException {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .alwaysDo(print())
      .apply(springSecurity())
      .build();
  }

  @After
  public void tearDown() throws InterruptedException {
    mockOAuth2Filter.cleanupSecurityContext();
    notificationDelivery.clearDeliveredNotifications();
  }

  @Test
  public void testSendWithEmptyQueue() {

    notificationDelivery.sendPendingNotifications();
    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(0));
  }

  @Test
  public void testApproveFlowNotifications() throws Exception {

    String username = "approve_flow";

    RegistrationRequestDto request = new RegistrationRequestDto();
    request.setGivenname("Approve flow");
    request.setFamilyname("Test");
    request.setEmail("approve_flow@example.org");
    request.setUsername(username);
    request.setNotes("Some short notes...");

    String responseJson = mvc
      .perform(post("/registration/create").contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(request)))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    request = mapper.readValue(responseJson, RegistrationRequestDto.class);

    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    IamEmailNotification message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("confirmation")));

    notificationDelivery.clearDeliveredNotifications();

    String confirmationKey = generator.getLastToken();

    mvc.perform(get("/registration/confirm/{token}", confirmationKey).contentType(APPLICATION_JSON))
      .andExpect(status().isOk());


    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("adminHandleRequest")));

    assertThat(message.getReceivers(), hasSize(1));
    assertThat(message.getReceivers().get(0).getEmailAddress(),
        equalTo(properties.getAdminAddress()));


    notificationDelivery.clearDeliveredNotifications();

    mvc.perform(post("/registration/{uuid}/APPROVED", request.getUuid())
      .with(authentication(adminAuthentication()))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("activated")));


  }

  @Test
  public void testRejectFlowNotifications() throws Exception {
    String username = "reject_flow";

    RegistrationRequestDto request = new RegistrationRequestDto();
    request.setGivenname("Reject flow");
    request.setFamilyname("Test");
    request.setEmail("reject_flow@example.org");
    request.setUsername(username);
    request.setNotes("Some short notes...");


    String responseJson = mvc
      .perform(post("/registration/create").contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(request)))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    request = mapper.readValue(responseJson, RegistrationRequestDto.class);

    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    IamEmailNotification message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("confirmation")));

    notificationDelivery.clearDeliveredNotifications();

    String confirmationKey = generator.getLastToken();

    mvc.perform(get("/registration/confirm/{token}", confirmationKey).contentType(APPLICATION_JSON))
      .andExpect(status().isOk());


    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("adminHandleRequest")));

    assertThat(message.getReceivers(), hasSize(1));
    assertThat(message.getReceivers().get(0).getEmailAddress(),
        equalTo(properties.getAdminAddress()));


    notificationDelivery.clearDeliveredNotifications();

    mvc.perform(post("/registration/{uuid}/REJECTED", request.getUuid())
      .with(authentication(adminAuthentication()))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk());

    notificationDelivery.sendPendingNotifications();

    assertThat(notificationDelivery.getDeliveredNotifications(), hasSize(1));

    message = notificationDelivery.getDeliveredNotifications().get(0);

    assertThat(message.getSubject(), equalTo(properties.getSubject().get("rejected")));

  }

}