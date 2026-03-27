package com.ike.sb4camunda8;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 26/3/2026
 */
@SpringBootTest(classes = Sb4camunda8Application.class)
@CamundaSpringProcessTest
class DemoWorkerProcessTest {
    @Autowired CamundaClient client;

    @Test
    void shouldCreateProcessInstance() {
        String bpmnXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:zeebe="http://camunda.org/schema/zeebe/1.0" targetNamespace="http://bpmn.io/schema/bpmn">
                    <bpmn:process id="Process_1" isExecutable="true">
                      <bpmn:startEvent id="Event_1hv93zx">
                        <bpmn:outgoing>Flow_0dbdhy1</bpmn:outgoing>
                      </bpmn:startEvent>
                      <bpmn:intermediateThrowEvent id="Event_0m39jw8">
                        <bpmn:incoming>Flow_159uaa6</bpmn:incoming>
                      </bpmn:intermediateThrowEvent>
                      <bpmn:sequenceFlow id="Flow_0dbdhy1" sourceRef="Event_1hv93zx" targetRef="Activity_1yqfnos" />
                      <bpmn:sequenceFlow id="Flow_159uaa6" sourceRef="Activity_1yqfnos" targetRef="Event_0m39jw8" />
                      <bpmn:serviceTask id="Activity_1yqfnos">
                        <bpmn:documentation>生成雪花ID</bpmn:documentation>
                        <bpmn:extensionElements>
                          <zeebe:taskDefinition type="snowflake" />
                          <zeebe:ioMapping>
                            <zeebe:input source="=111" target="key1" />
                          </zeebe:ioMapping>
                          <zeebe:properties />
                        </bpmn:extensionElements>
                        <bpmn:incoming>Flow_0dbdhy1</bpmn:incoming>
                        <bpmn:outgoing>Flow_159uaa6</bpmn:outgoing>
                      </bpmn:serviceTask>
                    </bpmn:process>
                    <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                      <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
                        <bpmndi:BPMNShape id="Event_1hv93zx_di" bpmnElement="Event_1hv93zx">
                          <dc:Bounds x="352" y="332" width="36" height="36" />
                        </bpmndi:BPMNShape>
                        <bpmndi:BPMNShape id="Event_0m39jw8_di" bpmnElement="Event_0m39jw8">
                          <dc:Bounds x="862" y="332" width="36" height="36" />
                        </bpmndi:BPMNShape>
                        <bpmndi:BPMNShape id="Activity_0qc0s6c_di" bpmnElement="Activity_1yqfnos">
                          <dc:Bounds x="570" y="310" width="100" height="80" />
                        </bpmndi:BPMNShape>
                        <bpmndi:BPMNEdge id="Flow_0dbdhy1_di" bpmnElement="Flow_0dbdhy1">
                          <di:waypoint x="388" y="350" />
                          <di:waypoint x="570" y="350" />
                        </bpmndi:BPMNEdge>
                        <bpmndi:BPMNEdge id="Flow_159uaa6_di" bpmnElement="Flow_159uaa6">
                          <di:waypoint x="670" y="350" />
                          <di:waypoint x="862" y="350" />
                        </bpmndi:BPMNEdge>
                      </bpmndi:BPMNPlane>
                    </bpmndi:BPMNDiagram>
                </bpmn:definitions>
                """;

        var deploy = client
                .newDeployResourceCommand()
                .addResourceBytes(bpmnXml.getBytes(), "test.bpmn")
                .send()
                .join();
        String bpmnProcessId = deploy.getProcesses().getFirst().getBpmnProcessId();
        Assertions.assertThat(bpmnProcessId).as("BPMN XML's process ID should equals to Process_1").isEqualTo("Process_1");

        final ProcessInstanceEvent processInstance =
                client
                        .newCreateInstanceCommand()
                        .bpmnProcessId("Process_1")
                        .latestVersion()
                        .send()
                        .join();

        CamundaAssert.assertThat(processInstance).isCreated();
    }
}
