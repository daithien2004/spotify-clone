package com.spotify.common.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The wrapper must not interfere with streaming payloads (audio/bytes) —
 * wrapping an InputStreamResource/byte[] body breaks HTTP Range playback.
 */
class GlobalResponseWrapperTest {

    private final GlobalResponseWrapper wrapper = new GlobalResponseWrapper();

    @Test
    void should_LeaveStreamingResourceUnwrapped_when_BodyIsResource() {
        ServerHttpRequest request = requestWithPath("/api/v1/tracks/abc/audio");
        var audio = new InputStreamResource(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        Object result = wrapper.beforeBodyWrite(audio, null, null, null, request, mock(ServerHttpResponse.class));

        assertThat(result).isSameAs(audio);
    }

    @Test
    void should_LeaveRawBytesUnwrapped_when_BodyIsByteArray() {
        ServerHttpRequest request = requestWithPath("/api/v1/files/abc");
        byte[] raw = new byte[]{9, 9, 9};

        Object result = wrapper.beforeBodyWrite(raw, null, null, null, request, mock(ServerHttpResponse.class));

        assertThat(result).isSameAs(raw);
    }

    @Test
    void should_WrapPlainPayload_when_BodyIsJsonDto() {
        ServerHttpRequest request = requestWithPath("/api/v1/playlists");
        String payload = "hello";

        Object result = wrapper.beforeBodyWrite(payload, null, null, null, request, mock(ServerHttpResponse.class));

        assertThat(result).isInstanceOf(ApiResponse.class);
        assertThat(((ApiResponse<?>) result).data()).isEqualTo(payload);
    }

    @Test
    void should_LeaveApiResponseUnwrapped_when_AlreadyEnvelope() {
        ServerHttpRequest request = requestWithPath("/api/v1/playlists");
        ApiResponse<String> envelope = ApiResponse.success("ok");

        Object result = wrapper.beforeBodyWrite(envelope, null, null, null, request, mock(ServerHttpResponse.class));

        assertThat(result).isSameAs(envelope);
    }

    @Test
    void should_SkipWrapping_when_PathIsApiDocs() {
        ServerHttpRequest request = requestWithPath("/v3/api-docs");
        String payload = "{}";

        Object result = wrapper.beforeBodyWrite(payload, null, null, null, request, mock(ServerHttpResponse.class));

        assertThat(result).isSameAs(payload);
    }

    private ServerHttpRequest requestWithPath(String path) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(path));
        return request;
    }
}