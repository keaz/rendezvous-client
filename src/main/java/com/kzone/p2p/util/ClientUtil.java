package com.kzone.p2p.util;

import lombok.experimental.UtilityClass;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.UUID;

@UtilityClass
public class ClientUtil {

    private static final UUID clientId = UUID.randomUUID();

    public String getMac() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                byte[] adr = ni.getHardwareAddress();
                if (adr == null || adr.length != 6)
                    continue;
                return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                        adr[0], adr[1], adr[2], adr[3], adr[4], adr[5]);

            }

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Mac not found");
    }

    public long getPId(){
        return ProcessHandle.current().pid();
    }

    public String getHostName()  {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public String getClientName(){
        return getHostName() +"-"+getPId();
    }

    public static UUID getClientId() {
        return clientId;
    }
}
