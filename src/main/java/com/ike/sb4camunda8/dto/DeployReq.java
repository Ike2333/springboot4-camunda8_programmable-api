package com.ike.sb4camunda8.dto;

public record DeployReq(String sourceName, SuppHttpMethod method, String path, String bpmnXml, Boolean enable){}
