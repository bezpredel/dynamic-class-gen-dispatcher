package com.bezpredel.classgendispatcher;

import java.util.Arrays;

final class Signature {
    private final Class _interface;
    private final Class[] _delegateClasses;
    private final boolean _withExceptionHandler;

    Signature(Class intrfc, Class[] delegateClasses, boolean withExceptionHandler) {
        _withExceptionHandler = withExceptionHandler;
        assert intrfc != null;
        assert delegateClasses != null;

        this._interface = intrfc;
        this._delegateClasses = delegateClasses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Signature signature = (Signature) o;

        if (_withExceptionHandler != signature._withExceptionHandler) return false;
        if (!_interface.equals(signature._interface)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(_delegateClasses, signature._delegateClasses);

    }

    @Override
    public int hashCode() {
        int result = _interface.hashCode();
        result = 31 * result + Arrays.hashCode(_delegateClasses);
        result = 31 * result + (_withExceptionHandler ? 1 : 0);
        return result;
    }
}
