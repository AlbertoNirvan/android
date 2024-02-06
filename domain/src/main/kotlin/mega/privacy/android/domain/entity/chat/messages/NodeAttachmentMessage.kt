package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.FileNode

/**
 * Node attachment message
 * @property fileNode The attached node
 */
data class NodeAttachmentMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    override val reactions: List<Reaction>,
    val fileNode: FileNode,
) : AttachmentMessage {
    override val fileSize = fileNode.size
    override val fileName = fileNode.name
    override val duration = (fileNode.type as? VideoFileTypeInfo)?.duration
    override val fileType = fileNode.type
}
