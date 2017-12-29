package net.ducksmanager.whattheduck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import net.ducksmanager.inducks.coa.IssueListing;
import net.ducksmanager.retrievetasks.GetPurchaseList;
import net.ducksmanager.whattheduck.Collection.CollectionType;

import java.lang.ref.WeakReference;

public class IssueList extends List<Issue> {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String selectedCountry = WhatTheDuck.getSelectedCountry();
        final String selectedPublication = WhatTheDuck.getSelectedPublication();

        if (IssueListing.hasFullList(selectedPublication)) {
            this.show();
        }
        else {
            new IssueListing(this, selectedCountry, selectedPublication, activity ->
                ((List)activity.get()).show()
            ).execute();
        }

        setNavigationCountry(selectedCountry);
        setNavigationPublication(selectedCountry, selectedPublication);
    }

    protected void show() {
        if (WhatTheDuck.getSelectedCountry() != null && WhatTheDuck.getSelectedPublication() != null) {
            super.show(new IssueAdapter(this, getCollection().getIssueList(
                WhatTheDuck.getSelectedCountry(),
                WhatTheDuck.getSelectedPublication()
            )));
        }
    }
    
    @Override
    public void onBackPressed() {
        Intent i = new Intent(WhatTheDuck.wtd, PublicationList.class);
        i.putExtra("type", type);
        startActivity(i);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, final long id) {
        if (type.equals(CollectionType.COA.toString())) {
            final Issue selectedIssue = (Issue) this.getListView().getItemAtPosition(((Long) id).intValue());
            if (WhatTheDuck.userCollection.getIssue(WhatTheDuck.getSelectedCountry(), WhatTheDuck.getSelectedPublication(), selectedIssue.getIssueNumber()) != null) {
                WhatTheDuck.wtd.info(new WeakReference<>(this), R.string.input_error__issue_already_possessed);
            }
            else {
                WhatTheDuck.setSelectedIssue(selectedIssue.getIssueNumber());
                GetPurchaseList.initAndShowAddIssue(IssueList.this);
            }
        }
        super.onListItemClick(l, v, position, id);
    }
}
