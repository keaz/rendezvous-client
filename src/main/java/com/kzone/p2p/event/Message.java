package com.kzone.p2p.event;

import java.util.UUID;

public record Message(UUID clientId,String message) implements Notification {

}
