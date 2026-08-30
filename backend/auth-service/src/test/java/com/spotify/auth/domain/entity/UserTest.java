package com.spotify.auth.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void should_BeDisabledByDefault_when_NewUser() {
    User user = User.builder().build();
    assertFalse(user.isTwoFactorEnabled());
  }

  @Test
  void should_StoreSecretWithoutEnabling_when_PendingEnrollment() {
    User user = User.builder().build();
    user.storePendingTotpSecret("JBSWY3DPEHPK3PXP");
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
    assertFalse(user.isTwoFactorEnabled());
  }

  @Test
  void should_Enable2fa_When_ValidSecretProvided() {
    User user = User.builder().build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    assertTrue(user.isTwoFactorEnabled());
    assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
  }

  @Test
  void should_Disable2fa_When_TurnedOff() {
    User user = User.builder().build();
    user.enable2fa("JBSWY3DPEHPK3PXP");
    user.disable2fa();
    assertFalse(user.isTwoFactorEnabled());
    assertNull(user.getTotpSecret());
  }
}