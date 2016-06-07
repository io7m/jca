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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A simplified executor interface that supports sequential execution for groups
 * delimited by integer keys.
 *
 * A given executor may support any number of threads, but all tasks submitted
 * for a given key {@code k} are guaranteed to execute in the order submitted
 * and on the same thread.
 */

public interface JCExecutorType
{
  /**
   * Submits a value-returning task for execution and returns a Future
   * representing the pending results of the task. Tasks submitted for a given
   * key {@code k} are guaranteed to execute in the order submitted, and on the
   * same thread.
   *
   * @param key The task key
   * @param op  The task to execute
   * @param <T> The type of the task's result
   *
   * @return a Future representing pending completion of the task
   *
   * @throws java.util.concurrent.RejectedExecutionException if the task cannot
   *                                                         be scheduled for
   *                                                         execution
   * @throws NullPointerException                            if the task is
   *                                                         null
   */

  <T>
  CompletableFuture<T> submit(
    int key,
    Supplier<T> op);

  /**
   * Initiates an orderly shutdown in which previously submitted tasks are
   * executed, but no new tasks will be accepted. Invocation has no additional
   * effect if already shut down.
   *
   * <p>This method does not wait for previously submitted tasks to complete
   * execution.  Use {@link #awaitTermination awaitTermination} to do that.
   *
   * @throws SecurityException if a security manager exists and shutting down
   *                           this ExecutorService may manipulate threads that
   *                           the caller is not permitted to modify because it
   *                           does not hold {@link java.lang.RuntimePermission}{@code
   *                           ("modifyThread")}, or the security manager's
   *                           {@code checkAccess} method denies access.
   */

  void shutdown();

  /**
   * Attempts to stop all actively executing tasks, halts the processing of
   * waiting tasks, and returns a list of the tasks that were awaiting
   * execution.
   *
   * <p>This method does not wait for actively executing tasks to terminate. Use
   * {@link #awaitTermination awaitTermination} to do that.
   *
   * <p>There are no guarantees beyond best-effort attempts to stop processing
   * actively executing tasks.  For example, typical implementations will cancel
   * via {@link Thread#interrupt}, so any task that fails to respond to
   * interrupts may never terminate.
   *
   * @return list of tasks that never commenced execution
   *
   * @throws SecurityException if a security manager exists and shutting down
   *                           this ExecutorService may manipulate threads that
   *                           the caller is not permitted to modify because it
   *                           does not hold {@link java.lang.RuntimePermission}{@code
   *                           ("modifyThread")}, or the security manager's
   *                           {@code checkAccess} method denies access.
   */

  List<Runnable> shutdownNow();

  /**
   * Returns {@code true} if this executor has been shut down.
   *
   * @return {@code true} if this executor has been shut down
   */

  boolean isShutdown();

  /**
   * Returns {@code true} if all tasks have completed following shut down. Note
   * that {@code isTerminated} is never {@code true} unless either {@code
   * shutdown} or {@code shutdownNow} was called first.
   *
   * @return {@code true} if all tasks have completed following shut down
   */

  boolean isTerminated();

  /**
   * Blocks until all tasks have completed execution after a shutdown request,
   * or the timeout occurs, or the current thread is interrupted, whichever
   * happens first.
   *
   * @param timeout the maximum time to wait
   * @param unit    the time unit of the timeout argument
   *
   * @return {@code true} if this executor terminated and {@code false} if the
   * timeout elapsed before termination
   *
   * @throws InterruptedException if interrupted while waiting
   */

  boolean awaitTermination(
    long timeout,
    TimeUnit unit)
    throws InterruptedException;
}
