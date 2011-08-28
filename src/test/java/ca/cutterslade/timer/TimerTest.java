package ca.cutterslade.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import ca.cutterslade.blocktimer.Timer;
import ca.cutterslade.blocktimer.TimerEvent;
import ca.cutterslade.blocktimer.TimerHandler;

import com.google.common.collect.ImmutableSet;

public class TimerTest {

  @Test
  public void test() {
    final AtomicBoolean eventRecieved = new AtomicBoolean(false);
    Timer.setTimerHandlers(ImmutableSet.of(new TimerHandler() {

      private final long startTime = System.nanoTime();

      @Override
      public void timerEvent(final TimerEvent event) {
        eventRecieved.set(true);
        assertTrue("!" + event.getStartNanos() + " - " + startTime + " >= 0", event.getStartNanos() - startTime >= 0);
        assertTrue("!" + event.getEndNanos() + " - " + event.getStartNanos() + " >= 0",
            event.getEndNanos() - event.getStartNanos() >= 0);
        assertTrue(event.getTimeNanos() >= 0);
        assertEquals(TimerTest.class, event.getHostClass());
        assertEquals("test", event.getMethod());
        assertNull(event.getOperation());
      }
    }));
    try (Timer timer = Timer.time(TimerTest.class, "test", null)) {
    }
    assertTrue(eventRecieved.get());
  }
}
