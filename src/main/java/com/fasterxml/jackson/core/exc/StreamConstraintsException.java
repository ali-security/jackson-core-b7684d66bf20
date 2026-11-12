package com.fasterxml.jackson.core.exc;

import com.fasterxml.jackson.core.JsonProcessingException;

public class StreamConstraintsException extends JsonProcessingException
{
    private static final long serialVersionUID = 1L;

    public StreamConstraintsException(String msg) {
        super(msg);
    }
}
