package com.google.android.gms.drive.sample.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HomeActivity extends Activity {

  private ListView mListViewSamples;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    String[] titles = getResources().getStringArray(R.array.titles_array);
    mListViewSamples = (ListView) findViewById(R.id.listViewSamples);
    mListViewSamples.setAdapter(
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles));
    mListViewSamples.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int i, long arg3) {
      }});

  }

}
