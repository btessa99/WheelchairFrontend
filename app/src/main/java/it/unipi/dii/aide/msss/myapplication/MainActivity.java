package it.unipi.dii.aide.msss.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button contributeButton;
    Button routingButton;
    Button exploreButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contributeButton = (Button) findViewById(R.id.contribute);
        routingButton = (Button) findViewById(R.id.path);
        exploreButton = (Button) findViewById(R.id.explore);

        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeActivity(MapsActivity.class);
            }
        });

        contributeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeActivity(ContributorActivity.class);
            }
        });
    }

    private void changeActivity(Class newActivity){
        Intent intent = new Intent(MainActivity.this,newActivity);
        startActivity(intent);
    }
}