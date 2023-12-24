package org.abos.linker.scraper;

import java.util.concurrent.BlockingQueue;

public final class ScraperTestUtil {

    private ScraperTestUtil() {
        /* No instantiation. */
    }

    public static void doTestBlockingQueue(final int timeOut, final BlockingQueue<?> queue) {
        try {
            Thread.sleep(10L*timeOut);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(queue.peek());
        try {
            Thread.sleep(50L*timeOut);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(queue.size());

    }

}
