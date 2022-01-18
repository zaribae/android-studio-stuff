package com.example.fragmentintroduction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DetailFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.from(getContext()).inflate(R.layout.detail_fragment_layout, container, false);

        TextView versionName = view.findViewById(R.id.version_name);

        if (getArguments() != null && getArguments().getString("VersionName") != null) {
            versionName.setText(getArguments().getString("VersionName"));
        }

        return view;
    }
}
