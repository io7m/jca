/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jca.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jranges.RangeCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * The default implementation of the {@link JCExecutorType} type.
 */

public final class JCExecutor implements JCExecutorType
{
  private final ExecutorService[] execs;

  private JCExecutor(
    final int thread_count,
    final ThreadFactory thread_factory)
  {
    RangeCheck.checkGreaterInteger(
      thread_count, "Thread count", 0, "Minimum number of threads");

    this.execs = new ExecutorService[thread_count];
    for (int index = 0; index < thread_count; ++index) {
      this.execs[index] = Executors.newSingleThreadExecutor(thread_factory);
    }
  }

  /**
   * Create an executor named {@code name}, using exactly {@code thread_count}
   * threads internally.
   *
   * @param name         The executor name
   * @param thread_count The number of threads
   *
   * @return A new executor
   */

  public static JCExecutorType create(
    final String name,
    final int thread_count)
  {
    NullCheck.notNull(name);

    final AtomicInteger fi = new AtomicInteger(0);
    final ThreadFactory factory = r -> {
      final Thread thread = new Thread(r);
      thread.setName(String.format(
        "%s-%d", name, Integer.valueOf(fi.incrementAndGet())));
      return thread;
    };

    return JCExecutor.createWithFactory(thread_count, factory);
  }

  /**
   * Create an executor using exactly {@code thread_count} threads internally.
   * Threads are created using the given {@code factory}.
   *
   * @param factory      A thread factory
   * @param thread_count The number of threads
   *
   * @return A new executor
   */

  public static JCExecutorType createWithFactory(
    final int thread_count,
    final ThreadFactory factory)
  {
    return new JCExecutor(thread_count, factory);
  }

  @Override
  public <T> CompletableFuture<T> submit(
    final int key,
    final Supplier<T> op)
  {
    NullCheck.notNull(op);
    return CompletableFuture.supplyAsync(op, this.execs[this.execIndex(key)]);
  }

  private int execIndex(final int key)
  {
    return (key & 0x7FFF_FFFF) % this.execs.length;
  }

  @Override
  public void shutdown()
  {
    for (int index = 0; index < this.execs.length; ++index) {
      this.execs[index].shutdown();
    }
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    final List<Runnable> rr = new ArrayList<>(64);
    for (int index = 0; index < this.execs.length; ++index) {
      rr.addAll(this.execs[index].shutdownNow());
    }
    return rr;
  }

  @Override
  public boolean isShutdown()
  {
    boolean shut = true;
    for (int index = 0; index < this.execs.length; ++index) {
      shut = shut && this.execs[index].isShutdown();
    }
    return shut;
  }

  @Override
  public boolean isTerminated()
  {
    boolean term = true;
    for (int index = 0; index < this.execs.length; ++index) {
      term = term && this.execs[index].isTerminated();
    }
    return term;
  }

  @Override
  public boolean awaitTermination(
    final long timeout,
    final TimeUnit unit)
    throws InterruptedException
  {
    boolean ok = true;
    for (int index = 0; index < this.execs.length; ++index) {
      ok = ok && this.execs[index].awaitTermination(timeout, unit);
    }
    return ok;
  }
}
