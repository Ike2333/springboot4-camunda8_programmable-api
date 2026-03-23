package com.ike.sb4camunda8.config;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * @author <a href=mailto://idiotpre@outlook.com>IKE</a> 23/3/2026
 */
@RestControllerAdvice
public class GlobalExHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExHandler.class);


//    @ExceptionHandler(IllegalArgumentException.class)
//    public ProblemDetail handlerBizEx(IllegalArgumentException ex) {
//        String errMsg = ex.getMessage();
//        log.warn("参数不合法: {}", errMsg);
//        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errMsg);
//        pd.setTitle("参数不合法");
//        var props = Map.<String, Object>of(
//                "timestamp", Instant.now(),
//                "code", 400
//        );
//        pd.setProperties(props);
//        return pd;
//    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String collectedErrMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "参数不合法");
        pd.setProperty("errors", collectedErrMsg);
        pd.setInstance(URI.create(request.getContextPath()));

        return ResponseEntity.status(status).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handlerOtherEx(Exception ex) {
        log.error("系统未识别的异常", ex);

        return switch (ex) {
            case IllegalArgumentException e ->
                    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "参数不合法: " + e.getMessage());
            case IllegalStateException e -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "状态不合法");
            default -> {
                var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
                pd.setTitle("Server Error");
                yield pd;
            }
        };
    }
}
