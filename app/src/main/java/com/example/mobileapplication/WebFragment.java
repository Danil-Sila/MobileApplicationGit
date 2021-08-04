package com.example.mobileapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class WebFragment extends Fragment implements View.OnClickListener {

    //функция для получения данных из activity
    public static WebFragment getNewInstance(String mobileURL){
        WebFragment webFragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString("mobile_url",mobileURL);
        webFragment.setArguments(args);
        return webFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web, container, false);
        WebView webView = (WebView) view.findViewById(R.id.webContent);
        Button btnReturn = (Button) view.findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(this);
        webView.getSettings().setJavaScriptEnabled(true);
        String url = getArguments().getString("mobile_url");
        webView.loadUrl(url);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnReturn:
                getActivity().onBackPressed();
                break;
        }
    }
}