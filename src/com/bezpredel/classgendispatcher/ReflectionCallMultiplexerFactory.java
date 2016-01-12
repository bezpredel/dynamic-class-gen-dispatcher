package com.bezpredel.classgendispatcher;

import com.google.common.base.Preconditions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public enum ReflectionCallMultiplexerFactory
        implements CallMultiplexerFactory
{
    INSTANCE;

    @Override
    public <T> T createCallMultiplexer(Class<T> clazz, DispatchExceptionHandler h, T ... delegates) throws Exception{
        preconditions(clazz, delegates);

        // optimization
        if (delegates.length == 1) {
            return delegates[0];
        }

        // todo: do something different when zero delegates

        return new CallMultiplexer<T>(clazz, h, delegates)._proxy;
    }

    public static class CallMultiplexer<T> {
        final T _proxy;

        private DispatchExceptionHandler _exceptionHandler;
        final T[] _subscribers;

        public CallMultiplexer(Class<T> clazz, DispatchExceptionHandler h, T...subscribers) {
            _exceptionHandler = h;
            _subscribers = subscribers;

            InvocationHandler invocationHandler;
            if (h == null) {
                invocationHandler = new MyInvocationHandler();
            } else {
                invocationHandler = new MyInvocationHandlerWithCatcher();
            }
            _proxy = (T) Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class[]{ clazz },
                    invocationHandler
            );
        }


        private class MyInvocationHandler implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object retVal = null;
                for (T subscriber : _subscribers) {
                    retVal = method.invoke(subscriber, args);
                }
                return retVal;
            }
        }

        private class MyInvocationHandlerWithCatcher implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object retVal = null;
                for (T subscriber : _subscribers) {
                    try {
                        retVal = method.invoke(subscriber, args);
                    } catch (Exception e) {
                        _exceptionHandler.errorWhileDispatching(e, subscriber, method.getName());
                    }
                }
                return retVal;
            }
        }
    }



    private <T> void preconditions(Class<T> clazz, T[] delegates) {
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(delegates);
        for (T listener : delegates) {
            Preconditions.checkNotNull(listener);
        }
    }
}
