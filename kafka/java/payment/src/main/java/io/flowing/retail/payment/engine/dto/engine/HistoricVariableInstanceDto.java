/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto.engine;

import lombok.Data;

@Data
public class HistoricVariableInstanceDto implements EngineDto {
  private String id;
  private String name;
  private String type;
  private String value;
  private String processDefinitionKey;
  private String processDefinitionId;
  private String processInstanceId;
  private String tenantId;
}
