package com.sevenmoor.penstriman;

import android.content.SharedPreferences;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SaveRestore {

    String VIDEO_NAME_TAG = "VIDEO_NAME";

    private ArrayList<String> container;
    SharedPreferences sharedPreferences;

    public SaveRestore(ArrayList<String> dataTosave, SharedPreferences sharedPreferences){
        container = dataTosave;
        this.sharedPreferences = sharedPreferences;
    }

    public void save(){
        Gson gson = new Gson();
        String json = gson.toJson(container);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VIDEO_NAME_TAG,json );
        editor.apply();
    }

    public ArrayList<String> restore(){
        Gson gson = new Gson();
        ArrayList<String> stored = null;
        String json = sharedPreferences.getString(VIDEO_NAME_TAG, "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<List<String>>() {
            }.getType();
            stored = gson.fromJson(json, type);
        }
        return stored; 
    }


    public void clean(){
        container.clear();
    }

}
