package com.gullesnuffs.codenames

import android.os.Handler
import java.util.ArrayList

interface IObservable {
    fun listen (listener : (() -> Unit))
}

class Observable<T>(initial : T) : IObservable {
    private var mValue = initial
    private val listeners = ArrayList<((T, T) -> Unit)>()
    private val handler = Handler()

    var value : T
        get() = mValue
        set(value) {
            if (value != mValue) {
                val prev = mValue
                mValue = value
                onChanged(prev, value)
            }
        }

    fun setSilently(value : T) {
        mValue = value
    }

    override fun listen (listener : (() -> Unit)) {
        listen { a, b -> listener() }
    }

    fun listen (listener : ((T,T) -> Unit)) {
        listeners.add(listener)
    }

    private fun onChanged(prev : T, current : T) {
        handler.post({
            for (listener in listeners) {
                listener(prev, current)
            }
        })
    }

    fun init() {
        onChanged(value, value)
    }
}

fun <T>react (listener : ((T,T) -> Unit), observable : Observable<T>) {
    observable.listen(listener)
}

fun react(listener : (() -> Unit), vararg observables : IObservable) {
    for(observable in observables) {
        observable.listen(listener)
    }
}

fun <T>init(vararg observables : Observable<T>) {
    for(observable in observables) {
        observable.init()
    }
}