package com.kzone.client.event;

public record ClientUpdated(String id, String status) implements ClientEvent {

}
