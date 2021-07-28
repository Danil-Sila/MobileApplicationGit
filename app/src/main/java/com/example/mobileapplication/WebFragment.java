package com.example.mobileapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WebFragment extends Fragment {

    WebView webView;

    //функция для получения данных из activity
    public static WebFragment getNewInstance(String someString){
        WebFragment wf = new WebFragment();
        Bundle args = new Bundle();
        args.putString("mobileurl",someString);
        wf.setArguments(args);
        return wf;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web, container, false);
        webView = (WebView) view.findViewById(R.id.webContent);
        webView.getSettings().setJavaScriptEnabled(true);
        String url = getArguments().getString("mobileurl");
        webView.loadUrl(url);
        return view;
    }
}