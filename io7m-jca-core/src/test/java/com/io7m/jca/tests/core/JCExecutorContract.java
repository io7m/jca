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
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

public abstract class JCExecutorContract
{
  private JCExecutorType executor;

  protected abstract JCExecutorType create(int threads);

  protected abstract JCExecutorType createWithFactory(int threads, ThreadFactory tf);

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
  public final void testFactory()
  {
    final JCExecutorType e = this.createWithFactory(
      4, r -> new Thread(null, r, "t", 64L));

    e.shutdown();
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
