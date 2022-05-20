package com.kzone.client.event;

import java.util.UUID;

public record ClientJoined(String id, int port) implements ClientEvent {
}
