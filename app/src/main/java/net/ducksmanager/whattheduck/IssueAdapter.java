package net.ducksmanager.whattheduck;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import net.ducksmanager.retrievetasks.GetPurchaseList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class IssueAdapter extends ItemAdapter<Issue> {
    IssueAdapter(ItemList itemList, ArrayList<Issue> items) {
        super(itemList, R.layout.row, items);
    }

    @Override
    protected ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    protected View.OnClickListener getOnClickListener() {
        return view -> {
            int position = ((RecyclerView) view.getParent()).getChildLayoutPosition(view);
            if (ItemList.type.equals(Collection.CollectionType.COA.toString())) {
                final Issue selectedIssue = IssueAdapter.this.getItem(position);
                if (WhatTheDuck.userCollection.getIssue(WhatTheDuck.getSelectedCountry(), WhatTheDuck.getSelectedPublication(), selectedIssue.getIssueNumber()) != null) {
                    WhatTheDuck.wtd.info(new WeakReference<>(IssueAdapter.this.getOriginActivity()), R.string.input_error__issue_already_possessed, Toast.LENGTH_SHORT);
                } else {
                    WhatTheDuck.wtd.toggleProgressbarLoading(new WeakReference<>(IssueAdapter.this.getOriginActivity()), true);
                    WhatTheDuck.setSelectedIssue(selectedIssue.getIssueNumber());
                    GetPurchaseList.initAndShowAddIssue(IssueAdapter.this.getOriginActivity());
                }
            }
        };
    }

    class ViewHolder extends ItemAdapter.ViewHolder {
        ViewHolder(View v) {
            super(v);
        }
    }

    @Override
    protected boolean isHighlighted(Issue i) {
        return i.getIssueCondition() != null;
    }

    @Override
    protected Integer getPrefixImageResource(Issue i, Activity activity) {
        if (this.resourceToInflate == R.layout.row && i.getIssueCondition() != null) {
            return Issue.issueConditionToResourceId(i.getIssueCondition());
        } else {
            return android.R.color.transparent;
        }
    }

    @Override
    protected Integer getSuffixImageResource(Issue i) {
        if (this.resourceToInflate == R.layout.row && i.getPurchase() != null) {
            return R.drawable.ic_clock;
        } else {
            return null;
        }
    }

    @Override
    protected String getSuffixText(Issue i) {
        if (this.resourceToInflate == R.layout.row && i.getPurchase() != null) {
            return PurchaseAdapter.dateFormat.format(i.getPurchase().getPurchaseDate());
        } else {
            return null;
        }
    }

    @Override
    protected String getIdentifier(Issue i) {
        return i.getIssueNumber();
    }

    @Override
    protected String getText(Issue i) {
        return i.getIssueNumber();
    }

    @Override
    protected String getComparatorText(Issue i) {
        return getText(i);
    }
}
