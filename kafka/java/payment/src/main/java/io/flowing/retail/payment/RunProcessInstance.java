/*
 * Copyright © 2012 - 2018 camunda services GmbH and various authors (info@camunda.com)
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
package io.flowing.retail.payment;

import io.flowing.retail.payment.engine.SimpleEngineClient;
import io.flowing.retail.payment.engine.dto.engine.HistoricActivityInstanceEngineDto;
import io.flowing.retail.payment.engine.dto.engine.HistoricProcessInstanceDto;
import io.flowing.retail.payment.engine.dto.engine.ProcessDefinitionEngineDto;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RunProcessInstance {

  public static boolean run(String... args) throws Exception {
    SimpleEngineClient engineClient = new SimpleEngineClient();

    Optional<ProcessDefinitionEngineDto> definition = engineClient.getLatestProcessDefinitions()
      .stream()
      .filter(def -> def.getKey().equals("payment"))
      .findFirst();

    String definitionId = "";
    if (!definition.isPresent()) {
      definitionId = engineClient.deployProcessAndGetId(
        Bpmn.readModelFromStream(
          RunProcessInstance.class.getResourceAsStream("/payment.bpmn")));
    } else {
      definitionId = definition.get().getId();
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("amount", 12);
    variables.put("remainingAmount", 12);
    variables.put("refId", "some Random id");
    variables.put("paymentComplete", ThreadLocalRandom.current().nextBoolean());
    variables.put("correlationId", "some random correlation id");

    String processInstanceId = engineClient.correlateMessage("RetrievePaymentCommand", variables);

    HistoricProcessInstanceDto historicProcessInstance = new HistoricProcessInstanceDto();
    while (historicProcessInstance.getEndTime() == null) {
      historicProcessInstance = engineClient.getHistoricProcessInstance(processInstanceId);
      Thread.sleep(1000L);
    }

    Map<String, String> parameters = new HashMap<>();
    parameters.put("processInstanceId", historicProcessInstance.getId());
    parameters.put("activityType", "noneEndEvent");
    List<HistoricActivityInstanceEngineDto> historicActivityInstances = engineClient.getHistoricActivityInstances(
      parameters);

    HistoricActivityInstanceEngineDto activity = historicActivityInstances.get(0);
    return activity.getActivityId().equals("paymentReceived");
  }
}
