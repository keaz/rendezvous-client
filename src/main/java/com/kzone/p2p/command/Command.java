package com.kzone.p2p.command;

import java.io.Serializable;

public sealed interface Command extends Serializable permits CreateFolderCommand, ModifyFolderCommand,ReadyToUploadCommand,ReadyToReceiveCommand,AddFileChunkCommand {
}
