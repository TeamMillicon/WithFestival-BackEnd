package com.festival.global.security.provider;

import com.festival.domain.admin.data.dto.JwtTokenRes;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenProvider {
    private final Key key;

    public TokenProvider(@Value("${jwt.secret}") String secretKey){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

    }
    public JwtTokenRes generateToken(Authentication authentication, String role){
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        //System.out.println(authorities);

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + 10 * 1000 * 60 *30); //30분
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) // 토큰 제목
                .claim("auth", role) //
                .setExpiration(accessTokenExpiresIn) // 만료시간
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + 10 * 1000 * 60 *30))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtTokenRes.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    public Authentication getAuthentication(String accessToken){
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null){
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        Collection<? extends  GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try
        {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        }catch (ExpiredJwtException e){
            return e.getClaims();
        }
    }
}