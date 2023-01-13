package com.kzone.p2p.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
public record AddFileChunkCommand(UUID id, String fileName, byte[] data, int start) implements Command {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddFileChunkCommand that = (AddFileChunkCommand) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AddFileChunkCommand{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", start=" + start +
                '}';
    }
}
