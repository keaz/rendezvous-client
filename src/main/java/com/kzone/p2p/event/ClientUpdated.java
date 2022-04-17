package com.kzone.p2p.event;

import java.util.UUID;

public record ClientUpdated(UUID clientId, String clientName, String status) implements Notification {

}
