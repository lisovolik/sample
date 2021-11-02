package com.evos.whitelabelblank.mvp.favorites

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.evos.whitelabelblank.MainApplication
import com.evos.whitelabelblank.R
import com.evos.whitelabelblank.databinding.FragmentFavoriteAddressesBinding
import com.evos.whitelabelblank.mvp.AbstractFragment
import com.evos.whitelabelblank.mvp.choiceaddress.AddressObject
import com.evos.whitelabelblank.mvp.choiceaddress.IChoiceAddress
import com.evos.whitelabelblank.mvp.main.listeners.MainNavigationListener
import com.evos.whitelabelblank.network.responses.AddressItem
import com.evos.whitelabelblank.usecases.favorites.IFavoriteAddressesUseCase
import com.evos.whitelabelblank.view.actionbar.ActionBarBackTitle
import com.evos.whitelabelblank.view.error.ErrorDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class FavoritesAddressesFragment : AbstractFragment(), FavoriteAddressAdapter.FavoriteAddressesListener, IChoiceAddress {

    @Inject
    lateinit var addressUseCase: IFavoriteAddressesUseCase

    private var _binding: FragmentFavoriteAddressesBinding? = null
    private val binding get() = _binding!!

    private var listener: MainNavigationListener? = null
    var adapter: FavoriteAddressAdapter? = null
    private var selectedItemType = AddressItem.AddressType.FAVORITE

    companion object {
        private const val MAX_ADDRESS_COUNT: Int = 12
    }

    override fun onAttach(context: Context) {
        if (activity is MainNavigationListener) {
            listener = activity as MainNavigationListener
        }
        (context.applicationContext as MainApplication).profileComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoriteAddressesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initializeViews(view: View) {
        super.initializeViews(view)
        (view.findViewById(R.id.favorite_addresses_action_bar) as ActionBarBackTitle)
                .setOnBackButtonClickListener { closeFragment() }

        binding.rvFavAddresses.layoutManager = LinearLayoutManager(context)
        binding.fabAddAddress.setOnClickListener {
            selectedItemType = AddressItem.AddressType.FAVORITE
            addAddress()
        }
    }

    private fun checkArguments(item: AddressItem?) {
        item?.let {
            item.addressType = selectedItemType
            listener!!.goToAddressEditor()
        }
    }

    override fun subscribe(cd: CompositeDisposable) {
        cd.addAll(addressUseCase.getFavoriteAddresses(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{updateAdapter(it)},

                AddressObject.getAddressObservable()
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe { checkArguments(it) }
        )
    }


    private fun updateAdapter(list: MutableList<AddressItem>) {
        adapter = FavoriteAddressAdapter(list, this)
        binding.rvFavAddresses.adapter = adapter
    }

    private fun addAddress() {
        adapter ?: return
        if (canAddAddress()) {
            showMapScreen(AddressItem(selectedItemType))
        } else parentFragmentManager.let { MaxAddressCountErrorDialog().show(it, null) }
    }

    private fun canAddAddress() =
            adapter?.itemCount!! < MAX_ADDRESS_COUNT
                    || selectedItemType == AddressItem.AddressType.HOME
                    || selectedItemType == AddressItem.AddressType.WORK

    override fun onAddressClick(item: AddressItem) {
        if (item.addressType == null) {
            item.addressType = AddressItem.AddressType.FAVORITE
            item.type = AddressItem.AddressType.FAVORITE.value
        }
        selectedItemType = item.addressType!!
        if (needAddAddress(item)) {
            addAddress()
        } else {
            AddressObject.getAddressObserver().onNext(item)
        }
    }

    private fun needAddAddress(item: AddressItem) =
            item.addressType != AddressItem.AddressType.FAVORITE &&
                    (item.address == null || item.address?.name.isNullOrBlank())

    override fun showMapScreen(address: AddressItem) {
        AddressObject.getMapAddressObserver().onNext(address)
        listener!!.goToMapScreenFromFavorite()
    }

    class MaxAddressCountErrorDialog : ErrorDialog() {
        override fun getTitle(): String {
            return requireContext().getString(R.string.error)
        }

        override fun getText(): String {
            return requireContext().getString(R.string.max_favorite_address_count)
        }
    }
}
