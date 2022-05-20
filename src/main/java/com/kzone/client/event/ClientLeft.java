package com.kzone.client.event;

import java.util.UUID;

public record ClientLeft(String id) implements ClientEvent {
}
