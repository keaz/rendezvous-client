package com.kzone.client.event;


public record ClientInfo(String id, String address, int port) implements ClientEvent {

}
