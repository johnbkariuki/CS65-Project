package com.example.checkmate.console;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.example.checkmate.utils.PythonConsoleActivity;

import java.util.ArrayList;
import java.util.List;

public class RequestActivity extends PythonConsoleActivity {

    protected static String username;
    protected static String password;
    protected static String amountsStr;
    protected static String notesStr;
    protected static String idsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getIntent().getExtras();
        username = b.getString("username");
        password = b.getString("password");
        ArrayList<Double> amountsList = (ArrayList<Double>) b.getSerializable("amountsList");
        Log.d("debug amountsList", amountsList.toString());
        ArrayList<String> notesList = (ArrayList<String>) b.getSerializable("notesList");
        Log.d("debug notesList", notesList.toString());
        ArrayList<String> idsList = (ArrayList<String>) b.getSerializable("idsList");
        Log.d("debug idsList", idsList.toString());
        amountsStr = listToString(amountsList);
        notesStr = listToString(notesList);
        idsStr = listToString(idsList);
    }

    protected String listToString(ArrayList list) {
        String ret = "";
        for (int i=0; i<list.size(); i++) {
            ret += list.get(i).toString();
            ret += "////";
        }
        return ret;
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
            py.getModule("main").callAttr("venmo_payments", username, password,
                    amountsStr, notesStr, idsStr);
        }
    }
}
