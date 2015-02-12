package com.mayday.md;

import android.app.Application;
import android.content.SharedPreferences;


import com.mayday.md.common.ApplicationSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPreferenceManager;

import static junit.framework.Assert.assertNull;
import static com.mayday.md.common.ApplicationSettings.isFirstRun;
import static com.mayday.md.common.ApplicationSettings.setFirstRun;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ApplicationSettingsTest {

    private SharedPreferences sharedPreferences;
    private Application context;

    @Before
    public void setUp() {
        context = Robolectric.application;
        sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(context);
    }

    @Test
    public void shouldReturnTrueWhenOnFirstRun() {
        assertTrue(isFirstRun(context));
    }

    @Test
    public void shouldReturnNullIfThereIsNoLocationStored() {
        assertNull(ApplicationSettings.getCurrentBestLocation(context));
    }

    @Test
    public void shouldUpdateFirstRunFlagOnCompletion() {
        setFirstRun(context, false);
        assertFalse(sharedPreferences.getBoolean("FIRST_RUN", true));
    }

    @Test
    public void shouldReturnFalseWhenFirstRunIsCompleted() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("FIRST_RUN", false);
        editor.commit();
        assertFalse(isFirstRun(context));
    }
}
