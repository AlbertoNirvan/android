package mega.privacy.android.app.fetcher

import coil.key.Keyer
import coil.request.Options

/**
 * Mega thumbnail keyer to build the key for MegaNode thumbnail in the memory cache
 *
 */
internal object MegaThumbnailKeyer : Keyer<ThumbnailRequest> {
    override fun key(data: ThumbnailRequest, options: Options): String =
        "${data.id.longValue}-${options.size}"
}