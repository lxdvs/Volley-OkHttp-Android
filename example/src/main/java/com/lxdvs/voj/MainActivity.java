package com.lxdvs.voj;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestListener;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StarRequest request = new StarRequest(new RequestListener< StarRequest >() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(StarRequest response) {
                String result = "rate:" + response.rate.remaining;
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }
        });
        ExampleApplication.queue.add(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
