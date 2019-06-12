/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProcessDefinitionEngineDto implements Serializable, EngineDto {

  protected String id;
  protected String key;
  protected String category;
  protected String description;
  protected String name;
  protected int version;
  protected String resource;
  protected String deploymentId;
  protected String diagram;
  protected boolean suspended;
  protected String tenantId;
  protected String versionTag;

  @JsonIgnore
  public String getVersionAsString() {
    return String.valueOf(version);
  }

  public Optional<String> getTenantId() {
    return Optional.ofNullable(tenantId);
  }
}
