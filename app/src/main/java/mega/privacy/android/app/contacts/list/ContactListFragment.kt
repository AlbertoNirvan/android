package mega.privacy.android.app.contacts.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.FragmentContactListBinding
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactListFragment : Fragment() {

    private lateinit var binding: FragmentContactListBinding

    private val viewModel by viewModels<ContactListViewModel>()
    private val recentlyAddedAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick, false)
    }
    private val contactsAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick, true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    private fun setupView() {
        val adapterConfig = ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.listContacts.adapter = ConcatAdapter(adapterConfig, recentlyAddedAdapter, contactsAdapter)
        binding.listContacts.setHasFixedSize(true)
        binding.listContacts.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )

        binding.btnRequests.setOnClickListener {
            findNavController().navigate(ContactListFragmentDirections.actionListToRequests())
        }
        binding.btnAddContact.setOnClickListener { openAddContactScreen() }
        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.black)
            .formatColorTag(requireContext(), 'B', R.color.grey_300)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {
        viewModel.getContacts().observe(viewLifecycleOwner, ::showContacts)
        viewModel.getRecentlyAddedContacts().observe(viewLifecycleOwner, ::showRecentlyAddedContacts)
    }

    private fun showContacts(items: List<ContactItem>) {
        binding.txtContacts.isVisible = items.isNotEmpty()
        binding.viewEmpty.isVisible = items.isNullOrEmpty()
        contactsAdapter.submitList(items)
    }

    private fun showRecentlyAddedContacts(items: List<ContactItem>) {
        binding.txtContacts.isVisible = items.isNotEmpty()
        recentlyAddedAdapter.submitList(items)
    }

    private fun onContactClick(userHandle: Long) {
        // Do something
    }

    private fun onContactMoreInfoClick(userHandle: Long) {
        // Do something
    }

    private fun openAddContactScreen() {
        startActivity(Intent(requireContext(), AddContactActivityLollipop::class.java))
    }
}
