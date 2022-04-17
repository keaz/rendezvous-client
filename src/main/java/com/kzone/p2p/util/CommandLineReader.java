package com.kzone.p2p.util;

import com.kzone.p2p.event.Message;
import com.kzone.p2p.message.MessageHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@RequiredArgsConstructor
@Log4j2
public class CommandLineReader implements Runnable {

    private final MessageHolder messageHolder;

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.println("enter some commands:");
            String str;
            do {
                str = reader.readLine();
                messageHolder.putMessage(new Message(ClientUtil.getClientId(),str));
            }
            while (str != null);

        } catch (Exception ex) {
            log.error("Error reading message", ex);
        }

    }

}
