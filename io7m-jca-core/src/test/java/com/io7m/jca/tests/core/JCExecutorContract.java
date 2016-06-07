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

package com.io7m.jca.tests.core;

import com.io7m.jca.core.JCExecutor;
import com.io7m.jca.core.JCExecutorType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class JCExecutorContract
{
  private JCExecutorType executor;

  protected abstract JCExecutorType create(int threads);

  protected abstract JCExecutorType createWithFactory(int threads, ThreadFactory tf);

  @Rule public final ExpectedException expected = ExpectedException.none();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(20L);

  @Before
  public final void setUp()
  {
    this.executor = JCExecutor.create("agents", 8);
  }

  @After
  public final void tearDown()
    throws IOException
  {
    this.executor.shutdown();
  }

  @Test
  public final void testShutdownRejected()
  {
    final JCExecutorType e = this.create(4);

    e.shutdown();
    this.expected.expect(RejectedExecutionException.class);
    e.submit(0, Object::new);
  }

  @Test
  public final void testShutdownAwaitHangingTerminated()
    throws Exception
  {
    final JCExecutorType e = this.create(4);

    e.submit(0, () -> {
      try {
        Thread.sleep(TimeUnit.MILLISECONDS.convert(
          1L,
          TimeUnit.SECONDS));
        return Long.valueOf(0L);
      } catch (final InterruptedException x) {
        x.printStackTrace();
        return Long.valueOf(0L);
      }
    });

    e.shutdown();
    Assert.assertTrue(e.isShutdown());
    Assert.assertFalse(e.isTerminated());
    e.awaitTermination(3L, TimeUnit.SECONDS);
    Assert.assertTrue(e.isShutdown());
    Assert.assertTrue(e.isTerminated());
  }

  @Test
  public final void testShutdownNowAwaitHangingTerminated()
    throws Exception
  {
    final JCExecutorType e = this.create(4);

    e.submit(0, () -> {
      try {
        Thread.sleep(TimeUnit.MILLISECONDS.convert(
          1L,
          TimeUnit.SECONDS));
        return Long.valueOf(0L);
      } catch (final InterruptedException x) {
        x.printStackTrace();
        return Long.valueOf(0L);
      }
    });

    final List<Runnable> xs = e.shutdownNow();
    Assert.assertEquals(0L, (long) xs.size());
    Assert.assertTrue(e.isShutdown());
    Assert.assertFalse(e.isTerminated());
    e.awaitTermination(3L, TimeUnit.SECONDS);
    Assert.assertTrue(e.isShutdown());
    Assert.assertTrue(e.isTerminated());
  }

  @Test
  public final void testShutdownAwaitTerminated()
    throws Exception
  {
    final JCExecutorType e = this.create(4);

    e.shutdown();
    e.awaitTermination(1L, TimeUnit.SECONDS);
    Assert.assertTrue(e.isShutdown());
    Assert.assertTrue(e.isTerminated());
  }

  @Test
  public final void testShutdownNowAwaitTerminated()
    throws Exception
  {
    final JCExecutorType e = this.create(4);

    e.shutdownNow();
    e.awaitTermination(1L, TimeUnit.SECONDS);
    Assert.assertTrue(e.isShutdown());
    Assert.assertTrue(e.isTerminated());
  }

  @Test
  public final void testShutdownTwiceOK()
  {
    final JCExecutorType e = this.create(4);

    e.shutdown();
    e.shutdown();
  }

  @Test
  public final void testShutdownIsShutdown()
  {
    final JCExecutorType e = this.create(4);

    e.shutdown();
    Assert.assertTrue(e.isShutdown());
  }

  @Test
  public final void testShutdownNowIsShutdown()
  {
    final JCExecutorType e = this.create(4);

    e.shutdown();
    Assert.assertTrue(e.isShutdown());
  }

  @Test
  public final void testShutdownNowTwiceOK()
  {
    final JCExecutorType e = this.create(4);

    final List<Runnable> xs0 = e.shutdownNow();
    Assert.assertEquals(0L, (long) xs0.size());
    final List<Runnable> xs1 = e.shutdownNow();
    Assert.assertEquals(0L, (long) xs1.size());

    Assert.assertNotSame(xs0, xs1);
  }

  @Test
  public final void testShutdownNowRejected()
  {
    final JCExecutorType e = this.create(4);

    final List<Runnable> xs = e.shutdownNow();
    Assert.assertEquals(0L, (long) xs.size());
    this.expected.expect(RejectedExecutionException.class);
    e.submit(0, Object::new);
  }

  @Test
  public final void testSequential()
  {
    final List<Integer> xs = new ArrayList<>(3);

    final CompletableFuture<Integer> f0 =
      this.executor.submit(0, () -> {
        synchronized (xs) {
          final Integer e = Integer.valueOf(0);
          xs.add(e);
          return e;
        }
      });

    final CompletableFuture<Integer> f1 =
      this.executor.submit(0, () -> {
        synchronized (xs) {
          final Integer e = Integer.valueOf(1);
          xs.add(e);
          return e;
        }
      });

    final CompletableFuture<Integer> f2 =
      this.executor.submit(0, () -> {
        synchronized (xs) {
          final Integer e = Integer.valueOf(2);
          xs.add(e);
          return e;
        }
      });

    final CompletableFuture<Void> f = CompletableFuture.allOf(f0, f2, f1);
    f.join();

    Assert.assertEquals(3L, (long) xs.size());
    Assert.assertEquals(Integer.valueOf(0), xs.get(0));
    Assert.assertEquals(Integer.valueOf(1), xs.get(1));
    Assert.assertEquals(Integer.valueOf(2), xs.get(2));
  }
}
