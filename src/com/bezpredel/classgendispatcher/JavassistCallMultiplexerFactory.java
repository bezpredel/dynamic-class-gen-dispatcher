package com.bezpredel.classgendispatcher;

import com.google.common.base.Preconditions;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public enum JavassistCallMultiplexerFactory
        implements CallMultiplexerFactory
{
    INSTANCE;

    private final Map<Signature, CacheRecord> _cache = new HashMap<>();

    @Override
    public <T> T createCallMultiplexer(Class<T> clazz, DispatchExceptionHandler h, T ... delegates) throws Exception{
        preconditions(clazz, delegates);

        // optimization
        if (delegates.length == 1) {
            return delegates[0];
        }

        Signature signature = createSignature(clazz, delegates, h != null);

        CacheRecord<T> cacheRecord;
        synchronized (_cache) {
            cacheRecord = _cache.get(signature);

            if (cacheRecord == null) {
                cacheRecord = new CacheRecord<>();
                _cache.put(signature, cacheRecord);
            }
        }

        return cacheRecord.createInstance(clazz, delegates, h);
    }

    private <T> Signature createSignature(Class<T> clazz, T[] delegates, boolean withExceptionHandler) {
        Class[] delegateTypes = new Class[delegates.length];
        for (int i=0; i<delegates.length; i++) {
            delegateTypes[i] = delegates[i].getClass();
        }

        return new Signature(clazz, delegateTypes, withExceptionHandler);
    }

    private <T> void preconditions(Class<T> clazz, T[] delegates) {
        Preconditions.checkNotNull(clazz);
        Preconditions.checkNotNull(delegates);
        for (T listener : delegates) {
            Preconditions.checkNotNull(listener);
        }
    }

    private static class CacheRecord<T> {
        boolean _initialized;
        Class<T> _dispatcherType;
        Constructor<T> _dispatcherConstructor;
        Throwable _initializationException;

        boolean isInitialized() {
            return _initialized;
        }

        void initialize(Class<T> clazz, T[] delegates,  boolean needExceptionHandler) {
            try {
                long t0 = System.currentTimeMillis();
                Class<T> tClass = InterfaceMultiplexerClassGenerator.INSTANCE.create(clazz, needExceptionHandler, delegates);
                Constructor<T> constructor = tClass.getConstructor(delegates.getClass(), DispatchExceptionHandler.class);
                long t1 = System.currentTimeMillis();

                System.out.println("Class creation took: " + (t1-t0) + "ms");

                _dispatcherType = tClass;
                _dispatcherConstructor = constructor;
            } catch (Throwable e) {
                _initializationException = e;
            } finally {
                _initialized = true;
            }
        }



        T createInstance(Class<T> clazz, T[] delegates, DispatchExceptionHandler h) throws Exception {
            initializeIfNecessary(clazz, delegates, h != null);

            if (_initializationException != null) {
                if (_initializationException instanceof Error) {
                    throw (Error)_initializationException;
                } else {
                    throw new Exception("Failed to create a dispatcher class", _initializationException);
                }
            } else {
                return _dispatcherConstructor.newInstance(new Object[]{delegates, h});
            }
        }

        private void initializeIfNecessary(Class<T> clazz, T[] delegates, boolean needExceptionHandler) {
            synchronized (this) {
                if (!isInitialized()) {
                    initialize(clazz, delegates, needExceptionHandler);
                }
            }
        }

    }
}
