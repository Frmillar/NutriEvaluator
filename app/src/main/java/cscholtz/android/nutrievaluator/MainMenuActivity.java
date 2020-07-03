package cscholtz.android.nutrievaluator;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        System.out.println("MAINMENU");

        Button loop = (Button) findViewById(R.id.buttonLoop);
        Button cache = (Button) findViewById(R.id.buttonCache);

        loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainMenuActivity.this, LoopUploadActivity.class);
                MainMenuActivity.this.startActivity(myIntent);
            }
        });
        cache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainMenuActivity.this, CacheUploadActivity.class);
                MainMenuActivity.this.startActivity(myIntent);
            }
        });

    }
}
