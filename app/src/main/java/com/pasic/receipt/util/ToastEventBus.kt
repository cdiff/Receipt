package com.pasic.receipt.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 앱 전체에서 화면 전환/수명주기(Lifecycle)에 구애받지 않고
 * 최상위 오버레이 토스트를 띄우기 위한 전역 싱글톤 이벤트 버스.
 */
object ToastEventBus {
    private val _toastEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    fun showToast(message: String) {
        if (message.isNotBlank()) {
            _toastEvents.tryEmit(message)
        }
    }
}
