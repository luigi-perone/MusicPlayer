package it.unisa.gruppo7.musicplayer.command;

public interface Command<T>{
    T execute() throws Exception;
}
