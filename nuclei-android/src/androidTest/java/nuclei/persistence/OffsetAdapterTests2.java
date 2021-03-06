/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.persistence;

import android.support.annotation.NonNull;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import io.nuclei.test.R;
import nuclei.ui.OffsetTestActivity;
import nuclei.ui.OffsetTestActivity2;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class OffsetAdapterTests2 extends ActivityInstrumentationTestCase2<OffsetTestActivity2> {

    public OffsetAdapterTests2() {
        super(OffsetTestActivity2.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testOffsetTests() {
        onView(withId(R.id.items))
                .check(matches(atPosition(0, withText(Integer.toString(Integer.MAX_VALUE - 1)))));
        onView(withId(R.id.items))
                .check(matches(atPosition(1, withText(Integer.toString(Integer.MAX_VALUE - 2)))));
        onView(withId(R.id.items))
                .check(matches(atPosition(2, withText(Integer.toString(Integer.MAX_VALUE - 3)))));

        for (int i = 0; i < 103; i++) {
            int v = i;

            if (i == 0) {
                v = Integer.MAX_VALUE - 1;
            } else if (v == 1) {
                v = Integer.MAX_VALUE - 2;
            } else if (v == 2) {
                v = Integer.MAX_VALUE - 3;
            } else {
                v -= 3;
            }

            try {
                onView(withId(R.id.items))
                        .perform(scrollToPosition(i));
                onView(withId(R.id.items))
                        .check(matches(atPosition(i, withText(Integer.toString(v)))));
            } catch (Throwable err) {
                err.printStackTrace();;
            }
        }
    }

    public static Matcher<View> atPosition(final int position, @NonNull final Matcher<View> itemMatcher) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("adapter position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView);
            }
        };
    }

    public static ViewAction scrollToPosition(final int position) {
        return new ViewAction() {
            @SuppressWarnings("unchecked")
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((RecyclerView) view).scrollToPosition(position);
            }

            @Override
            public String getDescription() {
                return "scroll to";
            }
        };
    }

}