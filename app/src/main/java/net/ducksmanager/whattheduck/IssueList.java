package net.ducksmanager.whattheduck;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import net.ducksmanager.apigateway.DmServer;
import net.ducksmanager.persistence.models.coa.InducksIssue;
import net.ducksmanager.persistence.models.composite.InducksIssueWithUserIssueDetails;
import net.ducksmanager.util.DraggableRelativeLayout;
import net.ducksmanager.util.Settings;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Response;

import static net.ducksmanager.whattheduck.WhatTheDuckApplication.CollectionType;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.appDB;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.info;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.isMobileConnection;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.selectedCountry;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.selectedPublication;
import static net.ducksmanager.whattheduck.WhatTheDuckApplication.trackEvent;

public class IssueList extends ItemList<InducksIssueWithUserIssueDetails> {

    public enum ViewType {
        LIST_VIEW,
        EDGE_VIEW,
    }

    public static ViewType viewType = ViewType.LIST_VIEW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        show();
    }

    @Override
    protected boolean hasList() {
        return false; // FIXME
    }

    @Override
    protected void downloadList(Activity currentActivity) {
        DmServer.api.getIssues(selectedPublication).enqueue(new DmServer.Callback<List<String>>("getInducksIssues", currentActivity) {
            @Override
            public void onSuccessfulResponse(Response<List<String>> response) {
                List<InducksIssue> issues = new ArrayList<>();
                for(String issueNumber : response.body()) {
                    issues.add(new InducksIssue(selectedPublication, issueNumber));
                }
                appDB.inducksIssueDao().insertList(issues);
                setData();
            }
        });
    }

    @Override
    protected boolean hasDividers() {
        return !viewType.equals(ViewType.EDGE_VIEW);
    }

    @Override
    protected boolean shouldShow() {
        return selectedCountry != null && selectedPublication != null;
    }

    @Override
    protected boolean shouldShowNavigationCountry() {
        return !isLandscapeEdgeView();
    }

    @Override
    protected boolean shouldShowNavigationPublication() {
        return !isLandscapeEdgeView();
    }

    @Override
    protected boolean shouldShowToolbar() {
        return !isLandscapeEdgeView();
    }

    @Override
    protected boolean shouldShowAddToCollectionButton() {
        return !isLandscapeEdgeView();
    }

    @Override
    protected boolean shouldShowFilter(List<InducksIssueWithUserIssueDetails> issues) {
        return issues.size() > MIN_ITEM_NUMBER_FOR_FILTER && viewType.equals(ViewType.LIST_VIEW);
    }

    @Override
    protected ItemAdapter<InducksIssueWithUserIssueDetails> getItemAdapter() {
        RelativeLayout switchViewWrapper = this.findViewById(R.id.switchViewWrapper);
        DraggableRelativeLayout.makeDraggable(switchViewWrapper);

        Switch switchView = switchViewWrapper.findViewById(R.id.switchView);

        if (type.equals(CollectionType.COA.toString())) {
            viewType = ViewType.LIST_VIEW;
            switchViewWrapper.setVisibility(View.GONE);
        }
        else {
            switchViewWrapper.setVisibility(View.VISIBLE);
            switchView.setChecked(viewType.equals(ViewType.EDGE_VIEW));
            switchView.setOnClickListener(view -> {
                if (switchView.isChecked()) {
                    if (Settings.shouldShowMessage(Settings.MESSAGE_KEY_DATA_CONSUMPTION) && isMobileConnection()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(IssueList.this);
                        builder.setTitle(getString(R.string.bookcaseViewTitle));
                        builder.setMessage(getString(R.string.bookcaseViewMessage));
                        builder.setNegativeButton(R.string.cancel, (dialogInterface, which) -> {
                            switchView.toggle();
                            dialogInterface.dismiss();
                        });
                        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                            Settings.addToMessagesAlreadyShown(Settings.MESSAGE_KEY_DATA_CONSUMPTION);
                            dialogInterface.dismiss();
                            switchBetweenViews();
                        });
                        builder.create().show();
                    }
                    else {
                        switchBetweenViews();
                    }
                }
                else {
                    switchBetweenViews();
                }
            });
        }

        RecyclerView recyclerView = this.findViewById(R.id.itemList);

        if (viewType.equals(ViewType.EDGE_VIEW)) {
            int deviceOrientation = getResources().getConfiguration().orientation;
            int listOrientation = deviceOrientation == Configuration.ORIENTATION_LANDSCAPE
                ? RecyclerView.HORIZONTAL
                : RecyclerView.VERTICAL;

            if (Settings.shouldShowMessage(Settings.MESSAGE_KEY_WELCOME_BOOKCASE_VIEW)
             && listOrientation == RecyclerView.VERTICAL) {
                info(new WeakReference<>(this), R.string.welcomeBookcaseViewPortrait, Toast.LENGTH_LONG);
                Settings.addToMessagesAlreadyShown(Settings.MESSAGE_KEY_WELCOME_BOOKCASE_VIEW);
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(this, listOrientation, false));

            return new IssueEdgeAdapter(this, data, recyclerView, deviceOrientation);
        }
        else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
            return new IssueAdapter(this, data);
        }
    }

    private boolean isLandscapeEdgeView() {
        int deviceOrientation = getResources().getConfiguration().orientation;
        return viewType.equals(ViewType.EDGE_VIEW) && deviceOrientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void switchBetweenViews() {
        trackEvent("issuelist/switchview");
        viewType = ((Switch)this.findViewById(R.id.switchView)).isChecked() ? ViewType.EDGE_VIEW : ViewType.LIST_VIEW;
        loadList();
        show();
    }

    @Override
    protected boolean isPossessedByUser() {
        for (InducksIssueWithUserIssueDetails i: data) {
            if (i.getUserIssue() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void setData() {
        appDB.inducksIssueDao().findByPublicationCode(selectedPublication).observe(this, this::storeItemList);
    }

    @Override
    public void onBackPressed() {
        if (type.equals(CollectionType.COA.toString())) {
            onBackFromAddIssueActivity();
        }
        else {
            startActivity(new Intent(this, PublicationList.class));
        }
    }
}
