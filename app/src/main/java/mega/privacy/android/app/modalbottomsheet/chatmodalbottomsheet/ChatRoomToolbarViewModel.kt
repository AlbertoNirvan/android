package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.main.megachat.usecase.GetGalleryFilesUseCase
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import javax.inject.Inject


@HiltViewModel
class ChatRoomToolbarViewModel @Inject constructor(
    private val getGalleryFilesUseCase: GetGalleryFilesUseCase,
) : BaseRxViewModel() {

    private val _imagesGallery =
        MutableStateFlow<List<FileGalleryItem>>(ArrayList())

    val gallery: StateFlow<List<FileGalleryItem>>
        get() = _imagesGallery

    /**
     * How to get images and videos from the gallery
     */
    fun loadGallery(){
        getGalleryFilesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = { items ->
                            _imagesGallery.value = items
                        },
                        onError = { error ->
                            LogUtil.logError(error.stackTraceToString())
                        }
                )
                .addTo(composite)
    }

    fun getDefaultLocation(): String =
        FileUtil.getDownloadLocation()
}