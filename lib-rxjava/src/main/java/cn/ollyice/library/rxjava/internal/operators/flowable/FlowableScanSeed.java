/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package cn.ollyice.library.rxjava.internal.operators.flowable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import cn.ollyice.library.reactivestreams.*;

import cn.ollyice.library.rxjava.*;
import cn.ollyice.library.rxjava.exceptions.Exceptions;
import cn.ollyice.library.rxjava.functions.BiFunction;
import cn.ollyice.library.rxjava.internal.functions.ObjectHelper;
import cn.ollyice.library.rxjava.internal.fuseable.SimplePlainQueue;
import cn.ollyice.library.rxjava.internal.queue.SpscArrayQueue;
import cn.ollyice.library.rxjava.internal.subscriptions.*;
import cn.ollyice.library.rxjava.internal.util.BackpressureHelper;
import cn.ollyice.library.rxjava.plugins.RxJavaPlugins;

public final class FlowableScanSeed<T, R> extends AbstractFlowableWithUpstream<T, R> {
    final BiFunction<R, ? super T, R> accumulator;
    final Callable<R> seedSupplier;

    public FlowableScanSeed(Flowable<T> source, Callable<R> seedSupplier, BiFunction<R, ? super T, R> accumulator) {
        super(source);
        this.accumulator = accumulator;
        this.seedSupplier = seedSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        R r;

        try {
            r = ObjectHelper.requireNonNull(seedSupplier.call(), "The seed supplied is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }

        source.subscribe(new ScanSeedSubscriber<T, R>(s, accumulator, r, bufferSize()));
    }

    static final class ScanSeedSubscriber<T, R>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {
        private static final long serialVersionUID = -1776795561228106469L;

        final Subscriber<? super R> actual;

        final BiFunction<R, ? super T, R> accumulator;

        final SimplePlainQueue<R> queue;

        final AtomicLong requested;

        final int prefetch;

        final int limit;

        volatile boolean cancelled;

        volatile boolean done;
        Throwable error;

        Subscription s;

        R value;

        int consumed;

        ScanSeedSubscriber(Subscriber<? super R> actual, BiFunction<R, ? super T, R> accumulator, R value, int prefetch) {
            this.actual = actual;
            this.accumulator = accumulator;
            this.value = value;
            this.prefetch = prefetch;
            this.limit = prefetch - (prefetch >> 2);
            this.queue = new SpscArrayQueue<R>(prefetch);
            this.queue.offer(value);
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(prefetch - 1);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            R v = value;
            try {
                v = ObjectHelper.requireNonNull(accumulator.apply(v, t), "The accumulator returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
                return;
            }

            value = v;
            queue.offer(v);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> a = actual;
            SimplePlainQueue<R> q = queue;
            int lim = limit;
            int c = consumed;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }
                    boolean d = done;

                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();
                            a.onError(ex);
                            return;
                        }
                    }

                    R v = q.poll();
                    boolean empty = v == null;

                    if (d && empty) {
                        a.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;
                    if (++c == lim) {
                        c = 0;
                        s.request(lim);
                    }
                }

                if (e == r) {
                    if (done) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();
                            a.onError(ex);
                            return;
                        }
                        if (q.isEmpty()) {
                            a.onComplete();
                            return;
                        }
                    }
                }

                if (e != 0L) {
                    BackpressureHelper.produced(requested, e);
                }

                consumed = c;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
