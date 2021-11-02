package com.evos.ui.fragments;

import static com.evos.util.BindingUtils.prepareTextSize;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evos.R;
import com.evos.storage.model.Message;
import com.evos.storage.model.Messages;
import com.evos.storage.model.Order;
import com.evos.storage.model.Orders;
import com.evos.storage.observables.DataSubjects;
import com.evos.ui.activities.OrdersListActivity;
import com.evos.ui.adapters.ListOrderAdapter;
import com.evos.ui.adapters.ListRatingAdapter;
import com.evos.ui.adapters.model.InnerRatingHistoryItem;
import com.evos.ui.adapters.model.OrderListItem;
import com.evos.ui.adapters.model.RatingListItem;
import com.evos.ui.adapters.model.ViewOrderHeaderItem;
import com.evos.ui.adapters.model.ViewOrderItem;
import com.evos.ui.adapters.model.ViewRatingHeaderItem;
import com.evos.ui.adapters.model.ViewRatingItem;
import com.evos.view.CustomTextView;
import com.evos.view.impl.MainHomeActivity;
import com.evos.view.impl.RatingActivity;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class InnerRatingFragment extends AbstractStyledFragment
{
	private RecyclerView recyclerView;
	private CustomTextView tvEmpty;
	protected ListRatingAdapter adapter;

	@Override
	public void subscribe(@NonNull DataSubjects ds, @NonNull CompositeDisposable cd)
	{
		cd.add(ds.getInnerRatingHistoryObservable()
				 .map(this::processHistoryUpdate)
				 .observeOn(AndroidSchedulers.mainThread())
				 .subscribe(this::onRatingUpdate));
	}

	private List<RatingListItem> processHistoryUpdate(ArrayList<InnerRatingHistoryItem> ratingHistory)
	{
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy");
		List<RatingListItem> list = new ArrayList<>();
		Map<DateTime, List<InnerRatingHistoryItem>> groupsMap = new TreeMap<>(Collections.reverseOrder());

		for (InnerRatingHistoryItem rating : ratingHistory) {
			DateTime date = new DateTime(rating.getDate(), DateTimeZone.UTC).withMillisOfDay(0);
			List<InnerRatingHistoryItem> value = groupsMap.get(date);
			if (value == null) {
				value = new ArrayList<>();
				groupsMap.put(date, value);
			}
			value.add(rating);
		}

		for (DateTime date : groupsMap.keySet()) {
			ViewRatingHeaderItem header = new ViewRatingHeaderItem(date, dateTimeFormatter.print(date));
			list.add(header);
			for (InnerRatingHistoryItem history : groupsMap.get(date)) {
				ViewRatingItem item = new ViewRatingItem(history);
				list.add(item);
			}
		}

		return list;
	}


	protected void onRatingUpdate(List<RatingListItem> list) {
		if (list == null || list.size() == 0){
			tvEmpty.setVisibility(View.VISIBLE);
			recyclerView.setVisibility(View.GONE);
		}
		else {
			tvEmpty.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);
			
			showData(list);
		}
	}

	private void showData(List<RatingListItem> list) {
		adapter = new ListRatingAdapter(requireActivity(), list);

		RecyclerView.LayoutManager layoutManager = new BlockingLinearLayoutManager(requireActivity());
		recyclerView.setLayoutManager(layoutManager);
		recyclerView.setHasFixedSize(false);
		recyclerView.setItemAnimator(null);
		recyclerView.setNestedScrollingEnabled(false);
		adapter.setTextSize(prepareTextSize());
		recyclerView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}


	@Override
	protected void findViews(View view)
	{
		recyclerView = view.findViewById(R.id.recycler_view);
		tvEmpty = view.findViewById(R.id.tv_empty);
		addStyleableView(tvEmpty);
	}

	@Override
	protected int getLayoutId()
	{
		return R.layout.screen_inner_rating_fragment;
	}

	@Override
	protected void setInteractionHandlers()
	{
	}

	@Override
	public void applyStyle()
	{
		super.applyStyle();
	}


	private static class BlockingLinearLayoutManager extends LinearLayoutManager
	{
		private boolean isScrollEnabled = true;

		BlockingLinearLayoutManager(Context context) {
			super(context, LinearLayoutManager.VERTICAL, false);
		}

		@Override
		public boolean canScrollVertically() {
			return isScrollEnabled && super.canScrollVertically();
		}

		void setScrollEnabled(boolean isEnabled) {
			this.isScrollEnabled = isEnabled;
		}
	}
}