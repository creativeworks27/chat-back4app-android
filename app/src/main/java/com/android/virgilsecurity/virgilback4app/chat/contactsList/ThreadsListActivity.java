package com.android.virgilsecurity.virgilback4app.chat.contactsList;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.android.virgilsecurity.virgilback4app.R;
import com.android.virgilsecurity.virgilback4app.auth.SignInControlActivity;
import com.android.virgilsecurity.virgilback4app.base.BaseActivityWithPresenter;
import com.android.virgilsecurity.virgilback4app.chat.thread.ChatThreadActivity;
import com.android.virgilsecurity.virgilback4app.model.ChatThread;
import com.android.virgilsecurity.virgilback4app.util.Const;
import com.android.virgilsecurity.virgilback4app.util.Utils;
import com.android.virgilsecurity.virgilback4app.util.customElements.CreateThreadDialog;
import com.parse.ParseUser;

import java.util.List;

import butterknife.BindView;
import nucleus5.factory.RequiresPresenter;

/**
 * Created by Danylo Oliinyk on 11/22/17 at Virgil Security.
 * -__o
 */

// TODO: 11/27/17 add double back exit

@RequiresPresenter(ThreadsListActivityPresenter.class)
public class ThreadsListActivity extends BaseActivityWithPresenter<ThreadsListActivityPresenter>
        implements ThreadsListFragment.OnStartThreadListener {

    private static final String THREADS_FRAGMENT = "THREADS_FRAGMENT";

    private ActionBarDrawerToggle mDrawerToggle;
    private CreateThreadDialog createThreadDialog;
    private ParseUser userNewThread;
//    private boolean threadCreated;

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.nvNavigation)
    protected NavigationView nvNavigation;
    @BindView(R.id.dlDrawer)
    protected DrawerLayout dlDrawer;

    public static void start(AppCompatActivity from) {
        from.startActivity(new Intent(from, ThreadsListActivity.class));
    }

    public static void startWithFinish(AppCompatActivity from) {
        from.startActivity(new Intent(from, ThreadsListActivity.class));
        from.finish();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_contacts;
    }

    @Override
    protected void postButterInit() {
        initToolbar(toolbar, getString(R.string.contacts));
        initDrawer();
        Utils.replaceFragmentNoBackStack(getSupportFragmentManager(),
                                         R.id.flContainer,
                                         ThreadsListFragment.newInstance(),
                                         THREADS_FRAGMENT);
    }

    private void initDrawer() {

        TextView tvUsernameDrawer =
                nvNavigation.getHeaderView(0).findViewById(R.id.tvUsernameDrawer);
        tvUsernameDrawer.setText(ParseUser.getCurrentUser().getUsername());

        mDrawerToggle = new ActionBarDrawerToggle(this, dlDrawer,
                                                  R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                getActionBar().setTitle(mTitle);
//                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                getActionBar().setTitle(mDrawerTitle);
//                invalidateOptionsMenu();
            }
        };

        dlDrawer.addDrawerListener(mDrawerToggle);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        nvNavigation.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.itemNewChat:
                    createThreadDialog =
                            new CreateThreadDialog(this, R.style.NotTransBtnsDialogTheme,
                                                   getString(R.string.create_thread),
                                                   getString(R.string.enter_username));

                    createThreadDialog.setOnCreateThreadDialogListener((username -> {
                        getPresenter().requestUser(username);
                    }));

                    createThreadDialog.show();

                    return true;
                case R.id.itemLogOut:
                    ParseUser.logOutInBackground(e -> {
                        if (e == null) {
                            SignInControlActivity.startClearTop(this);
                        } else {
                            Utils.toast(this, Utils.resolveError(e));
                        }
                    });
                    return true;
                default:
                    return false;
            }
        });
    }

    @Override public void onStartThread(ChatThread thread) {
        ChatThreadActivity.start(this, thread);
    }

    public void onGetUserSuccess(ParseUser user) {
        if (user != null) {
            userNewThread = user;
            getPresenter().requestThreads(ParseUser.getCurrentUser(),
                                          1000,
                                          0,
                                          Const.TableNames.CREATED_AT_CRITERIA); // TODO: 11/27/17 add pagination
        } else {
            createThreadDialog.dismiss();
        }
    }

    public void onGetUserError(Throwable t) {
        createThreadDialog.showLoading(false);
        Utils.toast(this, Utils.resolveError(t));
    }

    public void onGetThreadsSuccess(@NonNull List<ChatThread> threads) {
//        if (threadCreated) {
//            createThreadDialog.dismiss();
//            return;
//        }

        boolean threadExists = false;
        ChatThread chatThread = null;

        for (ChatThread thread : threads) {
            if (thread.getSenderUsername().equals(userNewThread.getUsername())
                    || thread.getRecipientUsername().equals(userNewThread.getUsername())) {
                threadExists = true;
                chatThread = thread;
            }
        }

        if (!threadExists) {
            getPresenter().requestCreateThread(ParseUser.getCurrentUser(), userNewThread);
        } else {
            createThreadDialog.dismiss();
            ChatThreadActivity.start(this, chatThread);
        }
    }

    public void onGetThreadsError(Throwable t) {
        createThreadDialog.dismiss();
        Utils.toast(this, Utils.resolveError(t));
    }

    public void onCreateThreadSuccess(Object o) {
        getPresenter().requestThreads(ParseUser.getCurrentUser(),
                                      1000,
                                      0,
                                      Const.TableNames.CREATED_AT_CRITERIA); // TODO: 11/27/17 add pagination
//        threadCreated = true; // for no recursion
    }

    public void onCreateThreadError(Throwable t) {
        createThreadDialog.dismiss();
        Utils.toast(this, Utils.resolveError(t));
    }
}