/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UserOperationLogEntryEngineDto implements EngineDto {
  private String id;
  private String processDefinitionId;
  private String processDefinitionKey;
  private String processInstanceId;
  private String taskId;
  private String userId;
  private OffsetDateTime timestamp;
  private String operationType;
  private String entityType;
  private String property;
  private String orgValue;
  private String newValue;
}
