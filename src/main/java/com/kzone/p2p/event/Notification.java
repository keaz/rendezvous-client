package com.kzone.p2p.event;

import java.io.Serializable;
import java.util.UUID;

public interface Notification extends Serializable {

    UUID clientId();

}
