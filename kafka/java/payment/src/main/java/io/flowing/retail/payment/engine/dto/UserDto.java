/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto;

public class UserDto {

  protected UserProfileDto profile;

  protected UserCredentialsDto credentials;


  // getters / setters /////////////////////////////
  
  public UserProfileDto getProfile() {
    return profile;
  }

  public void setProfile(UserProfileDto profile) {
    this.profile = profile;
  }

  public UserCredentialsDto getCredentials() {
    return credentials;
  }

  public void setCredentials(UserCredentialsDto credentials) {
    this.credentials = credentials;
  }

}
