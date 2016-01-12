package com.bezpredel.classgendispatcher;

/**
 * Created by alex on 10/27/2015.
 */
public class MoreOrLessRealisticTest {



    public interface Listener {
        void event1(Object e);
        void event2(Object e);
        void event3(Object e);
        void event4(Object e);
        void event5(Object e);
        void event6(Object e);
        void event7(Object e);
        void event8(Object e);
        void event9(Object e);
        void event10(Object e);
        void event11(Object e);
        void event12(Object e);
        void event13(Object e);
        void event14(Object e);
    }

    public static abstract class AC implements Listener {
        @Override
        public void event1(Object e) {

        }

        @Override
        public void event2(Object e) {

        }

        @Override
        public void event3(Object e) {

        }

        @Override
        public void event4(Object e) {

        }

        @Override
        public void event5(Object e) {

        }

        @Override
        public void event6(Object e) {

        }

        @Override
        public void event7(Object e) {

        }

        @Override
        public void event8(Object e) {

        }

        @Override
        public void event9(Object e) {

        }

        @Override
        public void event10(Object e) {

        }

        @Override
        public void event11(Object e) {

        }

        @Override
        public void event12(Object e) {

        }

        @Override
        public void event13(Object e) {

        }

        @Override
        public void event14(Object e) {

        }
    }

    private static class L1 extends AC {
        @Override
        public void event1(Object e) {
            // todo: do something non-trivial
        }

        @Override
        public void event3(Object e) {
            // todo: do something non-trivial
        }
    }
}
