package com.synergyhub.service.meeting;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to generate LiveKit access tokens for video conferencing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveKitService {

    @Value("${livekit.api.key:devkey}")
    private String apiKey;

    @Value("${livekit.api.secret:secret}")
    private String apiSecret;

    /**
     * Generates a LiveKit access token for the given room and participant.
     */
    public String generateToken(String roomName, String participantIdentity, String participantName) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName(participantName);
        token.setIdentity(participantIdentity);
        token.setTtl(24 * 60 * 60); // 24 hours in seconds
        
        // Grant room access
        token.addGrants(new RoomJoin(true), new RoomName(roomName));
        
        return token.toJwt();
    }
}
