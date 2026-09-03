package com.spotify.playlist.presentation.controller;

import com.spotify.playlist.application.dto.PlaylistResponse;
import com.spotify.playlist.application.dto.PlaylistSummaryResponse;
import com.spotify.playlist.application.usecase.CreatePlaylistUseCase;
import com.spotify.playlist.application.usecase.GetPlaylistByIdUseCase;
import com.spotify.playlist.application.usecase.ListPlaylistsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvcTest cho POST /playlists — xác minh contract HTTP: envelope
 * {@code {success,data,...}} + @Valid validation + status 201 + principal (X-User-Id)
 * được dùng làm ownerUserId. Chỉ load tầng web; use cases mock bằng @MockBean.
 */
@WebMvcTest(PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private GetPlaylistByIdUseCase getPlaylistByIdUseCase;
    @MockBean private ListPlaylistsUseCase listPlaylistsUseCase;
    @MockBean private CreatePlaylistUseCase createPlaylistUseCase;

    @Test
    void should_Return201Wrapped_when_CreatePlaylistValid() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();
        when(createPlaylistUseCase.execute(any(CreatePlaylistUseCase.CreatePlaylistCommand.class)))
                .thenReturn(new PlaylistResponse(
                        playlistId, "Chill Hits", "relax", "Alice", null,
                        OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/playlists")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Chill Hits\",\"description\":\"relax\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(playlistId.toString()))
                .andExpect(jsonPath("$.data.ownerName").value("Alice"));
    }

    @Test
    void should_Return400_when_TitleBlank() throws Exception {
        mockMvc.perform(post("/api/v1/playlists")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_PassOwnerUserIdFromPrincipal_when_Creating() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(createPlaylistUseCase.execute(any(CreatePlaylistUseCase.CreatePlaylistCommand.class)))
                .thenAnswer(inv -> new PlaylistResponse(
                        UUID.randomUUID(), "T", null, "You", null,
                        OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/playlists")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        var captor = org.mockito.ArgumentCaptor.forClass(CreatePlaylistUseCase.CreatePlaylistCommand.class);
        org.mockito.Mockito.verify(createPlaylistUseCase).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().ownerUserId()).isEqualTo(ownerId);
    }
}
