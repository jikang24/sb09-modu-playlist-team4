package com.mopl.global.websocket;

import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
  private final JwtProvider jwtProvider;
  private final GetConversationUseCase getConversationUseCase;

  @Override
  public Message<?> preSend(Message<?> message , MessageChannel channel){
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    if(StompCommand.CONNECT.equals(accessor.getCommand())){
      String token = extractToken(accessor);
      JwtClaims claims = jwtProvider.parse(token);

      var authentication = new UsernamePasswordAuthenticationToken(
          claims,
          null,
          List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole()))
      );
      accessor.setUser(authentication);
    }
    if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
    validateSubscription(accessor);
    }
    return message;
  }

  private String extractToken(StompHeaderAccessor accessor){
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if(authHeader == null || !authHeader.startsWith("Bearer ")){
      throw new IllegalArgumentException("토큰이 없습니다.");
    }
    return authHeader.substring(7);
  }

  private void validateSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination != null && destination.matches("/sub/conversations/[^/]+/direct-messages")) {
      UUID conversationId = extractConversationId(destination);
      UUID myId = getUserIdFromAccessor(accessor);
      getConversationUseCase.getById(conversationId, myId);
    }
  }
  private UUID extractConversationId(String destination){
    String[] parts = destination.split("/");
    return UUID.fromString(parts[3]);
  }

  private UUID getUserIdFromAccessor(StompHeaderAccessor accessor) {
    var authentication = (UsernamePasswordAuthenticationToken) accessor.getUser();
    JwtClaims claims = (JwtClaims) authentication.getPrincipal();
    return claims.getUserId();
  }



}
