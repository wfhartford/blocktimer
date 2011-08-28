package ca.cutterslade.blocktimer;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;

public class Timer implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(Timer.class);

  private static final Map<Class<?>, Logger> TIMER_LOGGERS =
      new MapMaker().weakKeys().makeComputingMap(new Function<Class<?>, Logger>() {

        @Override
        public Logger apply(final Class<?> input) {
          return LoggerFactory.getLogger("TIMER." + input.getName());
        }
      });

  private static final TimerHandler LOGGER_HANDLER = new TimerHandler() {

    @Override
    public void timerEvent(final TimerEvent event) {
      TIMER_LOGGERS.get(event.getHostClass()).info("{}", event);
    }
  };

  private static volatile ImmutableSet<TimerHandler> CUSTOM_HANDLERS = ImmutableSet.of();

  public static void addTimerHander(final TimerHandler handler) {
    CUSTOM_HANDLERS = ImmutableSet.<TimerHandler> builder().addAll(CUSTOM_HANDLERS).add(handler).build();
  }

  public static void setTimerHandlers(final Iterable<? extends TimerHandler> handlers) {
    CUSTOM_HANDLERS = ImmutableSet.copyOf(handlers);
  }

  private static final Iterable<TimerHandler> HANDLERS = Iterables.concat(ImmutableSet.of(LOGGER_HANDLER),
      new Iterable<TimerHandler>() {

        @Override
        public Iterator<TimerHandler> iterator() {
          return CUSTOM_HANDLERS.iterator();
        }
      });

  public static Timer time(final Class<?> host, final String method, final Object operation) {
    return new Timer(host, method, operation);
  }

  private final String method;

  private final Object operation;

  private final long startDate;

  private final long startNanos;

  private final Class<?> host;

  private final ImmutableList<StackTraceElement> stackTrace;

  private Timer(final Class<?> host, final String method, final Object operation) {
    Preconditions.checkArgument(null != host);
    Preconditions.checkArgument(null != method);
    this.host = host;
    this.method = method;
    this.operation = operation;
    this.stackTrace = ImmutableList.copyOf(Thread.currentThread().getStackTrace());
    this.startDate = System.currentTimeMillis();
    this.startNanos = System.nanoTime();
  }

  String getMethod() {
    return method;
  }

  Object getOperation() {
    return operation;
  }

  long getStartDate() {
    return startDate;
  }

  long getStartNanos() {
    return startNanos;
  }

  Class<?> getHost() {
    return host;
  }

  ImmutableList<StackTraceElement> getStackTrace() {
    return stackTrace;
  }

  @Override
  public void close() {
    final long endNanos = System.nanoTime();
    final TimerEvent event = new TimerEvent(this, endNanos);
    for (final TimerHandler handler : HANDLERS) {
      try {
        handler.timerEvent(event);
      }
      catch (final RuntimeException e) {
        log.warn("Handler {} threw exception", handler, e);
      }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + host.hashCode();
    result = prime * result + method.hashCode();
    result = prime * result + stackTrace.hashCode();
    result = prime * result + (int) (startDate ^ (startDate >>> 32));
    result = prime * result + (int) (startNanos ^ (startNanos >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Timer other = (Timer) obj;
    if (!host.equals(other.host)) {
      return false;
    }
    if (!method.equals(other.method)) {
      return false;
    }
    if (!stackTrace.equals(other.stackTrace)) {
      return false;
    }
    if (startDate != other.startDate) {
      return false;
    }
    if (startNanos != other.startNanos) {
      return false;
    }
    return true;
  }

}
