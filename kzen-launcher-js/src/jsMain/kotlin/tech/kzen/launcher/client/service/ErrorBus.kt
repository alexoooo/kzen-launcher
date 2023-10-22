package tech.kzen.launcher.client.service


object ErrorBus {
    interface Subscriber {
        fun onError(message: String)
        fun onSuccess()
    }


    private val subscribers = mutableListOf<Subscriber>()


    fun subscribe(subscriber: Subscriber) {
        subscribers.add(subscriber)
    }


    fun unSubscribe(subscriber: Subscriber) {
        subscribers.remove(subscriber)
    }


    fun onError(message: String) {
        subscribers.forEach { it.onError(message) }
    }

    fun onSuccess() {
        subscribers.forEach { it.onSuccess() }
    }
}