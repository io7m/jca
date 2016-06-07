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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The type of agents that have internal state of type {@code S}. Values of type
 * {@code S} are assumed to be immutable.
 *
 * @param <S> The type of internal state
 */

public interface JCAgentType<S>
{
  /**
   * Evaluate a function on the agent.
   *
   * @param op  A function that accepts the current state value and returns a
   *            new state value and a result
   * @param <T> A future that returns the result of {@code op}
   *
   * @return A future representing the function to be evaluated
   */

  <T> CompletableFuture<T> send(Function<S, Pair<S, T>> op);

  /**
   * @return The current state value
   */

  S read();
}
