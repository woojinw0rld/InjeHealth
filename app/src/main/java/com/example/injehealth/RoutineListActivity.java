package com.example.injehealth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class RoutineListActivity extends AppCompatActivity {

    private static final List<String> BODY_PARTS = List.of("가슴", "등", "하체", "어깨", "팔", "유산소");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        viewPager.setAdapter(new BodyPartPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(BODY_PARTS.get(position))
        ).attach();
    }

    private static class BodyPartPagerAdapter extends FragmentStateAdapter {


        BodyPartPagerAdapter(FragmentActivity fa) { super(fa); }

        @Override
        public Fragment createFragment(int position) {
            return RoutineTabFragment.newInstance(BODY_PARTS.get(position));
        }

        @Override
        public int getItemCount() { return BODY_PARTS.size(); }
    }
}
