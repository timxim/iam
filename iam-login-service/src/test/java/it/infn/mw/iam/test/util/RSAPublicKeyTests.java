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
package it.infn.mw.iam.test.util;

import static it.infn.mw.iam.test.SshKeyUtils.sshKeys;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import it.infn.mw.iam.util.ssh.InvalidSshKeyException;
import it.infn.mw.iam.util.ssh.RSAPublicKeyUtils;

public class RSAPublicKeyTests {

  @Test
  public void testMD5Fingerprint() {

    String fp = RSAPublicKeyUtils.getMD5Fingerprint(sshKeys.get(0).key);
    assertThat(fp, equalTo(sshKeys.get(0).fingerprintMDS));
    fp = RSAPublicKeyUtils.getMD5Fingerprint(sshKeys.get(1).key);
    assertThat(fp, equalTo(sshKeys.get(1).fingerprintMDS));
  }

  @Test
  public void testFormattedMD5Fingerprint() {

    String fp = RSAPublicKeyUtils.getFormattedMD5Fingerprint(sshKeys.get(0).key);
    assertThat(fp, equalTo(sshKeys.get(0).fingerprintMD5Formatted));
    fp = RSAPublicKeyUtils.getFormattedMD5Fingerprint(sshKeys.get(1).key);
    assertThat(fp, equalTo(sshKeys.get(1).fingerprintMD5Formatted));
  }

  @Test
  public void testSHA256Fingerprint() {

    String fp = RSAPublicKeyUtils.getSHA256Fingerprint(sshKeys.get(0).key);
    assertThat(fp, equalTo(sshKeys.get(0).fingerprintSHA256));
    fp = RSAPublicKeyUtils.getSHA256Fingerprint(sshKeys.get(1).key);
    assertThat(fp, equalTo(sshKeys.get(1).fingerprintSHA256));
  }

  @Test
  public void testMD5FingerprintInvalidKeyError() {

    try {
      RSAPublicKeyUtils.getMD5Fingerprint("This is not an encoded base64 key");
      fail("InvalidSshKeyException expected");
    } catch (InvalidSshKeyException e) {
    }
  }

  @Test
  public void testFormattedMD5FingerprintInvalidKeyError() {

    try {
      RSAPublicKeyUtils.getFormattedMD5Fingerprint("This is not an encoded base64 key");
      fail("InvalidSshKeyException expected");
    } catch (InvalidSshKeyException e) {
    }
  }

  @Test
  public void testSHA26FingerprintInvalidKeyError() {

    try {
      RSAPublicKeyUtils.getSHA256Fingerprint("This is not an encoded base64 key");
      fail("InvalidSshKeyException expected");
    } catch (InvalidSshKeyException e) {
    }
  }
}
