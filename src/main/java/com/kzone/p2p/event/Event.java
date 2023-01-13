package com.kzone.p2p.event;

import java.io.Serializable;

public sealed interface Event extends Serializable permits DownloadCompletedEvent,DownloadFailedEvent,FileDownloadedEvent,
        FileUploadCompletedEvent,FolderModifiedEvent,UploadRejectedEvent {


}
