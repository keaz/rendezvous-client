package com.kzone.p2p.event;

import java.util.UUID;

public record ClientJoined(String clientName, UUID clientId, String address) implements Notification {
}
