package com.example.checkmate.utils;

import androidx.annotation.NonNull;
import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.core.content.*;
import androidx.lifecycle.*;
import com.example.checkmate.VenmoAdapter;
import com.example.checkmate.console.LoginActivity;
import com.example.checkmate.console.PaymentActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Credit for some of file: https://github.com/chaquo/chaquopy-console
 * Some adaptations were made (removed unnecessary functions, incorporated our functionality)
 */

public abstract class ConsoleActivity extends AppCompatActivity
implements ViewTreeObserver.OnGlobalLayoutListener {

    protected Task task;
    protected Boolean fromLogin;
    protected ArrayList<Double> amountsList;
    protected ArrayList<String> notesList;
    protected ArrayList<String> idsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        task = ViewModelProviders.of(this).get(getTaskClass());
        setContentView(resId("layout", "activity_console"));

        // Check if console called from LoginActivity
        Bundle bundle = getIntent().getExtras();
        fromLogin = bundle.getBoolean("fromLogin");
        amountsList = (ArrayList<Double>) bundle.getSerializable("amountsList");
        notesList = (ArrayList<String>) bundle.getSerializable("notesList");
        idsList = (ArrayList<String>) bundle.getSerializable("idsList");

        createInput();
    }

    protected abstract Class<? extends Task> getTaskClass();

    private void createInput() {
        /*
         * Input from EditText and Button click
         */
        EditText etOTP = findViewById(resId("id", "etOTP"));
        Button btnOTP = findViewById(resId("id", "btnOTP"));
        btnOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = etOTP.getText().toString() + "\n";
                task.onInput(text);
            }
        });

        /*
         * Put returned value in an intent
         */
        task.output.observe(this, new Observer<CharSequence>() {
            @Override
            public void onChanged(CharSequence charSequence) {
                // Get string from console output
                String str = charSequence.toString();
                String[] arr = str.split(" ");

                // Get user id from console
                if (arr[0].equals("user_id")) {
                    Intent intent = new Intent(ConsoleActivity.this, VenmoAdapter.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("venmoId", arr[1]);
                    intent.putExtras(bundle);
                    ConsoleActivity.this.startActivity(intent);
                    finish();
                }

                // Get unsuccessful request ids from console
                else if (arr[0].equals("unsuccessful_ids")) {
                    Intent intent = new Intent(ConsoleActivity.this, VenmoAdapter.class);
                    String ret = "";
                    for(int i=1; i<arr.length; i++) {
                        ret += arr[i] + ",";
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("unsuccessful", ret);
                    intent.putExtras(bundle);
                    ConsoleActivity.this.startActivity(intent);
                    finish();
                }

                // Handle invalid OTP code
                else if (arr[0].equals("invalid_login") || arr[0].equals("invalid_otp")) {
                    // Restart login/payment activities
                    Intent intent;
                    if(fromLogin) {
                        intent = new Intent(ConsoleActivity.this, LoginActivity.class);
                    } else {
                        intent = new Intent(ConsoleActivity.this, PaymentActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("amountsList", amountsList);
                        bundle.putSerializable("notesList", notesList);
                        bundle.putSerializable("idsList", idsList);
                        intent.putExtras(bundle);
                    }
                    ConsoleActivity.this.startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Don't restore the UI state unless we have the non-UI state as well.
        if (task.getState() != Thread.State.NEW) {
            super.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Needs to be in onResume rather than onStart because onRestoreInstanceState runs
        // between them.
        if (task.getState() == Thread.State.NEW) {
            task.start();
        }
    }

    // This callback is run after onResume, after each layout pass. If a view's size, position
    // or visibility has changed, the new values will be visible here.
    @Override public void onGlobalLayout() {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {return true;}

    @Override public boolean onOptionsItemSelected(MenuItem item) {return true;}

    private void output(CharSequence text) { }

    public int resId(String type, String name) {
        return Utils.resId(this, type, name);
    }

    // =============================================================================================

    public static abstract class Task extends AndroidViewModel {

        private Thread.State state = Thread.State.NEW;

        public void start() {
            new Thread(() -> {
                try {
                    Task.this.run();
                } finally {
                    inputEnabled.postValue(false);
                    state = Thread.State.TERMINATED;
                }
            }).start();
            state = Thread.State.RUNNABLE;
        }

        public Thread.State getState() { return state; }

        public MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>();
        public BufferedLiveEvent<CharSequence> output = new BufferedLiveEvent<>();

        public Task(Application app) {
            super(app);
            inputEnabled.setValue(false);
        }

        /** Override this method to provide the task's implementation. It will be called on a
         *  background thread. */
        public abstract void run();

        /** Called on the UI thread each time the user enters some input, A trailing newline is
         * always included. The base class implementation does nothing. */
        public void onInput(String text) {}

        public void output(final CharSequence text) {
            if (text.length() == 0) return;
            output.postValue(text);
        }

        public void outputError(CharSequence text) {
            output(text);
        }

        public int resId(String type, String name) {
            return Utils.resId(getApplication(), type, name);
        }
    }

}
