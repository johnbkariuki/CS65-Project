package com.example.checkmate.console;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyException;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.checkmate.utils.*;

public class CodeActivity extends PythonConsoleActivity {

    protected static String username;
    protected static String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        username = b.getString("username");
        password = b.getString("password");
    }

    // On API level 31 and higher, pressing Back in a launcher activity sends it to the back by
    // default, but that would make it difficult to restart the activity.
    @Override public void onBackPressed() {
        finish();
    }

    @Override protected Class<? extends Task> getTaskClass() {
        return Task.class;
    }

    public static class Task extends PythonConsoleActivity.Task {
        public Task(Application app) {
            super(app);
        }

        @Override public void run() {
            try {
                py.getModule("main").callAttr("venmo_setup", username, password);
            }
            catch (PyException error) {
                Intent intent = new Intent(getApplication().getApplicationContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().getApplicationContext().startActivity(intent);
            }
        }
    }
}
