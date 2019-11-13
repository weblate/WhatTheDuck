import android.content.Intent;

import net.ducksmanager.whattheduck.AddIssue;
import net.ducksmanager.whattheduck.CountryList;
import net.ducksmanager.whattheduck.IssueList;
import net.ducksmanager.whattheduck.PublicationList;
import net.ducksmanager.whattheduck.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class AddIssueTest extends WtdTest {

    public AddIssueTest() {
        super();
    }


    @Before
    public void login() {
        loginActivityRule.launchActivity(new Intent());
        login(DownloadHandlerMock.TEST_USER, DownloadHandlerMock.TEST_PASS);
    }


    @Test
    public void testShowAddIssueDialog()  {
        assertCurrentActivityIsInstanceOf(CountryList.class, true);
        onView(withText("France")).perform(ViewActions.click());

        assertCurrentActivityIsInstanceOf(PublicationList.class, true);
        onView(withText(containsString("dynastie"))).perform(ViewActions.click());

        assertCurrentActivityIsInstanceOf(IssueList.class, true);
        onView(withId(R.id.addToCollectionBySelectionButton))
            .perform(forceFloatingActionButtonsVisible(true))
            .perform(click());

        onView(withText("6")).perform(ViewActions.click());

        assertCurrentActivityIsInstanceOf(AddIssue.class, true);

        onView(withId(R.id.addpurchase)).perform(click());

        onView(withId(R.id.purchasetitlenew)).perform(typeText("Stockholm loppis"), closeSoftKeyboard());
        onView(withId(R.id.createpurchase)).perform(click());

        onView(allOf(withText("Stockholm loppis"), withParent(withId(R.id.purchase_list))));

    }
}
