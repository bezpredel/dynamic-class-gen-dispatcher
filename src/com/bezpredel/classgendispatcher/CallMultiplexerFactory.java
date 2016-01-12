package com.bezpredel.classgendispatcher;

/**
 * Created by alex on 10/26/2015.
 */
public interface CallMultiplexerFactory {
    <T> T createCallMultiplexer(Class<T> clazz, DispatchExceptionHandler h, T[] delegates) throws Exception;
}
