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

import cn.ollyice.library.reactivestreams.Subscriber;

import cn.ollyice.library.rxjava.Flowable;
import cn.ollyice.library.rxjava.internal.fuseable.ScalarCallable;
import cn.ollyice.library.rxjava.internal.subscriptions.EmptySubscription;

/**
 * A source Flowable that signals an onSubscribe() + onComplete() only.
 */
public final class FlowableEmpty extends Flowable<Object> implements ScalarCallable<Object> {

    public static final Flowable<Object> INSTANCE = new FlowableEmpty();

    private FlowableEmpty() {
    }

    @Override
    public void subscribeActual(Subscriber<? super Object> s) {
        EmptySubscription.complete(s);
    }

    @Override
    public Object call() {
        return null; // null scalar is interpreted as being empty
    }
}
