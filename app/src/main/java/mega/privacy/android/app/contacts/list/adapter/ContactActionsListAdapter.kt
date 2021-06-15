package mega.privacy.android.app.contacts.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.list.data.ContactActionItem
import mega.privacy.android.app.databinding.ItemContactActionBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactActionsListAdapter(
    private val onRequestsCallback: () -> Unit,
    private val onGroupsCallback: () -> Unit
) : ListAdapter<ContactActionItem, ContactActionViewHolder>(ContactActionItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactActionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactActionBinding.inflate(layoutInflater, parent, false)
        return ContactActionViewHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    when (viewType) {
                        ContactActionItem.Type.REQUESTS.ordinal -> onRequestsCallback.invoke()
                        else -> onGroupsCallback.invoke()
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactActionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int =
        getItem(position).id.ordinal

    override fun getItemId(position: Int): Long =
        getItem(position).id.ordinal.toLong()

}
