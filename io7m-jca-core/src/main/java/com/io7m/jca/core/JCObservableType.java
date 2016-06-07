package com.io7m.jca.core;

import java.util.function.Consumer;

public interface JCObservableType<S>
{
  /**
   * Observe state changes to the agent.
   *
   * @param handler A function evaluated on each state change
   *
   * @return A new observation
   */

  JCObservationType watch(Consumer<S> handler);
}
