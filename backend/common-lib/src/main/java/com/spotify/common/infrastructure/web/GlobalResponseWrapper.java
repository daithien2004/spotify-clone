package com.spotify.common.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.spotify")
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            return body;
        }

        if (body instanceof ApiResponse) {
            return body;
        }

        return ApiResponse.success(body);
    }
}
