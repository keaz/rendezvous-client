package com.kzone.util;

import com.kzone.App;
import com.kzone.p2p.event.Message;
import com.kzone.message.MessageHolder;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Log4j2
public record CommandLineReader(MessageHolder messageHolder) implements Runnable {

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.println("enter some commands:");
            String str;
            do {
                str = reader.readLine();
                messageHolder.putMessage(new Message(ClientUtil.getMac() + ":" + App.PEER_SERVER_PORT, str));
            }
            while (str != null);

        } catch (Exception ex) {
            log.error("Error reading message", ex);
        }

    }

}
