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

package cn.ollyice.library.rxjava.internal.operators.single;

import java.util.concurrent.atomic.AtomicReference;

import cn.ollyice.library.rxjava.*;
import cn.ollyice.library.rxjava.disposables.Disposable;
import cn.ollyice.library.rxjava.internal.disposables.DisposableHelper;

/**
 * Makes sure a dispose() call from downstream happens on the specified scheduler.
 * 
 * @param <T> the value type
 */
public final class SingleUnsubscribeOn<T> extends Single<T> {

    final SingleSource<T> source;

    final Scheduler scheduler;

    public SingleUnsubscribeOn(SingleSource<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new UnsubscribeOnSingleObserver<T>(observer, scheduler));
    }

    static final class UnsubscribeOnSingleObserver<T> extends AtomicReference<Disposable>
    implements SingleObserver<T>, Disposable, Runnable {

        private static final long serialVersionUID = 3256698449646456986L;

        final SingleObserver<? super T> actual;

        final Scheduler scheduler;

        Disposable ds;

        UnsubscribeOnSingleObserver(SingleObserver<? super T> actual, Scheduler scheduler) {
            this.actual = actual;
            this.scheduler = scheduler;
        }

        @Override
        public void dispose() {
            Disposable d = getAndSet(DisposableHelper.DISPOSED);
            if (d != DisposableHelper.DISPOSED) {
                this.ds = d;
                scheduler.scheduleDirect(this);
            }
        }

        @Override
        public void run() {
            ds.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
    }
}
