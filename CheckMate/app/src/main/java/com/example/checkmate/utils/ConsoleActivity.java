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
import org.w3c.dom.Text;

/**
 * Credit for some of file: https://github.com/chaquo/chaquopy-console
 * Some adaptations were made (removed unnecessary functions, incorporated our functionality)
 */

public abstract class ConsoleActivity extends AppCompatActivity
implements ViewTreeObserver.OnGlobalLayoutListener {

    protected Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        task = ViewModelProviders.of(this).get(getTaskClass());
        setContentView(resId("layout", "activity_console"));
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
        TextView tvHidden = findViewById(resId("id", "tvHidden"));
        task.output.observe(this, new Observer<CharSequence>() {
            @Override
            public void onChanged(CharSequence charSequence) {
                String str = charSequence.toString();
                Log.d("debug onChanged", str);
                String[] arr = str.split(" ");
                if (arr[0].equals("user_id")) {
                    Intent intent = new Intent(ConsoleActivity.this, VenmoAdapter.class);
                    intent.putExtra("venmoId", arr[1]);
                    ConsoleActivity.this.startActivity(intent);
                }
                else if (arr[0].equals("unsuccessful_ids")) {
                    Intent intent = new Intent(ConsoleActivity.this, VenmoAdapter.class);
                    String ret = "";
                    for(int i=1; i<arr.length; i++) {
                        ret += arr[i] + ",";
                    }
                    intent.putExtra("unsuccessful", ret);
                    ConsoleActivity.this.startActivity(intent);
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

    private void output(CharSequence text) {
        Log.d("debug python output", text.toString());
    }

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
            Log.d("debug python output", text.toString());
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
