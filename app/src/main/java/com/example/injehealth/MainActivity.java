package com.example.injehealth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private MyinbodyFragment myinbodyFragment;
    private MenuFragment menuFragment;
    private ExerciseCatalogFragment exerciseFragment;

    private HistoryListActivity historyListActivity;

    private HistoryDetailActivity historyDetailActivity;

    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFragments();
        setupBottomNav();
    }

    private void setupFragments() {
        homeFragment     = new HomeFragment();
        historyFragment  = new HistoryFragment();
        myinbodyFragment = new MyinbodyFragment();
        menuFragment     = new MenuFragment();
        exerciseFragment = new ExerciseCatalogFragment();
        historyListActivity = new HistoryListActivity();
        historyDetailActivity = new HistoryDetailActivity();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, exerciseFragment).hide(exerciseFragment)
                .add(R.id.fragment_container, menuFragment).hide(menuFragment)
                .add(R.id.fragment_container, myinbodyFragment).hide(myinbodyFragment)
                .add(R.id.fragment_container, historyFragment).hide(historyFragment)
                .add(R.id.fragment_container, homeFragment)
                .commit();

        activeFragment = homeFragment;
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = fragmentForId(item.getItemId());
            if (selected != null && selected != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_NONE)
                        .hide(activeFragment)
                        .show(selected)
                        .commit();
                activeFragment = selected;
            }
            return true;
        });
    }

    private Fragment fragmentForId(int id) {
        if (id == R.id.tab_home)     return homeFragment;
        if (id == R.id.tab_history)  return historyFragment;
        if (id == R.id.tab_myinbody) return myinbodyFragment;
        if (id == R.id.tab_exercise) return exerciseFragment;
        if (id == R.id.tab_menu)     return menuFragment;
        return null;
    }

    public void switchToTab(int tabId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(tabId);
    }
}
