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
package io.flowing.retail.payment.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import io.flowing.retail.payment.engine.dto.DeploymentDto;
import io.flowing.retail.payment.engine.dto.MessageCorrelationDto;
import io.flowing.retail.payment.engine.dto.ProcessInstanceEngineDto;
import io.flowing.retail.payment.engine.dto.TaskDto;
import io.flowing.retail.payment.engine.dto.UserCredentialsDto;
import io.flowing.retail.payment.engine.dto.UserDto;
import io.flowing.retail.payment.engine.dto.UserProfileDto;
import io.flowing.retail.payment.engine.dto.VariableValue;
import io.flowing.retail.payment.engine.dto.engine.AuthorizationDto;
import io.flowing.retail.payment.engine.dto.engine.DecisionDefinitionEngineDto;
import io.flowing.retail.payment.engine.dto.engine.GroupDto;
import io.flowing.retail.payment.engine.dto.engine.HistoricActivityInstanceEngineDto;
import io.flowing.retail.payment.engine.dto.engine.HistoricProcessInstanceDto;
import io.flowing.retail.payment.engine.dto.engine.HistoricUserTaskInstanceDto;
import io.flowing.retail.payment.engine.dto.engine.ProcessDefinitionEngineDto;
import io.flowing.retail.payment.engine.dto.engine.ProcessDefinitionXmlEngineDto;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleEngineClient {

  private static final int MAX_WAIT = 10;
  private static final String COUNT = "count";
  private static final CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
  public static final String DEFAULT_USERNAME = "demo";
  public static final String DEFAULT_PASSWORD = "demo";
  private static final String ENGINE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(ENGINE_DATE_FORMAT);

  public SimpleEngineClient() {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleDateFormat df = new SimpleDateFormat(ENGINE_DATE_FORMAT);
    objectMapper.setDateFormat(df);
    final JavaTimeModule javaTimeModule = new JavaTimeModule();
    objectMapper.registerModule(javaTimeModule);
    javaTimeModule.addSerializer(OffsetDateTime.class, new CustomSerializer(DATE_TIME_FORMATTER));
    javaTimeModule.addDeserializer(OffsetDateTime.class, new CustomDeserializer(DATE_TIME_FORMATTER));
  }

  private Logger logger = LoggerFactory.getLogger(SimpleEngineClient.class);

  private ObjectMapper objectMapper = new ObjectMapper();


  public UUID createIndependentUserTask() throws IOException {
    final UUID taskId = UUID.randomUUID();
    final HttpPost completePost = new HttpPost(getEngineUrl() + "/task/create");
    completePost.setEntity(new StringEntity(
      String.format("{\"id\":\"%s\",\"name\":\"name\"}", taskId.toString())
    ));
    completePost.addHeader("Content-Type", "application/json");
    try (CloseableHttpResponse response = getHttpClient().execute(completePost)) {
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new RuntimeException(
          "Could not create user task! Status-code: " + response.getStatusLine().getStatusCode()
        );
      }
    }
    return taskId;
  }

  public void finishAllRunningUserTasks() {
    finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }

  public void finishAllRunningUserTasks(final String user, final String password) {
    finishAllRunningUserTasks(user, password, null);
  }

  public void finishAllRunningUserTasks(final String processInstanceId) {
    finishAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceId);
  }

  public void finishAllRunningUserTasks(final String user, final String password, final String processInstanceId) {
    final BasicCredentialsProvider credentialsProvider = getBasicCredentialsProvider(user, password);
    try (final CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setDefaultCredentialsProvider(credentialsProvider).build()
    ) {
      final List<TaskDto> tasks = getUserTasks(httpClient, processInstanceId);
      for (TaskDto task : tasks) {
        claimAndCompleteUserTask(httpClient, task);
      }
    } catch (IOException e) {
      logger.error("Error while trying to create http client auth authentication!", e);
    }
  }

  public void claimAllRunningUserTasks(final String processInstanceId) {
    claimAllRunningUserTasks(DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceId);
  }

  public void claimAllRunningUserTasks(final String user, final String password, final String processInstanceId) {
    final BasicCredentialsProvider credentialsProvider = getBasicCredentialsProvider(user, password);
    try (final CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setDefaultCredentialsProvider(credentialsProvider).build()
    ) {
      final List<TaskDto> tasks = getUserTasks(httpClient, processInstanceId);
      for (TaskDto task : tasks) {
        claimUserTask(httpClient, user, task);
      }
    } catch (IOException e) {
      logger.error("Error while trying to create http client auth authentication!", e);
    }
  }

  public void completeUserTaskWithoutClaim(final String processInstanceId) {
    completeUserTaskWithoutClaim(DEFAULT_USERNAME, DEFAULT_PASSWORD, processInstanceId);
  }

  public void completeUserTaskWithoutClaim(final String user, final String password, final String processInstanceId) {
    final BasicCredentialsProvider credentialsProvider = getBasicCredentialsProvider(user, password);
    try (final CloseableHttpClient httpClient = HttpClientBuilder.create()
      .setDefaultCredentialsProvider(credentialsProvider).build()
    ) {
      final List<TaskDto> tasks = getUserTasks(httpClient, processInstanceId);
      for (TaskDto task : tasks) {
        completeUserTask(httpClient, task);
      }
    } catch (IOException e) {
      logger.error("Error while trying to complete user task!", e);
    }
  }

  private BasicCredentialsProvider getBasicCredentialsProvider(final String user, final String password) {
    final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    return credentialsProvider;
  }

  private List<TaskDto> getUserTasks(final CloseableHttpClient authenticatingClient,
                                     final String processInstanceIdFilter) {
    final List<TaskDto> tasks;
    try {
      final URIBuilder uriBuilder = new URIBuilder(getTaskListUri());
      if (processInstanceIdFilter != null) {
        uriBuilder.addParameter("processInstanceId", processInstanceIdFilter);
      }
      final HttpGet get = new HttpGet(uriBuilder.build());
      try (CloseableHttpResponse response = authenticatingClient.execute(get)) {
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        // @formatter:off
        tasks = objectMapper.readValue(responseString, new TypeReference<List<TaskDto>>() {});
        // @formatter:on
      } catch (IOException e) {
        throw new RuntimeException("Error while trying to finish the user task!!");
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException("Error while trying to create task list url !!");
    }
    return tasks;
  }

  private String getTaskListUri() {
    return getEngineUrl() + "/task";
  }

  private void claimAndCompleteUserTask(final CloseableHttpClient authenticatingClient, final TaskDto task)
    throws IOException {
    claimUserTaskAsDefaultUser(authenticatingClient, task);
    completeUserTask(authenticatingClient, task);
  }

  private void claimUserTaskAsDefaultUser(final CloseableHttpClient authenticatingClient, final TaskDto task)
    throws IOException {
    claimUserTask(authenticatingClient, DEFAULT_USERNAME, task);
  }

  private void claimUserTask(final CloseableHttpClient authenticatingClient, final String userId, final TaskDto task)
    throws IOException {
    HttpPost claimPost = new HttpPost(getSecuredClaimTaskUri(task.getId()));
    claimPost.setEntity(new StringEntity("{ \"userId\" : \"" + userId + "\" }"));
    claimPost.addHeader("Content-Type", "application/json");
    try (CloseableHttpResponse response = authenticatingClient.execute(claimPost)) {
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new RuntimeException(
          "Could not claim user task! Status-code: " + response.getStatusLine().getStatusCode()
        );
      }
    }
  }

  private void completeUserTask(final CloseableHttpClient authenticatingClient, final TaskDto task)
    throws IOException {
    HttpPost completePost = new HttpPost(getSecuredCompleteTaskUri(task.getId()));
    completePost.setEntity(new StringEntity("{}"));
    completePost.addHeader("Content-Type", "application/json");
    try (CloseableHttpResponse response = authenticatingClient.execute(completePost)) {
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new RuntimeException(
          "Could not complete user task! Status-code: " + response.getStatusLine().getStatusCode()
        );
      }
    }
  }

  private String getSecuredClaimTaskUri(final String taskId) {
    return getSecuredEngineUrl() + "/task/" + taskId + "/claim";
  }

  private String getSecuredCompleteTaskUri(final String taskId) {
    return getSecuredEngineUrl() + "/task/" + taskId + "/complete";
  }

  public String getProcessDefinitionId() {
    CloseableHttpClient client = getHttpClient();
    HttpRequestBase get = new HttpGet(getProcessDefinitionUri());
    CloseableHttpResponse response;
    try {
      response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      List<ProcessDefinitionEngineDto> procDefs =
        objectMapper.readValue(responseString, new TypeReference<List<ProcessDefinitionEngineDto>>() {
        });
      response.close();
      return procDefs.get(0).getId();
    } catch (IOException e) {
      throw new RuntimeException("Could not fetch the process definition!", e);
    }
  }

  public List<ProcessDefinitionEngineDto> getLatestProcessDefinitions() {
    CloseableHttpClient client = getHttpClient();
    URI uri;
    try {
      uri = new URIBuilder(getProcessDefinitionUri())
        .setParameter("latestVersion", "true")
        .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Could not create URI!", e);
    }
    HttpRequestBase get = new HttpGet(uri);
    CloseableHttpResponse response;
    try {
      response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      List<ProcessDefinitionEngineDto> procDefs =
        objectMapper.readValue(responseString, new TypeReference<List<ProcessDefinitionEngineDto>>() {
        });
      response.close();
      return procDefs;
    } catch (IOException e) {
      throw new RuntimeException("Could not fetch the process definition!", e);
    }
  }

  public ProcessDefinitionXmlEngineDto getProcessDefinitionXml(String processDefinitionId) {
    CloseableHttpClient client = getHttpClient();
    HttpRequestBase get = new HttpGet(getProcessDefinitionXmlUri(processDefinitionId));
    CloseableHttpResponse response;
    try {
      response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      ProcessDefinitionXmlEngineDto xml =
        objectMapper.readValue(responseString, ProcessDefinitionXmlEngineDto.class);
      response.close();
      return xml;
    } catch (IOException e) {
      String errorMessage =
        String.format("Could not fetch the process definition xml for id [%s]!", processDefinitionId);
      throw new RuntimeException(errorMessage, e);
    }
  }

  public ProcessInstanceEngineDto deployAndStartProcess(BpmnModelInstance bpmnModelInstance) {
    return deployAndStartProcess(bpmnModelInstance, null);
  }

  public ProcessInstanceEngineDto deployAndStartProcess(BpmnModelInstance bpmnModelInstance, String tenantId) {
    return deployAndStartProcessWithVariables(bpmnModelInstance, new HashMap<>(), tenantId);
  }


  public ProcessInstanceEngineDto deployAndStartProcessWithVariables(BpmnModelInstance bpmnModelInstance,
                                                                     Map<String, Object> variables) {
    return deployAndStartProcessWithVariables(bpmnModelInstance, variables, null);
  }

  public ProcessInstanceEngineDto deployAndStartProcessWithVariables(BpmnModelInstance bpmnModelInstance,
                                                                     Map<String, Object> variables,
                                                                     String tenantId) {
    return deployAndStartProcessWithVariables(bpmnModelInstance, variables, "aBusinessKey", tenantId);
  }

  public ProcessInstanceEngineDto deployAndStartProcessWithVariables(BpmnModelInstance bpmnModelInstance,
                                                                     Map<String, Object> variables,
                                                                     String businessKey,
                                                                     String tenantId) {
    final CloseableHttpClient client = getHttpClient();
    final DeploymentDto deployment = deployProcess(bpmnModelInstance, client, tenantId);
    final List<ProcessDefinitionEngineDto> procDefs = getAllProcessDefinitions(deployment, client);
    final ProcessDefinitionEngineDto processDefinitionEngineDto = procDefs.get(0);
    final ProcessInstanceEngineDto processInstanceDto = startProcessInstance(
      processDefinitionEngineDto.getId(), client, variables, businessKey
    );
    processInstanceDto.setProcessDefinitionKey(processDefinitionEngineDto.getKey());
    processInstanceDto.setProcessDefinitionVersion(String.valueOf(processDefinitionEngineDto.getVersion()));

    return processInstanceDto;
  }

  public HistoricProcessInstanceDto getHistoricProcessInstance(String processInstanceId) {
    CloseableHttpClient client = getHttpClient();
    HttpRequestBase get = new HttpGet(getHistoricGetProcessInstanceUri(processInstanceId));
    HistoricProcessInstanceDto processInstanceDto = new HistoricProcessInstanceDto();
    try {
      CloseableHttpResponse response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      processInstanceDto = objectMapper.readValue(responseString, new TypeReference<HistoricProcessInstanceDto>() {
      });
      response.close();
    } catch (IOException e) {
      logger.error("Could not get process definition for process instance: " + processInstanceId, e);
    }
    return processInstanceDto;
  }

  public List<HistoricActivityInstanceEngineDto> getHistoricActivityInstances() {
    CloseableHttpClient client = getHttpClient();
    HttpRequestBase get = new HttpGet(getHistoricGetActivityInstanceUri());
    try (CloseableHttpResponse response = client.execute(get)) {
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      return objectMapper.readValue(responseString, new TypeReference<List<HistoricActivityInstanceEngineDto>>() {
      });
    } catch (IOException e) {
      throw new RuntimeException("Could not fetch historic activity instances", e);
    }
  }

  public List<HistoricActivityInstanceEngineDto> getHistoricActivityInstances(Map<String, String> parameters) {
    CloseableHttpClient client = getHttpClient();
    try {
      URIBuilder builder = new URIBuilder(getHistoricGetActivityInstanceUri());

      for (Map.Entry<String, String> stringStringEntry : parameters.entrySet()) {
        builder.addParameter(stringStringEntry.getKey(), stringStringEntry.getValue());
      }
      HttpRequestBase get = new HttpGet(builder.build());
      try (CloseableHttpResponse response = client.execute(get)) {
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        return objectMapper.readValue(responseString, new TypeReference<List<HistoricActivityInstanceEngineDto>>() {
        });
      }

    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Could not fetch historic activity instances", e);
    }
  }

  public void deleteHistoricProcessInstance(String processInstanceId) {
    CloseableHttpClient client = getHttpClient();
    HttpDelete delete = new HttpDelete(getHistoricGetProcessInstanceUri(processInstanceId));
    try {
      CloseableHttpResponse response = client.execute(delete);
      if (response.getStatusLine().getStatusCode() != 204) {
        logger.error(
          "Could not delete process definition for process instance [{}]. Reason: wrong response code [{}]",
          processInstanceId,
          response.getStatusLine().getStatusCode()
        );
      }
    } catch (Exception e) {
      logger.error("Could not delete process definition for process instance: " + processInstanceId, e);
    }
  }

  public List<HistoricUserTaskInstanceDto> getHistoricTaskInstances(String processInstanceId) {
    return getHistoricTaskInstances(processInstanceId, null);
  }

  public List<HistoricUserTaskInstanceDto> getHistoricTaskInstances(String processInstanceId,
                                                                    String taskDefinitionKey) {
    try {
      final URIBuilder historicGetProcessInstanceUriBuilder = new URIBuilder(getHistoricTaskInstanceUri())
        .addParameter("processInstanceId", processInstanceId);

      if (taskDefinitionKey != null) {
        historicGetProcessInstanceUriBuilder.addParameter("taskDefinitionKey", taskDefinitionKey);
      }

      final HttpRequestBase get = new HttpGet(historicGetProcessInstanceUriBuilder.build());
      try (final CloseableHttpResponse response = getHttpClient().execute(get)) {
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        // @formatter:off
        final List<HistoricUserTaskInstanceDto> historicUserTaskInstanceDto = objectMapper.readValue(
          responseString,
          new TypeReference<List<HistoricUserTaskInstanceDto>>() {}
        );
        return historicUserTaskInstanceDto;
        // @formatter:on
      } catch (IOException e) {
        throw new RuntimeException(
          "Could not get process definition for process instance: " + processInstanceId, e
        );
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException("Failed building task instance url", e);
    }

  }

  public void externallyTerminateProcessInstance(String processInstanceId) {
    CloseableHttpClient client = getHttpClient();
    HttpDelete delete = new HttpDelete(getGetProcessInstanceUri(processInstanceId));
    try {
      CloseableHttpResponse response = client.execute(delete);
      if (response.getStatusLine().getStatusCode() != 204) {
        logger.error(
          "Could not cancel process definition for process instance [{}]. Reason: wrong response code [{}]",
          processInstanceId,
          response.getStatusLine().getStatusCode()
        );
      }
    } catch (Exception e) {
      logger.error("Could not cancel process definition for process instance: " + processInstanceId, e);
    }
  }

  public void deleteVariableInstanceForProcessInstance(String variableName, String processInstanceId) {
    CloseableHttpClient client = getHttpClient();
    HttpDelete delete = new HttpDelete(getVariableDeleteUri(variableName, processInstanceId));
    try {
      CloseableHttpResponse response = client.execute(delete);
      if (response.getStatusLine().getStatusCode() != 204) {
        logger.error(
          "Could not delete variable [{}] for process instance [{}]. Reason: wrong response code [{}]",
          variableName,
          processInstanceId,
          response.getStatusLine().getStatusCode()
        );
      }
    } catch (Exception e) {
      logger.error("Could not delete variable for process instance: " + processInstanceId, e);
    }
  }

  private CloseableHttpClient getHttpClient() {
    return closeableHttpClient;
  }


  public String deployProcessAndGetId(BpmnModelInstance modelInstance) {
    ProcessDefinitionEngineDto processDefinitionId = deployProcessAndGetProcessDefinition(modelInstance, null);
    return processDefinitionId.getId();
  }

  public ProcessDefinitionEngineDto deployProcessAndGetProcessDefinition(BpmnModelInstance modelInstance) {
    return deployProcessAndGetProcessDefinition(modelInstance, null);
  }

  public ProcessDefinitionEngineDto deployProcessAndGetProcessDefinition(BpmnModelInstance modelInstance,
                                                                         String tenantId) {
    CloseableHttpClient client = getHttpClient();
    DeploymentDto deploymentDto = deployProcess(modelInstance, client, tenantId);
    return getProcessDefinitionEngineDto(deploymentDto, client);
  }

  private DeploymentDto deployProcess(BpmnModelInstance bpmnModelInstance,
                                      CloseableHttpClient client,
                                      String tenantId) {
    String process = Bpmn.convertToString(bpmnModelInstance);
    HttpPost deploymentRequest = createDeploymentRequest(process, "test.bpmn", tenantId);
    DeploymentDto deployment = new DeploymentDto();
    try {
      CloseableHttpResponse response = client.execute(deploymentRequest);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Something really bad happened during deployment, " +
                                     "could not create a deployment!");
      }
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      deployment = objectMapper.readValue(responseString, DeploymentDto.class);
      response.close();
    } catch (IOException e) {
      logger.error("Error during deployment request! Could not deploy the given process model!", e);
    }
    return deployment;
  }

  public void startDecisionInstance(String decisionDefinitionId) {
    CloseableHttpClient client = getHttpClient();
    startDecisionInstance(decisionDefinitionId, client, new HashMap<String, Object>() {{
      put("amount", 200);
      put("invoiceCategory", "Misc");
    }});
  }

  public void startDecisionInstance(String decisionDefinitionId, Map<String, Object> variables) {
    CloseableHttpClient client = getHttpClient();
    startDecisionInstance(decisionDefinitionId, client, variables);
  }

  private void startDecisionInstance(String decisionDefinitionId,
                                     CloseableHttpClient client,
                                     Map<String, Object> variables) {
    final HttpPost post = new HttpPost(getStartDecisionInstanceUri(decisionDefinitionId));
    post.addHeader("Content-Type", "application/json");
    final Map<String, Object> requestBodyAsMap = convertVariableMap(variables);

    final String requestBodyAsJson;
    try {
      requestBodyAsJson = objectMapper.writeValueAsString(requestBodyAsMap);
      post.setEntity(new StringEntity(requestBodyAsJson, ContentType.APPLICATION_JSON));
      try (final CloseableHttpResponse response = client.execute(post)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          String body = "";
          if (response.getEntity() != null) {
            body = EntityUtils.toString(response.getEntity());
          }
          throw new RuntimeException(
            "Could not start the decision definition. " +
              "Request: [" + post.toString() + "]. " +
              "Response: [" + body + "]"
          );
        }
      }
    } catch (IOException e) {
      final String message = "Could not start the given decision model!";
      logger.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  private HttpPost createDeploymentRequest(String process, String fileName) {
    return createDeploymentRequest(process, fileName, null);
  }

  private HttpPost createDeploymentRequest(String process, String fileName, String tenantId) {
    HttpPost post = new HttpPost(getDeploymentUri());
    final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder
      .create()
      .addTextBody("deployment-name", "deployment")
      .addTextBody("enable-duplicate-filtering", "false")
      .addTextBody("deployment-source", "process application")
      .addBinaryBody("data", process.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_OCTET_STREAM, fileName);

    if (tenantId != null) {
      multipartEntityBuilder.addTextBody("tenant-id", tenantId);
    }

    final HttpEntity entity = multipartEntityBuilder.build();
    post.setEntity(entity);
    return post;
  }

  private String getDeploymentUri() {
    return getEngineUrl() + "/deployment/create";
  }

  private String getStartProcessInstanceUri(String procDefId) {
    return getEngineUrl() + "/process-definition/" + procDefId + "/start";
  }

  public String correlateMessage(String messageName, Map<String, Object> variables) {
    HttpPost post = new HttpPost(getEngineUrl() + "/message/");
    post.setHeader("Content-type", "application/json");
    MessageCorrelationDto message = new MessageCorrelationDto();
    message.setMessageName(messageName);
    message.setProcessVariables(convertVariableValueMap(variables));
    StringEntity content;
    CloseableHttpResponse response = null;
    try {
      content = new StringEntity(objectMapper.writeValueAsString(message), Charset.defaultCharset());
      post.setEntity(content);
      response = getHttpClient().execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode > 204) {
        throw new RuntimeException("Warning: response code for correlating message should be 204, got " + statusCode
                                     + " instead");
      }
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      String processInstanceId = JsonPath.read(responseString, "$.[0].processInstance.id");
      return processInstanceId;
    } catch (Exception e) {
      logger.error("Error while trying to correlate message!", e);
      throw new RuntimeException("Was not able to correlate message!");
    } finally {
      closeResponse(response);
    }
  }

  private Map<String, VariableValue> convertVariableValueMap(Map<String, Object> plainVariables) {
    Map<String, VariableValue> variables = new HashMap<>();
    for (Map.Entry<String, Object> nameToValue : plainVariables.entrySet()) {
      Object value = nameToValue.getValue();
      VariableValue variableValue = new VariableValue(nameToValue.getValue(), getSimpleName(nameToValue));
      variables.put(nameToValue.getKey(), variableValue);
    }
    return variables;
  }

  private void closeResponse(CloseableHttpResponse response) {
    if (response != null) {
      try {
        response.close();
      } catch (IOException e) {
        logger.error("Can't close response", e);
      }
    }
  }

  private String getHistoricGetProcessInstanceUri(String processInstanceId) {
    return getEngineUrl() + "/history/process-instance/" + processInstanceId;
  }

  private String getHistoricGetActivityInstanceUri() {
    return getEngineUrl() + "/history/activity-instance/";
  }

  private String getHistoricTaskInstanceUri() {
    return getEngineUrl() + "/history/task";
  }

  private String getGetProcessInstanceUri(String processInstanceId) {
    return getEngineUrl() + "/process-instance/" + processInstanceId;
  }

  private String getVariableDeleteUri(String variableName, String processInstanceId) {
    return getEngineUrl() + "/process-instance/" + processInstanceId + "/variables/" + variableName;
  }

  private String getProcessDefinitionUri() {
    return getEngineUrl() + "/process-definition";
  }

  private String getProcessDefinitionXmlUri(String processDefinitionId) {
    return getProcessDefinitionUri() + "/" + processDefinitionId + "/xml";
  }

  private String getDecisionDefinitionUri() {
    return getEngineUrl() + "/decision-definition";
  }


  private String getStartDecisionInstanceUri(final String decisionDefinitionId) {
    return getEngineUrl() + "/decision-definition/" + decisionDefinitionId + "/evaluate";
  }

  private String getCountHistoryUri() {
    return getEngineUrl() + "/history/process-instance/count";
  }

  private String getEngineUrl() {
    return "http://localhost:8080/engine-rest";
  }

  private String getSecuredEngineUrl() {
    return getEngineUrl().replace("/engine-rest", "/engine-rest-secure");
  }

  private ProcessDefinitionEngineDto getProcessDefinitionEngineDto(DeploymentDto deployment,
                                                                   CloseableHttpClient client) {
    List<ProcessDefinitionEngineDto> processDefinitions = getAllProcessDefinitions(deployment, client);
    return processDefinitions.get(0);
  }

  private List<ProcessDefinitionEngineDto> getAllProcessDefinitions(DeploymentDto deployment,
                                                                    CloseableHttpClient client) {
    HttpRequestBase get = new HttpGet(getProcessDefinitionUri());
    URI uri = null;
    try {
      uri = new URIBuilder(get.getURI())
        .addParameter("deploymentId", deployment.getId())
        .build();
    } catch (URISyntaxException e) {
      logger.error("Could not build uri!", e);
    }
    get.setURI(uri);
    CloseableHttpResponse response = null;
    try {
      response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      return objectMapper.readValue(
        responseString,
        new TypeReference<List<ProcessDefinitionEngineDto>>() {
        }
      );
    } catch (IOException e) {
      String message = "Could not retrieve all process definitions!";
      logger.error(message, e);
      throw new RuntimeException(message, e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          String message = "Could not close response!";
          logger.error(message, e);
        }
      }
    }
  }

  public DecisionDefinitionEngineDto getDecisionDefinitionByDeployment(DeploymentDto deployment) {
    HttpRequestBase get = new HttpGet(getDecisionDefinitionUri());
    try {
      URI uri = new URIBuilder(get.getURI()).addParameter("deploymentId", deployment.getId()).build();
      get.setURI(uri);
      try (CloseableHttpResponse response = getHttpClient().execute(get)) {
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        final List<DecisionDefinitionEngineDto> decisionDefinitionEngineDtos = objectMapper.readValue(
          responseString, new TypeReference<List<DecisionDefinitionEngineDto>>() {
          }
        );
        return decisionDefinitionEngineDtos.get(0);
      }
    } catch (URISyntaxException e) {
      logger.error("Could not build uri!", e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      String message = "Could not retrieve all process definitions!";
      logger.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  public ProcessInstanceEngineDto startProcessInstance(String processDefinitionId) {
    return startProcessInstance(processDefinitionId, new HashMap<>());
  }

  public ProcessInstanceEngineDto startProcessInstance(String processDefinitionId, Map<String, Object> variables) {
    return startProcessInstance(processDefinitionId, variables, "aBusinessKey");
  }

  public ProcessInstanceEngineDto startProcessInstance(String processDefinitionId,
                                                       Map<String, Object> variables,
                                                       String businessKey) {
    CloseableHttpClient client = getHttpClient();
    return startProcessInstance(processDefinitionId, client, variables, businessKey);
  }

  private ProcessInstanceEngineDto startProcessInstance(String procDefId,
                                                        CloseableHttpClient client,
                                                        Map<String, Object> variables,
                                                        String businessKey) {
    HttpPost post = new HttpPost(getStartProcessInstanceUri(procDefId));
    post.addHeader("Content-Type", "application/json");
    Map<String, Object> requestBodyAsMap = convertVariableMap(variables);
    requestBodyAsMap.put("businessKey", businessKey);
    String requestBodyAsJson;
    try {
      requestBodyAsJson = objectMapper.writeValueAsString(requestBodyAsMap);
      post.setEntity(new StringEntity(requestBodyAsJson, ContentType.APPLICATION_JSON));
      try (CloseableHttpResponse response = client.execute(post)) {
        if (response.getStatusLine().getStatusCode() != 200) {
          String body = "";
          if (response.getEntity() != null) {
            body = EntityUtils.toString(response.getEntity());
          }
          throw new RuntimeException(
            "Could not start the process definition. " +
              "Request: [" + post.toString() + "]. " +
              "Response: [" + body + "]"
          );
        }
        String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
        ProcessInstanceEngineDto processInstanceEngineDto = objectMapper.readValue(
          responseString, ProcessInstanceEngineDto.class
        );
        return processInstanceEngineDto;
      }
    } catch (IOException e) {
      String message = "Could not start the given process model!";
      logger.error(message, e);
      throw new RuntimeException(message, e);
    }
  }

  private Map<String, Object> convertVariableMap(Map<String, Object> plainVariables) {
    Map<String, Object> variables = new HashMap<>();
    for (Map.Entry<String, Object> nameToValue : plainVariables.entrySet()) {
      Object value = nameToValue.getValue();
      Map<String, Object> fields = new HashMap<>();
      fields.put("value", nameToValue.getValue());
      fields.put("type", getSimpleName(nameToValue));
      variables.put(nameToValue.getKey(), fields);
    }
    Map<String, Object> variableWrapper = new HashMap<>();
    variableWrapper.put("variables", variables);
    return variableWrapper;
  }

  private String getSimpleName(Map.Entry<String, Object> nameToValue) {

    String simpleName = nameToValue.getValue().getClass().getSimpleName();
    if (nameToValue.getValue().getClass().equals(OffsetDateTime.class)) {
      simpleName = Date.class.getSimpleName();
    }
    return simpleName;
  }

  public void waitForAllProcessesToFinish() throws Exception {
    CloseableHttpClient client = getHttpClient();
    boolean done = false;
    HttpRequestBase get = new HttpGet(getCountHistoryUri());
    URI uri = null;
    try {
      uri = new URIBuilder(get.getURI())
        .addParameter("unfinished", "true")
        .build();
    } catch (URISyntaxException e) {
      logger.error("Could not build uri!", e);
    }
    get.setURI(uri);
    int iterations = 0;
    Thread.sleep(100);
    while (!done && iterations < MAX_WAIT) {
      CloseableHttpResponse response = client.execute(get);
      String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
      HashMap<String, Object> parsed = objectMapper.readValue(
        responseString,
        new TypeReference<HashMap<String, Object>>() {
        }
      );
      if (!parsed.containsKey(COUNT)) {
        throw new RuntimeException("Engine could not count PIs");
      }
      if (Integer.valueOf(parsed.get(COUNT).toString()) != 0) {
        Thread.sleep(100);
        iterations = iterations + 1;
      } else {
        done = true;
      }
      response.close();
    }
  }

  public void addUser(String username, String password) {
    UserDto userDto = constructDemoUserDto(username, password);
    try {
      CloseableHttpClient client = getHttpClient();
      HttpPost httpPost = new HttpPost(getEngineUrl() + "/user/create");
      httpPost.addHeader("Content-Type", "application/json");

      httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(userDto), ContentType.APPLICATION_JSON));
      CloseableHttpResponse response = client.execute(httpPost);
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new RuntimeException("Wrong status code when trying to add user!");
      }
      response.close();
    } catch (Exception e) {
      logger.error("error creating user", e);
    }
  }

  public void createAuthorization(AuthorizationDto authorizationDto) {
    try {
      CloseableHttpClient client = getHttpClient();
      HttpPost httpPost = new HttpPost(getEngineUrl() + "/authorization/create");
      httpPost.addHeader("Content-Type", "application/json");

      httpPost.setEntity(
        new StringEntity(objectMapper.writeValueAsString(authorizationDto), ContentType.APPLICATION_JSON)
      );
      CloseableHttpResponse response = client.execute(httpPost);
      response.close();
    } catch (IOException e) {
      logger.error("Could not create authorization", e);
    }
  }


  private UserDto constructDemoUserDto(String username, String password) {
    UserProfileDto profile = new UserProfileDto();
    profile.setEmail("foo@camunda.org");
    profile.setId(username);
    UserCredentialsDto credentials = new UserCredentialsDto();
    credentials.setPassword(password);
    UserDto userDto = new UserDto();
    userDto.setProfile(profile);
    userDto.setCredentials(credentials);
    return userDto;
  }

  public void createGroup(String id, String name, String type) {
    GroupDto groupDto = new GroupDto();
    groupDto.setId(id);
    groupDto.setName(name);
    groupDto.setType(type);

    try {
      CloseableHttpClient client = getHttpClient();
      HttpPost httpPost = new HttpPost(getEngineUrl() + "/group/create");
      httpPost.addHeader("Content-Type", "application/json");

      httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(groupDto), ContentType.APPLICATION_JSON));
      CloseableHttpResponse response = client.execute(httpPost);
      response.close();
    } catch (Exception e) {
      logger.error("error creating group", e);
    }
  }

  public void addUserToGroup(String userId, String groupId) {

    try {
      CloseableHttpClient client = getHttpClient();
      HttpPut put = new HttpPut(getEngineUrl() + "/group/" + groupId + "/members/" + userId);
      put.addHeader("Content-Type", "application/json");

      put.setEntity(new StringEntity("", ContentType.APPLICATION_JSON));
      CloseableHttpResponse response = client.execute(put);
      response.close();
    } catch (Exception e) {
      logger.error("error creating group members", e);
    }
  }



}