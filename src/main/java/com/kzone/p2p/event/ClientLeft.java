package com.kzone.p2p.event;

import java.util.UUID;

public record ClientLeft(UUID clientId) implements Notification {
}
