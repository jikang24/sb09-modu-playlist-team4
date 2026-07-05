package com.mopl.domain.playlist.application.service;

import com.mopl.domain.content.dto.ContentSummary;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.application.dto.PlaylistSearchRequest;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.domain.playlist.application.dto.SortBy;
import com.mopl.domain.playlist.application.port.in.PlaylistUseCase;
import com.mopl.domain.playlist.application.port.out.LoadContentPort;
import com.mopl.domain.playlist.application.port.out.LoadPlaylistPort;
import com.mopl.domain.playlist.application.port.out.LoadUserPort;
import com.mopl.domain.playlist.application.port.out.SavePlaylistPort;
import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.PlaylistCreatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlaylistService implements PlaylistUseCase {

  private final SavePlaylistPort savePlaylistPort;
  private final LoadPlaylistPort loadPlaylistPort;
  private final LoadUserPort loadUserPort;
  private final LoadContentPort loadContentPort;
  private final NotificationEventPublisher notificationEventPublisher;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  @Transactional
  public PlaylistDto create(UUID ownerId, PlaylistCreateRequest request) {
    Playlist playlist = Playlist.create(ownerId, request.title(), request.description());
    Playlist saved = savePlaylistPort.save(playlist);

    applicationEventPublisher.publishEvent(
        new PlaylistCreatedEvent(saved.getId(), saved.getOwnerId(), saved.getTitle())
    );
    log.info("[Playlist] 생성 완료 - id: {}, ownerId: {}", saved.getId(), ownerId);
    return toDto(saved, ownerId);
  }

  @Override
  public PlaylistDto getById(UUID playlistId, UUID currentUserId) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    return toDto(playlist, currentUserId);
  }

  @Override
  public CursorPageResponse<PlaylistDto> getList(UUID currentUserId, PlaylistSearchRequest request) {
    validateSearchRequest(request);
    PlaylistSearchCondition condition = request.toCondition();
    List<Playlist> playlists = loadPlaylistPort.findAllByCondition(condition);
    long totalCount = loadPlaylistPort.countByCondition(condition);

    boolean hasNext = playlists.size() > condition.limit();
    List<Playlist> pageData = hasNext
        ? playlists.subList(0, condition.limit())
        : playlists;

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !pageData.isEmpty()) {
      Playlist last = pageData.get(pageData.size() - 1);
      nextCursor = condition.sortBy() == SortBy.subscribeCount
          ? String.valueOf(loadPlaylistPort.countSubscribers(last.getId()))
          : last.getUpdatedAt().toString();
      nextIdAfter = last.getId();
    }

    List<PlaylistDto> data = pageData.stream()
        .map(playlist -> toDto(playlist, currentUserId))
        .toList();

    return new CursorPageResponse<>(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        condition.sortBy().name(),
        condition.sortDirection().name()
    );
  }

  @Override
  @Transactional
  public PlaylistDto update(UUID playlistId, UUID currentUserId, PlaylistUpdateRequest request) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, currentUserId);
    playlist.update(request.title(), request.description());
    Playlist saved = savePlaylistPort.save(playlist);
    log.info("[Playlist] 수정 완료 - id: {}", playlistId);
    return toDto(saved, currentUserId);
  }

  @Override
  @Transactional
  public void delete(UUID playlistId, UUID currentUserId) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, currentUserId);
    savePlaylistPort.delete(playlistId);
    log.info("[Playlist] 삭제 완료 - id: {}", playlistId);
  }

  @Override
  @Transactional
  public void subscribe(UUID playlistId, UUID subscriberId) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    if (loadPlaylistPort.isSubscribed(playlistId, subscriberId)) {
      throw new MoplException(ErrorCode.PLAYLIST_ALREADY_SUBSCRIBED);
    }
    savePlaylistPort.subscribe(playlistId, subscriberId);

    if (!playlist.isOwner(subscriberId)) {
      notificationEventPublisher.publish(new NotificationRequestedEvent(
          playlist.getOwnerId(),
          NotificationType.PLAYLIST_SUBSCRIBED.name(),
          "플레이리스트 구독",
          loadUserPort.getUserSummary(subscriberId).name() + "님이 '" + playlist.getTitle() + "' 플레이리스트를 구독했습니다."
      ));
    }
    log.info("[Playlist] 구독 - playlistId: {}, subscriberId: {}", playlistId, subscriberId);
  }

  @Override
  @Transactional
  public void unsubscribe(UUID playlistId, UUID subscriberId) {
    findPlaylistOrThrow(playlistId);
    if (!loadPlaylistPort.isSubscribed(playlistId, subscriberId)) {
      throw new MoplException(ErrorCode.PLAYLIST_NOT_SUBSCRIBED);
    }
    savePlaylistPort.unsubscribe(playlistId, subscriberId);
    log.info("[Playlist] 구독 취소 - playlistId: {}, subscriberId: {}", playlistId, subscriberId);
  }

  @Override
  @Transactional
  public void addContent(UUID playlistId, UUID contentId, UUID currentUserId) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, currentUserId);

    if (!loadContentPort.existsById(contentId)) {
      throw new MoplException(ErrorCode.CONTENT_NOT_FOUND);
    }

    playlist.addContent(contentId);
    savePlaylistPort.save(playlist);

    notifySubscribersContentUpdated(playlist);
    log.info("[Playlist] 콘텐츠 추가 - playlistId: {}, contentId: {}", playlistId, contentId);
  }

  @Override
  @Transactional
  public void removeContent(UUID playlistId, UUID contentId, UUID currentUserId) {
    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, currentUserId);
    playlist.removeContent(contentId);
    savePlaylistPort.save(playlist);
    log.info("[Playlist] 콘텐츠 삭제 - playlistId: {}, contentId: {}", playlistId, contentId);
  }

  private void notifySubscribersContentUpdated(Playlist playlist) {
    List<UUID> subscriberIds = loadPlaylistPort.findSubscriberIds(playlist.getId());
    for (UUID subscriberId : subscriberIds) {
      notificationEventPublisher.publish(new NotificationRequestedEvent(
          subscriberId,
          NotificationType.PLAYLIST_UPDATED.name(),
          "플레이리스트 업데이트",
          "'" + playlist.getTitle() + "' 플레이리스트에 새 콘텐츠가 추가되었습니다."
      ));
    }
  }

  private PlaylistDto toDto(Playlist playlist, UUID currentUserId) {
    UserSummary owner = loadUserPort.getUserSummary(playlist.getOwnerId());
    List<ContentSummary> contents = loadContentPort.findSummariesByIds(playlist.getContentIds());
    long subscriberCount = loadPlaylistPort.countSubscribers(playlist.getId());
    boolean subscribedByMe = loadPlaylistPort.isSubscribed(playlist.getId(), currentUserId);

    return new PlaylistDto(
        playlist.getId(),
        owner,
        playlist.getTitle(),
        playlist.getDescription(),
        playlist.getUpdatedAt(),
        subscriberCount,
        subscribedByMe,
        contents
    );
  }

  private Playlist findPlaylistOrThrow(UUID playlistId) {
    return loadPlaylistPort.findById(playlistId)
        .orElseThrow(() -> new MoplException(ErrorCode.PLAYLIST_NOT_FOUND));
  }

  private void assertOwner(Playlist playlist, UUID currentUserId) {
    if (!playlist.isOwner(currentUserId)) {
      throw new MoplException(ErrorCode.PLAYLIST_NOT_OWNER);
    }
  }

  private void validateSearchRequest(PlaylistSearchRequest request) {
    if (request.limit() <= 0) {
      throw new MoplException(ErrorCode.INVALID_INPUT);
    }

    boolean hasCursor = request.cursor() != null && !request.cursor().isBlank();
    boolean hasIdAfter = request.idAfter() != null;
    if (hasCursor != hasIdAfter) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    if (hasCursor) {
      if (request.sortBy() == SortBy.subscribeCount) {
        try {
          Long.parseLong(request.cursor());
        } catch (NumberFormatException e) {
          throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
      } else {
        try {
          Instant.parse(request.cursor());
        } catch (DateTimeParseException e) {
          throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
      }
    }
  }
}
