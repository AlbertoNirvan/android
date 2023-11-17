package mega.privacy.android.app.presentation.videosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.app.presentation.videosection.model.VideoSectionState
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTabState
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import javax.inject.Inject

/**
 * Videos section view model
 */
@HiltViewModel
class VideoSectionViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val uiVideoMapper: UIVideoMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
) : ViewModel() {
    private val _state = MutableStateFlow(VideoSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<VideoSectionState> = _state.asStateFlow()

    private val _tabState = MutableStateFlow(VideoSectionTabState())

    /**
     * The state regarding the tabs
     */
    val tabState = _tabState.asStateFlow()

    init {
        viewModelScope.launch {
            getAllVideosUseCase(getCloudSortOrder()).collect { videos ->
                _state.update {
                    it.copy(allVideos = videos.map { videoNode ->
                        uiVideoMapper(videoNode)
                    })
                }
            }
        }
    }

    internal fun onTabSelected(selectTab: VideoSectionTab) = _tabState.update {
        it.copy(selectedTab = selectTab)
    }
}