package com.example.injehealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.injehealth.db.AppDatabase;

import java.util.concurrent.Executors;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.nav_today).setOnClickListener(v ->
                switchToTab(R.id.tab_home));

        view.findViewById(R.id.nav_history).setOnClickListener(v ->
                switchToTab(R.id.tab_history));

        view.findViewById(R.id.nav_myinbody).setOnClickListener(v ->
                switchToTab(R.id.tab_myinbody));

        view.findViewById(R.id.nav_settings).setOnClickListener(v ->
                Toast.makeText(requireContext(), "설정 (준비중)", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.nav_withdraw).setOnClickListener(v ->
                showWithdrawDialog());
    }

    private void switchToTab(int tabId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).switchToTab(tabId);
        }
    }

    private void showWithdrawDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("모든 데이터가 삭제됩니다. 정말 탈퇴하시겠습니까?")
                .setPositiveButton("탈퇴", (d, w) -> withdrawAccount())
                .setNegativeButton("취소", null)
                .show();
    }

    private void withdrawAccount() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).clearAllTables();
            requireActivity().runOnUiThread(() -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });
    }
}
