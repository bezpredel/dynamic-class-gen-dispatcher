package com.bezpredel.classgendispatcher;

public interface DispatchExceptionHandler<T> {
    void errorWhileDispatching(Exception e, T source, String methodName);
}
