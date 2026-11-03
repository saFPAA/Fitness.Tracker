package com.Author.Fitness.Exception;

public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException()
    {
        super("Current password is incorrect");
    }
}
