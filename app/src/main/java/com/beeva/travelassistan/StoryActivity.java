package com.beeva.travelassistan;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import com.mapbox.geocoder.MapboxGeocoder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.BindInt;
import butterknife.ButterKnife;
import jp.wasabeef.richeditor.RichEditor;

public class StoryActivity extends AppCompatActivity {

    @Bind(R.id.textViewTitle) TextView title;
    @Bind(R.id.textViewAuthor) TextView author;
    @Bind(R.id.textViewPlace) TextView place;
    @Bind(R.id.editor) RichEditor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);
        ButterKnife.bind(this);

        getSupportActionBar().setElevation(0);
        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        editor.setBackgroundColor(Color.TRANSPARENT);

        String authorS = getIntent().getExtras().getString("author");
        author.setText(authorS);
        String textS = getIntent().getExtras().getString("text");
        editor.setHtml(textS);
        String titleX = Html.fromHtml(textS).toString().replaceAll("[\n\r]", " ");
        if(titleX.split(" ").length > 6)
            title.setText(strJoin(Arrays.copyOfRange(titleX.split(" "), 0, 7), " "));
        else title.setText(titleX);

        double latS = getIntent().getExtras().getDouble("lat");
        double lonS = getIntent().getExtras().getDouble("lon");

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latS, lonS,1);
            place.setText(addresses.get(0).getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String strJoin(String[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = aArr.length; i < il; i++) {
            if (i > 0)
                sbStr.append(sSep);
            sbStr.append(aArr[i]);
        }
        return sbStr.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}