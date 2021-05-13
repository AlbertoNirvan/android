package mega.privacy.android.app.contacts.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactListAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemInfoCallback: (Long) -> Unit,
    private val enableHeaders: Boolean = true
) : ListAdapter<ContactItem, ContactListViewHolder>(ContactItem.DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ITEM_WITH_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactBinding.inflate(layoutInflater, parent, false)
        return ContactListViewHolder(binding).apply {
            binding.txtHeader.isVisible = viewType == VIEW_TYPE_ITEM_WITH_HEADER
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
            binding.btnMore.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemInfoCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle

    override fun getItemViewType(position: Int): Int =
        when {
            !enableHeaders ->
                VIEW_TYPE_ITEM
            position == 0 ->
                VIEW_TYPE_ITEM_WITH_HEADER
            getItem(position).name?.firstOrNull()?.isLetter() == true &&
                getItem(position - 1).name?.firstOrNull() != getItem(position).name?.firstOrNull() ->
                VIEW_TYPE_ITEM_WITH_HEADER
            else ->
                VIEW_TYPE_ITEM
        }
}
