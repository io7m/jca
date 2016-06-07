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

import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The default implementation of the {@link JCAgentType} interface.
 *
 * @param <S> The type of state values
 */

public final class JCAgent<S> implements JCAgentType<S>
{
  private final JCExecutorType exec;
  private final int index;
  private S state;

  private JCAgent(
    final JCExecutorType in_exec,
    final S initial)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.state = NullCheck.notNull(initial);
    this.index = this.hashCode();
  }

  /**
   * Create a new agent.
   *
   * @param in_exec An agent executor
   * @param x       An initial state value
   * @param <S>     The type of internal state
   *
   * @return A new agent
   */

  public static <S> JCAgentType<S> create(
    final JCExecutorType in_exec,
    final S x)
  {
    return new JCAgent<>(in_exec, x);
  }

  @Override
  public <T> CompletableFuture<T> send(
    final Function<S, Pair<S, T>> op)
  {
    NullCheck.notNull(op);
    return this.exec.submit(this.index, () -> {
      final Pair<S, T> p = op.apply(this.state);
      this.state = p.getLeft();
      return p.getRight();
    });
  }

  @Override
  public S read()
  {
    return this.state;
  }
}
