package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.StrictMode;
import android.security.NetworkSecurityPolicy;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AndroidBoundarySmokeTest {
    @Test
    public void collapsedHistoryRowDefersAndExpansionPayloadBindsDetails() {
        Context context = ApplicationProvider.getApplicationContext();
        User user = user("history-expansion-user");
        user.birthdate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365 * 30L);
        Weight weight = new Weight();
        weight.uuid = user.uuid;
        weight.date = System.currentTimeMillis();
        weight.weight = 75;
        weight.height = 180;
        weight.boneMass = 3.2;
        HistoryAdapter adapter = new HistoryAdapter(
                new ArrayList<>(Collections.singletonList(weight)), context, user,
                new HistoryFragment());
        FrameLayout parent = new FrameLayout(context);
        HistoryAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        holder.boneMassTV.setText("detail-not-bound");

        adapter.onBindViewHolder(holder, 0);

        assertEquals(View.GONE, holder.detailsContainer.getVisibility());
        assertEquals("detail-not-bound", holder.boneMassTV.getText().toString());
        holder.weightTV.setText("header-not-rebound");

        adapter.toggleExpanded(0);
        adapter.onBindViewHolder(holder, 0,
                Collections.singletonList(HistoryAdapter.EXPANSION_PAYLOAD));

        assertEquals(View.VISIBLE, holder.detailsContainer.getVisibility());
        assertEquals("header-not-rebound", holder.weightTV.getText().toString());
        assertFalse("detail-not-bound".contentEquals(holder.boneMassTV.getText()));
        assertFalse(holder.boneMassTV.getText().toString().isEmpty());

        adapter.toggleExpanded(0);
        adapter.onBindViewHolder(holder, 0,
                Collections.singletonList(HistoryAdapter.EXPANSION_PAYLOAD));
        assertEquals(View.GONE, holder.detailsContainer.getVisibility());

        Weight replacement = new Weight();
        replacement.uuid = user.uuid;
        replacement.date = weight.date + 1;
        replacement.weight = 76;
        replacement.height = 180;
        replacement.boneMass = 4.1;
        adapter.replaceAll(new ArrayList<>(Collections.singletonList(replacement)), user);
        holder.boneMassTV.setText("recycled-detail-not-bound");
        adapter.onBindViewHolder(holder, 0);
        assertEquals(View.GONE, holder.detailsContainer.getVisibility());
        assertEquals("recycled-detail-not-bound", holder.boneMassTV.getText().toString());

        adapter.toggleExpanded(0);
        adapter.onBindViewHolder(holder, 0,
                Collections.singletonList(HistoryAdapter.EXPANSION_PAYLOAD));
        assertEquals(View.VISIBLE, holder.detailsContainer.getVisibility());
        assertFalse("recycled-detail-not-bound".contentEquals(holder.boneMassTV.getText()));
    }

    @Test
    public void weightScreenShowsMuscleOnlySegmentData() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppRepository repository = AppRepository.get(context);
        assertTrue(repository.reloadState().isSuccess());
        User user = user("muscle-only-segment-user");
        user.height_cm = 180;
        Weight weight = new Weight();
        weight.uuid = user.uuid;
        weight.date = System.currentTimeMillis();
        weight.weight = 75;
        weight.height = user.height_cm;
        weight.leftArmMuscleMass = 3.2;
        awaitMutation(callback -> repository.upsertUser(user, callback));
        awaitMutation(callback -> repository.upsertWeight(weight, null, callback));
        repository.selectUser(user.uuid);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getSupportFragmentManager().executePendingTransactions();
                Fragment fragment = activity.getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);
                assertTrue(fragment instanceof WeightFragment);
                ((WeightFragment) fragment).updateUi();

                View segmentSection = activity.findViewById(R.id.cardSegmentalContainer);
                View leftArm = activity.findViewById(R.id.segLeftArm);
                TextView value = leftArm.findViewById(R.id.metricValue);
                assertEquals(View.VISIBLE, segmentSection.getVisibility());
                assertFalse(value.getText().toString().isEmpty());
                assertFalse("-".contentEquals(value.getText()));
            });
        }
    }

    @Test
    public void retainedAntControllerIsExposedOnlyForItsSelectedProfile() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppRepository repository = AppRepository.get(context);
        assertTrue(repository.reloadState().isSuccess());
        User firstUser = user("ant-controller-first");
        User secondUser = user("ant-controller-second");
        awaitMutation(callback -> repository.upsertUser(firstUser, callback));
        awaitMutation(callback -> repository.upsertUser(secondUser, callback));

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                AppStateViewModel state = new ViewModelProvider(activity)
                        .get(AppStateViewModel.class);
                state.selectUser(firstUser);
                AntWeightController controller = state.newAntWeightController(null);
                controller.setProfile(firstUser);
                controller.weight.uuid = firstUser.uuid;
                controller.weight.weight = 75;

                assertSame(controller, state.selectedAntWeightController());

                state.selectUser(secondUser);
                assertNull(state.selectedAntWeightController());

                state.selectUser(firstUser);
                assertSame(controller, state.selectedAntWeightController());
            });
        }
    }

    @Test
    public void mergedManifestKeepsUsbHostOptionalAndCleartextDisabled() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        boolean foundUsbHost = false;

        assertNotNull(packageInfo.reqFeatures);
        for (FeatureInfo feature : packageInfo.reqFeatures) {
            assertFalse("USB accessory mode must not be required",
                    PackageManager.FEATURE_USB_ACCESSORY.equals(feature.name));
            if (PackageManager.FEATURE_USB_HOST.equals(feature.name)) {
                foundUsbHost = true;
                assertEquals("USB host support must remain optional", 0,
                        feature.flags & FeatureInfo.FLAG_REQUIRED);
            }
        }

        assertTrue("Optional USB host capability is missing", foundUsbHost);
        assertFalse("Cleartext traffic must remain disabled",
                NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted());
    }

    @Test
    public void cancellingTheWeightEditorDiscardsItsWorkingCopy() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppRepository repository = AppRepository.get(context);
        assertTrue(repository.reloadState().isSuccess());
        User user = user("editor-user");
        user.birthdate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365 * 30L);
        user.height_cm = 180;
        user.usesCm = true;
        Weight persisted = new Weight();
        persisted.uuid = user.uuid;
        persisted.date = System.currentTimeMillis();
        persisted.weight = 75;
        persisted.height = user.height_cm;
        persisted.age = 30;

        awaitMutation(callback -> repository.upsertUser(user, callback));
        awaitMutation(callback -> repository.upsertWeight(persisted, null, callback));

        long editedDate = persisted.date + TimeUnit.DAYS.toMillis(1);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.openEditWeightFragment(persisted, user, true);
                activity.getSupportFragmentManager().executePendingTransactions();
                Fragment current = activity.getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);
                assertTrue(current instanceof EditWeightFragment);
                workingWeight((EditWeightFragment) current).date = editedDate;

                activity.getOnBackPressedDispatcher().onBackPressed();
                activity.getSupportFragmentManager().executePendingTransactions();
            });
        }

        assertNotNull(repository.findWeight(user.uuid, persisted.date));
        assertNull(repository.findWeight(user.uuid, editedDate));
    }

    @Test
    public void mainActivityKeepsAValidDestinationAcrossRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertCurrentDestinationExists(scenario);

            scenario.recreate();

            assertCurrentDestinationExists(scenario);
        }
    }

    @Test
    public void goalRowRebindsWhenTheSelectedUsersLatestWeightChanges() {
        Context context = ApplicationProvider.getApplicationContext();
        User firstUser = user("first-user");
        User secondUser = user("second-user");
        Goal goal = goal(firstUser.uuid);
        ArrayList<Goal> goals = new ArrayList<>(Collections.singletonList(goal));
        GoalAdapter adapter = new GoalAdapter(
                goals, context, firstUser, null, new GoalsFragment());
        FrameLayout parent = new FrameLayout(context);
        GoalAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        assertEquals(context.getString(R.string.goal_progress_unavailable),
                holder.totalProgressTV.getText().toString());

        Weight latest = new Weight();
        latest.uuid = secondUser.uuid;
        latest.date = System.currentTimeMillis();
        latest.weight = 80;
        goal.uuid = secondUser.uuid;
        adapter.replaceAll(goals, secondUser, latest);
        adapter.onBindViewHolder(holder, 0);

        String reboundProgress = holder.totalProgressTV.getText().toString();
        assertFalse(reboundProgress.equals(context.getString(R.string.goal_progress_unavailable)));
        assertTrue(reboundProgress.contains("-10"));
    }

    @Test
    public void notificationListenerPublishesOnlyAFreshGarminMfaCode() {
        Context context = ApplicationProvider.getApplicationContext();
        Notification unrelated = new NotificationCompat.Builder(context, "mfa-test")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText("Bank verification code: 111111")
                .build();
        Notification garmin = new NotificationCompat.Builder(context, "mfa-test")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentText("Garmin verification code: 123456")
                .build();
        AtomicReference<String> observed = new AtomicReference<>();
        long requestStartedAt = System.currentTimeMillis();
        NotificationRepository.MfaRequest request = NotificationRepository.getInstance()
                .registerMfaRequest(requestStartedAt, observed::set);
        NotificationListener listener = new NotificationListener();

        try {
            listener.processNotification(unrelated, requestStartedAt + 1);
            assertNull(observed.get());

            listener.processNotification(garmin, requestStartedAt - 1);
            assertNull(observed.get());

            listener.processNotification(garmin, requestStartedAt + 1);
            assertEquals("123456", observed.get());

            listener.processNotification(new NotificationCompat.Builder(context, "mfa-test")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText("Garmin verification code: 654321")
                    .build(), requestStartedAt + 2);
            assertEquals("123456", observed.get());
        } finally {
            request.close();
        }
    }

    @Test
    public void csvProviderWorkIsNotStartedOnTheMainThread() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Uri destination = Uri.parse("content://invalid.test/tree/root");
            scenario.onActivity(activity -> {
                AppStateViewModel state = new ViewModelProvider(activity)
                        .get(AppStateViewModel.class);
                StrictMode.ThreadPolicy original = StrictMode.getThreadPolicy();
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(original)
                        .detectDiskReads()
                        .detectDiskWrites()
                        .penaltyDeath()
                        .build());
                try {
                    state.exportCsv(activity.getContentResolver(), destination, "history.csv",
                            user("strict-mode-user"), Collections.emptyList());
                } finally {
                    StrictMode.setThreadPolicy(original);
                }
            });
        }
    }

    private static void assertCurrentDestinationExists(
            ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity -> {
            activity.getSupportFragmentManager().executePendingTransactions();
            Fragment fragment = activity.getSupportFragmentManager()
                    .findFragmentById(R.id.content_frame);
            assertNotNull(fragment);
            assertNotNull(NavigationDestination.forFragment(fragment));
        });
    }

    private static User user(String uuid) {
        User user = new User();
        user.uuid = uuid;
        user.name = uuid;
        user.mass_unit = User.MassUnit.KG;
        return user;
    }

    private static Goal goal(String uuid) {
        Goal goal = new Goal();
        goal.uuid = uuid;
        goal.start_date = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        goal.end_date = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        goal.start_value = 90;
        goal.end_value = 70;
        goal.type = Metric.WEIGHT;
        goal.color = Color.BLUE;
        return goal;
    }

    private static Weight workingWeight(EditWeightFragment fragment) {
        try {
            Field field = EditWeightFragment.class.getDeclaredField("the_weight");
            field.setAccessible(true);
            return (Weight) field.get(fragment);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inspect the editor working copy", exception);
        }
    }

    private static void awaitMutation(MutationStarter starter) throws Exception {
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<RepositoryResult<Void>> result = new AtomicReference<>();
        starter.start(value -> {
            result.set(value);
            complete.countDown();
        });
        assertTrue("Repository mutation timed out", complete.await(5, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertTrue(result.get().message, result.get().isSuccess());
    }

    private interface MutationStarter {
        void start(AppRepository.MutationCallback callback);
    }
}
