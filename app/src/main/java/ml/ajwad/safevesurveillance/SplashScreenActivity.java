package ml.ajwad.safevesurveillance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

public class SplashScreenActivity extends Activity {

    ImageView iv;
    Handler handler1, handler2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        iv = findViewById(R.id.splash);
        handler2 = new Handler();
        handler2.postDelayed(() -> {
            Intent intent = new Intent(SplashScreenActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }, 3000);
    }
}