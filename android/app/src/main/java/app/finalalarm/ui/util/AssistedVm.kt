package app.finalalarm.ui.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Hilt AssistedInject VM을 ViewModelStore에 등록해서 라이프사이클 누수 없이 사용.
 * @param key 같은 화면에서 인자가 바뀌면 새 VM 인스턴스 받기 위한 키.
 * @param create 호스트 VM에서 받아온 factory의 create 호출.
 */
@Composable
inline fun <reified VM : ViewModel> assistedViewModel(
    key: Any,
    crossinline create: () -> VM,
): VM = viewModel(
    key = key.toString(),
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    },
)
