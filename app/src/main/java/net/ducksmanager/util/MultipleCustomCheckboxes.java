package net.ducksmanager.util;

import android.view.View;
import android.view.ViewGroup;

import net.ducksmanager.whattheduck.R;
import net.igenius.customcheckbox.CustomCheckBox;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class MultipleCustomCheckboxes {

    private final CustomCheckBox.OnCheckedChangeListener onCheckedListener = new CustomCheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CustomCheckBox checkBox, boolean isChecked) {
            if (checkBox.getTag(R.id.check_by_user) != null) {
                checkBox.setTag(R.id.check_by_user, null);
            }
            else {
                if (isChecked) {
                    MultipleCustomCheckboxes.this.initClickEvents();
                    for (CustomCheckBox otherCheckbox : checkboxList) {
                        if (!checkBox.equals(otherCheckbox)) {
                            otherCheckbox.setTag(R.id.direct_uncheck, Boolean.FALSE);
                            otherCheckbox.setChecked(false);
                        }
                    }
                    customOnChecked.onClick(checkBox);
                } else {
                    if (checkBox.getTag(R.id.direct_uncheck) != null
                        && checkBox.getTag(R.id.direct_uncheck).equals(Boolean.FALSE)) {
                        checkBox.setTag(R.id.direct_uncheck, null);
                    } else {
                        customOnUnchecked.onClick(checkBox);
                        checkBox.setChecked(true);
                    }
                }
            }
        }
    };

    public interface CheckboxFilter {
        boolean isMatched(CustomCheckBox checkbox);
    }

    private final WeakReference<View> container;
    private final View.OnClickListener customOnChecked;
    private final View.OnClickListener customOnUnchecked;

    private final Set<CustomCheckBox> checkboxList = new HashSet<>();

    public MultipleCustomCheckboxes(WeakReference<View> container, View.OnClickListener customOnChecked, View.OnClickListener customOnUnchecked) {
        this.container = container;
        this.customOnChecked = customOnChecked;
        this.customOnUnchecked = customOnUnchecked;
    }

    private void storeDescendantsOfType(ViewGroup v, Class type) {
        for (int i = 0; i < v.getChildCount(); i++) {
            this.storeOrStoreDescendantsOfType(v.getChildAt(i), type);
        }
    }

    private void storeOrStoreDescendantsOfType(Object item, Class type) {
        if (item instanceof CustomCheckBox) {
            this.checkboxList.add((CustomCheckBox) item);
        } else if (item instanceof ViewGroup) {
            storeDescendantsOfType((ViewGroup) item, type);
        }
    }

    public void checkInitialCheckbox(CheckboxFilter filter) {
        CustomCheckBox checkboxToCheck = null;
        for (CustomCheckBox checkbox : checkboxList) {
            if (filter.isMatched(checkbox)) {
                checkboxToCheck = checkbox; // Don't call setChecked() here to avoir concurrent access in initClickEvents() called in the OnCheckedChangeListener
            }
        }
        if (checkboxToCheck != null) {
            checkboxToCheck.setChecked(true);
        }
    }

    public void initClickEvents() {
        this.storeDescendantsOfType((ViewGroup) container.get(), CustomCheckBox.class);

        for (CustomCheckBox checkBox : checkboxList) {
            checkBox.setTag(R.id.direct_uncheck, null);
            checkBox.setOnCheckedChangeListener(onCheckedListener);
        }

    }
}
