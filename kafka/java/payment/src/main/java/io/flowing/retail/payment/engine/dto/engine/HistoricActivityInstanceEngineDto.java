/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto.engine;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class HistoricActivityInstanceEngineDto implements EngineDto {

  protected String id;
  protected String parentActivityInstanceId;
  protected String activityId;
  protected String activityName;
  protected String activityType;
  protected String processDefinitionKey;
  protected String processDefinitionId;
  protected String processInstanceId;
  protected String executionId;
  protected String taskId;
  protected String calledProcessInstanceId;
  protected String calledCaseInstanceId;
  protected String assignee;
  protected OffsetDateTime startTime;
  protected OffsetDateTime endTime;
  protected Long durationInMillis;
  protected Boolean canceled;
  protected Boolean completeScope;
  protected String tenantId;
}
