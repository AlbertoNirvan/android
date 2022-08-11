package mega.privacy.android.app.presentation.shares.incoming.model

import nz.mega.sdk.MegaNode

/**
 * Incoming shares UI state
 *
 * @param incomingHandle current incoming shares handle
 * @param incomingTreeDepth current incoming tree depth
 * @param incomingParentHandle parent handle of the current incoming node
 * @param nodes current list of nodes
 * @param isInvalidHandle true if parent handle is invalid
 * @param isLoading true if the nodes are loading
 */
data class IncomingSharesState(
    val incomingHandle: Long = -1L,
    val incomingTreeDepth: Int = 0,
    val incomingParentHandle: Long? = null,
    val nodes: List<MegaNode> = emptyList(),
    val isInvalidHandle: Boolean = true,
    val isLoading: Boolean = false,
) {

    /**
     * Check if we are at the root of the incoming shares page
     *
     * @return true if at the root of the incoming shares page
     */
    fun isFirstNavigationLevel() = incomingTreeDepth == 0
}