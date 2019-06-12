/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.flowing.retail.payment.engine.dto;

import java.util.HashMap;
import java.util.Map;

public class MessageCorrelationDto {

  String messageName;
  boolean resultEnabled = true;
  Map<String, VariableValue> processVariables = new HashMap<>();

  public Map<String, VariableValue> getProcessVariables() {
    return processVariables;
  }

  public void setProcessVariables(Map<String, VariableValue> processVariables) {
    this.processVariables = processVariables;
  }

  public String getMessageName() {
    return messageName;
  }

  public void setMessageName(String messageName) {
    this.messageName = messageName;
  }

  public boolean isResultEnabled() {
    return resultEnabled;
  }

  public void setResultEnabled(boolean resultEnabled) {
    this.resultEnabled = resultEnabled;
  }
}
