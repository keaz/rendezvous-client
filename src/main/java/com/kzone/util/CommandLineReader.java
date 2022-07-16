package com.kzone.util;

import com.kzone.App;
import com.kzone.p2p.command.Folder;
import com.kzone.p2p.command.ModifyFolderCommand;
import com.kzone.p2p.event.Message;
import com.kzone.message.MessageHolder;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

@Log4j2
public record CommandLineReader(MessageHolder messageHolder) implements Runnable {

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        final var folders = new ArrayList<Folder>();
        final var uuid = UUID.randomUUID();
        folders.add(new Folder(uuid.toString(), "asd",Collections.emptyList()));
//        final var modifyFolderCommand = new ModifyFolderCommand(ClientUtil.getClientName(), UUID.randomUUID(), folders);
//        messageHolder.putMessage(modifyFolderCommand);
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
