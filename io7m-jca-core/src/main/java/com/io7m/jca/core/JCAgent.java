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
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
  private final Map<Observation<S>, Unit> observers;
  private volatile S state;

  private JCAgent(
    final JCExecutorType in_exec,
    final S initial)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.state = NullCheck.notNull(initial);
    this.index = this.hashCode();
    this.observers = new ConcurrentHashMap<>(0);
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
    return this.exec.submit(this.index, () -> this.run(op));
  }

  private <T> T run(final Function<S, Pair<S, T>> op)
  {
    final Pair<S, T> p = op.apply(this.state);
    final S new_state = p.getLeft();
    this.state = new_state;

    for (final Observation<S> o : this.observers.keySet()) {
      o.handler.accept(new_state);
    }

    return p.getRight();
  }

  @Override
  public S read()
  {
    return this.state;
  }

  @Override
  public JCObservationType watch(final Consumer<S> handler)
  {
    final Observation<S> o =
      new Observation<>(handler, new WeakReference<>(this));
    this.observers.put(o, Unit.unit());
    return o;
  }

  private static final class Observation<S> implements JCObservationType
  {
    private final Consumer<S> handler;
    private final WeakReference<JCAgent<S>> agent;

    Observation(
      final Consumer<S> in_handler,
      final WeakReference<JCAgent<S>> in_agent)
    {
      this.handler = NullCheck.notNull(in_handler);
      this.agent = NullCheck.notNull(in_agent);
    }

    @Override
    public void unwatch()
    {
      final JCAgent<S> a = this.agent.get();
      if (a != null) {
        a.observers.remove(this);
        this.agent.clear();
      }
    }
  }
}
