package com.mopl.global.websocket;

import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.jwt.JwtProvider;
import java.util.List;
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
    return message;
  }

  private String extractToken(StompHeaderAccessor accessor){
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if(authHeader == null || !authHeader.startsWith("Bearer ")){
      throw new IllegalArgumentException("토큰이 없습니다.");
    }
    return authHeader.substring(7);
  }



}
