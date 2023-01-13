package com.kzone.client.event;

import java.io.Serializable;

public sealed interface ClientEvent extends Serializable permits ClientInfo, ClientJoined, ClientLeft, ClientUpdated {

    String id();

}
