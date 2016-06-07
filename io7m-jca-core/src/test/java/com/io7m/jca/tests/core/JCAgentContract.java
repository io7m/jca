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

import com.io7m.jca.core.JCAgent;
import com.io7m.jca.core.JCAgentType;
import com.io7m.jca.core.JCExecutor;
import com.io7m.jca.core.JCExecutorType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class JCAgentContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(JCAgentContract.class);
  }

  private JCExecutorType executor;

  protected abstract <T> JCAgentType<T> create(JCExecutorType e, T value);

  final class Counter
  {
    private final JCAgentType<Integer> agent;

    Counter(final JCExecutorType in_exec)
    {
      this.agent = JCAgentContract.this.create(in_exec, Integer.valueOf(0));
    }

    CompletableFuture<Unit> increment()
    {
      return this.agent.send(
        (x) -> {
          final Integer next = Integer.valueOf(x.intValue() + 1);
          JCAgentContract.LOG.debug("incrementing: {} -> {}", x, next);
          return Pair.pair(next, Unit.unit());
        });
    }

    int read()
    {
      return this.agent.read().intValue();
    }
  }

  final class BankAccount
  {
    private final JCAgentType<BigDecimal> agent;

    BankAccount(final JCExecutorType in_exec)
    {
      this.agent = JCAgentContract.this.create(in_exec, BigDecimal.ZERO);
    }

    final class InsufficientFunds extends RuntimeException
    {
      InsufficientFunds(final String message)
      {
        super(message);
      }
    }

    CompletableFuture<BigDecimal> withdraw(final BigDecimal amount)
    {
      return this.agent.send((available) -> {
        JCAgentContract.LOG.debug(
          "withdraw: available: {}, requested: {}", available, amount);

        Assert.assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);

        if (available.compareTo(amount) < 0) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Insufficient funds.");
          sb.append(System.lineSeparator());
          sb.append("Requested: ");
          sb.append(amount);
          sb.append(System.lineSeparator());
          sb.append("Available: ");
          sb.append(available);
          sb.append(System.lineSeparator());
          final String message = sb.toString();
          JCAgentContract.LOG.error("withdraw: {}", message);
          throw new InsufficientFunds(message);
        }

        final BigDecimal now = available.subtract(amount);
        return Pair.pair(now, now);
      });
    }

    CompletableFuture<BigDecimal> deposit(final BigDecimal amount)
    {
      return this.agent.send((available) -> {
        JCAgentContract.LOG.debug(
          "deposit: available: {}, requested: {}", available, amount);

        Assert.assertTrue(amount.compareTo(BigDecimal.ZERO) > 0);
        final BigDecimal now = available.add(amount);
        return Pair.pair(now, now);
      });
    }

    BigDecimal read()
    {
      return this.agent.read();
    }
  }

  @Before
  public final void setUp()
  {
    this.executor = JCExecutor.create("agents", 3);
  }

  @After
  public final void tearDown()
    throws IOException
  {
    this.executor.shutdown();
  }

  @Test
  public final void testCounter()
    throws Exception
  {
    final Counter c = new Counter(this.executor);
    Assert.assertEquals(0L, (long) c.read());

    final CompletableFuture<?> f0 = c.increment()
      .thenCompose(u -> c.increment())
      .thenCompose(u -> c.increment());

    f0.get();
    JCAgentContract.LOG.debug("finished f0");

    Assert.assertEquals(3L, (long) c.read());
  }

  @Test
  public final void testBankAccount()
    throws Exception
  {
    final BankAccount c = new BankAccount(this.executor);
    Assert.assertEquals(BigDecimal.ZERO, c.read());

    final CompletableFuture<BigDecimal> f0 =
      c.deposit(BigDecimal.valueOf(100L))
        .thenCompose(now -> c.withdraw(BigDecimal.valueOf(50L)))
        .thenCompose(now -> c.deposit(BigDecimal.valueOf(20L)));

    final BigDecimal x = f0.get();
    JCAgentContract.LOG.debug("finished f0: {}", x);

    Assert.assertEquals(BigDecimal.valueOf(70L), c.read());
    Assert.assertEquals(x, c.read());
  }

  @Test
  public final void testBankAccountBlast()
    throws Exception
  {
    final BankAccount c = new BankAccount(this.executor);
    Assert.assertEquals(BigDecimal.ZERO, c.read());

    CompletableFuture<BigDecimal> f =
      CompletableFuture.completedFuture(BigDecimal.ZERO);
    for (int index = 0; index < 100; ++index) {
      f = f.thenCompose(now -> c.deposit(BigDecimal.valueOf(100L)));
    }

    final BigDecimal x = f.get();
    JCAgentContract.LOG.debug("finished f: {}", x);

    Assert.assertEquals(BigDecimal.valueOf(100L * 100L), c.read());
    Assert.assertEquals(x, c.read());
  }

  @Test
  public final void testBankAccountInsufficient()
    throws Exception
  {
    final BankAccount c = new BankAccount(this.executor);
    Assert.assertEquals(BigDecimal.ZERO, c.read());

    final CompletableFuture<BigDecimal> f0 =
      c.deposit(BigDecimal.valueOf(100L))
        .thenCompose(now -> c.withdraw(BigDecimal.valueOf(101L)))
        .thenCompose(now -> c.deposit(BigDecimal.valueOf(20L)));

    boolean caught = false;
    try {
      f0.get();
      Assert.fail();
    } catch (final ExecutionException e) {
      Assert.assertEquals(BankAccount.InsufficientFunds.class, e.getCause().getClass());
      Assert.assertEquals(BigDecimal.valueOf(100L), c.read());
      caught = true;
    }

    if (!caught) {
      Assert.fail();
    }
  }
}
