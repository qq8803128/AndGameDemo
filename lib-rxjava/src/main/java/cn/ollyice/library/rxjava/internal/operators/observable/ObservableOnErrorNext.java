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

package cn.ollyice.library.rxjava.internal.operators.observable;

import cn.ollyice.library.rxjava.*;
import cn.ollyice.library.rxjava.disposables.Disposable;
import cn.ollyice.library.rxjava.exceptions.*;
import cn.ollyice.library.rxjava.functions.Function;
import cn.ollyice.library.rxjava.internal.disposables.SequentialDisposable;
import cn.ollyice.library.rxjava.plugins.RxJavaPlugins;

public final class ObservableOnErrorNext<T> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier;
    final boolean allowFatal;

    public ObservableOnErrorNext(ObservableSource<T> source,
                                 Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier, boolean allowFatal) {
        super(source);
        this.nextSupplier = nextSupplier;
        this.allowFatal = allowFatal;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        OnErrorNextObserver<T> parent = new OnErrorNextObserver<T>(t, nextSupplier, allowFatal);
        t.onSubscribe(parent.arbiter);
        source.subscribe(parent);
    }

    static final class OnErrorNextObserver<T> implements Observer<T> {
        final Observer<? super T> actual;
        final Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier;
        final boolean allowFatal;
        final SequentialDisposable arbiter;

        boolean once;

        boolean done;

        OnErrorNextObserver(Observer<? super T> actual, Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier, boolean allowFatal) {
            this.actual = actual;
            this.nextSupplier = nextSupplier;
            this.allowFatal = allowFatal;
            this.arbiter = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable s) {
            arbiter.replace(s);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                actual.onError(t);
                return;
            }
            once = true;

            if (allowFatal && !(t instanceof Exception)) {
                actual.onError(t);
                return;
            }

            ObservableSource<? extends T> p;

            try {
                p = nextSupplier.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(new CompositeException(t, e));
                return;
            }

            if (p == null) {
                NullPointerException npe = new NullPointerException("Observable is null");
                npe.initCause(t);
                actual.onError(npe);
                return;
            }

            p.subscribe(this);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            once = true;
            actual.onComplete();
        }
    }
}
