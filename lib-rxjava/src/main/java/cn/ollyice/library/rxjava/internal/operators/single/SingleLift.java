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

import cn.ollyice.library.rxjava.*;
import cn.ollyice.library.rxjava.exceptions.Exceptions;
import cn.ollyice.library.rxjava.internal.disposables.EmptyDisposable;
import cn.ollyice.library.rxjava.internal.functions.ObjectHelper;

public final class SingleLift<T, R> extends Single<R> {

    final SingleSource<T> source;

    final SingleOperator<? extends R, ? super T> onLift;

    public SingleLift(SingleSource<T> source, SingleOperator<? extends R, ? super T> onLift) {
        this.source = source;
        this.onLift = onLift;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> s) {
        SingleObserver<? super T> sr;

        try {
            sr = ObjectHelper.requireNonNull(onLift.apply(s), "The onLift returned a null SingleObserver");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, s);
            return;
        }

        source.subscribe(sr);
    }

}
