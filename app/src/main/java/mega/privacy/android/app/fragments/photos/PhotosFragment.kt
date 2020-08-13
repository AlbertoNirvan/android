package mega.privacy.android.app.fragments.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: PhotosViewModel

    @Inject
    lateinit var actionModeViewModel: ActionModeViewModel

    private lateinit var binding: FragmentPhotosBinding
    private lateinit var listView: NewGridRecyclerView

    @Inject
    lateinit var listAdapter: PhotosGridAdapter

    private var actionMode: ActionMode? = null

    @Inject
    lateinit var actionModeCallback: ActionModeCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
            actionModeViewModel = this@PhotosFragment.actionModeViewModel
        }

        listView = binding.photoList
        preventListItemBlink()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        setupListAdapter()
        setupFastScroller()
        setupNavigation()
        setupActionMode()
        Log.i("Alex", "viewmodel:$viewModel")

        viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
    }

    private fun preventListItemBlink() {
        val animator = listView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun setupActionMode() {
        observeSelectedNodes()
        observeSelectAll()
        observeAnimatedNodes()
        observeActionModeDestroy()
    }

    private fun observeSelectedNodes() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = getRealNodeCount()

                if (actionMode == null) {
                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeSelectAll() =
        actionModeViewModel.selectAllEvent.observe(viewLifecycleOwner, Observer {
            viewModel.items.value?.run {
                actionModeViewModel.selectAll(filter { it.node != null })
            }
        })

    private fun observeAnimatedNodes() =
        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner, Observer {
            val animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    // Refresh the Ui here is necessary. The reason is certain cache mechanism of
                    // RecyclerView would cause a couple of selected icons failed to be updated even
                    // though listView.setItemViewCacheSize(0) was called (some ItemViews just out of
                    // the screen are already generated but get ViewHolders return null,
                    // and their bind() wouldn't be invoked via scrolling)
                    updateUi()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    viewHolder.itemView.findViewById<ImageView>(
                        R.id.icon_selected
                    )?.run {
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.photo_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet.playTogether(animatorList)
            animatorSet.start()
        })

    private fun observeActionModeDestroy() =
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, Observer {
            actionMode = null
        })

    private fun updateUi() {
        viewModel.items.value?.let { it ->
            listAdapter.submitList(ArrayList<PhotoNode>(it))
        }
    }

    private fun getRealNodeCount(): Int {
        viewModel.items.value?.filter { it.type == PhotoNode.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    private fun setupFastScroller() {
        binding.scroller.setRecyclerView(listView)
    }

    private fun setupNavigation() {
    }

    private fun setupListAdapter() {
        listView.layoutManager?.apply {
            spanSizeLookup = listAdapter.getSpanSizeLookup(spanCount)
            val itemDimen =
                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
                    .toInt() * 2
            listAdapter.setItemDimen(itemDimen)
        }

        listView.adapter = listAdapter
    }
}