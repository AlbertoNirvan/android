package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.domain.entity.search.SearchCategory
import javax.inject.Inject

/**
 * Mapper used to map string names to search categories
 *
 * These strings are shown to the users as filter chips
 */
class SearchFilterMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * invoke
     *
     * @param category search category enum [SearchCategory]
     */
    operator fun invoke(category: SearchCategory) = when (category) {
        SearchCategory.ALL -> SearchFilter(
            SearchCategory.ALL,
            context.getString(R.string.all_view_button)
        )

        SearchCategory.IMAGES -> SearchFilter(
            SearchCategory.IMAGES,
            context.getString(R.string.section_images)
        )

        SearchCategory.ALL_DOCUMENTS -> SearchFilter(
            SearchCategory.ALL_DOCUMENTS,
            context.getString(R.string.section_documents)
        )

        SearchCategory.AUDIO -> SearchFilter(
            SearchCategory.AUDIO,
            context.getString(R.string.upload_to_audio)
        )

        SearchCategory.VIDEO -> SearchFilter(
            SearchCategory.VIDEO,
            context.getString(R.string.upload_to_video)
        )

        SearchCategory.PDF -> SearchFilter(
            SearchCategory.PDF,
            "Pdf"//            context.getString(R.string.section_pdf)
        )

        SearchCategory.PRESENTATION -> SearchFilter(
            SearchCategory.PRESENTATION,
            "Presentation"//context.getString(R.string.section_presentation)
        )

        SearchCategory.SPREADSHEET -> SearchFilter(
            SearchCategory.SPREADSHEET,
            "Spreadsheet"// context.getString(R.string.section_spreadsheet)
        )

        SearchCategory.FOLDER -> SearchFilter(
            SearchCategory.FOLDER,
            "Folder"//context.getString(R.string.section_folder)
        )

        SearchCategory.OTHER -> SearchFilter(
            SearchCategory.OTHER,
            "Other"//context.getString(R.string.section_other)
        )

        SearchCategory.DOCUMENTS -> SearchFilter(
            SearchCategory.DOCUMENTS,
            context.getString(R.string.section_documents)
        )

        else -> SearchFilter(
            SearchCategory.ALL,
            "All"
        )
    }
}