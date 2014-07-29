package net.ducksmanager.inducks.coa;

import net.ducksmanager.whattheduck.Issue;
import net.ducksmanager.whattheduck.IssueList;
import net.ducksmanager.whattheduck.List;
import net.ducksmanager.whattheduck.WhatTheDuck;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IssueListing extends CoaListing {

    public IssueListing(List list, int progressBarId, String countryShortName, String publicationShortName) {
        super(list, ListType.ISSUE_LIST, progressBarId, countryShortName, publicationShortName);
        this.urlSuffix+="&pays="+countryShortName+"&magazine="+publicationShortName;
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        if (response != null) {
            try {
                String shortCountryName = this.countryShortName;
                String shortPublicationName = this.publicationShortName;

                JSONObject object = new JSONObject(response);
                JSONArray issues = object.getJSONObject("static").getJSONArray("numeros");
                for (int i = 0; i < issues.length(); i++) {
                    String issue = (String) issues.get(i);
                    WhatTheDuck.coaCollection.addIssue(shortCountryName, shortPublicationName, new Issue(issue, Boolean.FALSE, Issue.NO_CONDITION));
                }
            }
            catch (JSONException e) {
                handleJSONException(e);
            }
        }

        displayedList.show();
    }
}
