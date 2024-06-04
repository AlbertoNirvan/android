package mega.privacy.android.domain.usecase.offline


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetOfflineFileInformationUseCaseTest {

    private lateinit var underTest: GetOfflineFileInformationUseCase
    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val getOfflineFolderInformationUseCase = mock<GetOfflineFolderInformationUseCase>()
    private val getOfflineFileTotalSizeUseCase = mock<GetOfflineFileTotalSizeUseCase>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineFileInformationUseCase(
            getOfflineFileUseCase,
            getThumbnailUseCase,
            getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase
        )
    }

    @BeforeEach
    fun initStubCommon() {
        runBlocking {
            stubCommon()
        }
    }

    private suspend fun stubCommon() {
        val offlineFile = File(temporaryFolder, "OfflineFile.jpg")
        offlineFile.createNewFile()
        whenever(getOfflineFileUseCase(any())) doReturn (offlineFile)
        whenever(getOfflineFileTotalSizeUseCase(any())) doReturn (1000L)
    }

    @Test
    fun `test that folderInfo is set when node is a folder`() = runTest {
        val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
            on { id } doReturn 3
            on { isFolder } doReturn true
            on { name } doReturn "title"
            on { lastModifiedTime } doReturn 5679
            on { handle } doReturn "3"
        }
        val folderInfo = OfflineFolderInfo(0, 2)
        whenever(getOfflineFolderInformationUseCase(any())) doReturn (folderInfo)
        val result = underTest(offlineNodeInformation)
        assertThat(result.folderInfo).isEqualTo(folderInfo)
        assertThat(result.totalSize).isEqualTo(1000L)
    }

    @Test
    fun `test that thumbnail is set null when node is a folder`() =
        runTest {
            val offlineNodeInformation = mock<OtherOfflineNodeInformation> {
                on { id } doReturn 3
                on { isFolder } doReturn true
                on { name } doReturn "title.jpg"
                on { lastModifiedTime } doReturn 5679
                on { handle } doReturn "3"
            }
            whenever(getOfflineFileUseCase(any())) doReturn (File(temporaryFolder, "NonExistent"))

            val result = underTest(offlineNodeInformation)
            assertThat(result.thumbnail).isNull()
        }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineFileUseCase,
            getThumbnailUseCase,
            getOfflineFolderInformationUseCase,
            getOfflineFileTotalSizeUseCase,
        )
    }
}