package com.example.currentplacedetailsonmap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.edmodo.rangebar.RangeBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class DemographicScoresActivtiy extends Activity {

    private String IP;
    private int PORT;
    private int myId;

    private int ageMin = 0;
    private int ageMax = 100;
    private String sex = "all";

    private TextView time_diff_label;
    private TextView age_min_label;
    private TextView age_max_label;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        IP = extras.getString("IP");
        PORT = extras.getInt("PORT");
        myId = extras.getInt("myId");

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.demo_scores_layout);

        time_diff_label = findViewById(R.id.time_diff_label);
        age_min_label = findViewById(R.id.age_min_label);
        age_max_label = findViewById(R.id.age_max_label);

        // Slider
        RangeBar ageBar = (RangeBar) findViewById(R.id.ageBar);
        ageBar.setTickCount(100);
        ageBar.setTickHeight(0);
        ageBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener(){
            @Override
            public void onIndexChangeListener(RangeBar rangeBar, int left, int right) {
                age_min_label.setText("Age Min: " + String.valueOf(left));
                age_max_label.setText("Age Max: " + String.valueOf(right));
                ageMin = left;
                ageMax = right;
            }
        });

        // Spinner
        Spinner sexSpinner = (Spinner) findViewById(R.id.sexSpinner);
        String[] items = new String[]{"M", "F", "All"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        sexSpinner.setAdapter(adapter);
        sexSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sex = parent.getItemAtPosition(position).toString().toLowerCase();
                Log.d("SPINNER_TESTING", String.valueOf(sex));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Buttons
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                time_diff_label.setText("Today");
                getScores(1);
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                time_diff_label.setText("One Week");
                getScores(7);
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                time_diff_label.setText("Two Weeks");
                getScores(14);
            }
        });

        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                time_diff_label.setText("One Month");
                getScores(30);
            }
        });
    }

    private void setScores(String score, String trips, String people){
        ((TextView) findViewById(R.id.people_label)).setText("People Near: " + people);
        ((TextView) findViewById(R.id.trips_label)).setText("Trips Taken: " + trips);
        ((TextView) findViewById(R.id.score_label)).setText("Score: " + score);
    }

    private void getScores(int time_diff){
        try {
            Socket clientSocket = new Socket(IP, PORT);

            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            JSONObject obj = new JSONObject();
            obj.put("type", "query");
            obj.put("query", "get_demo_score");
            obj.put("id", myId);
            obj.put("time_diff", time_diff);
            obj.put("age_min", ageMin);
            obj.put("age_max", ageMax);
            obj.put("sex", sex);
            String msg = obj.toString();
            out.writeUTF(msg);
            out.flush();

            String response = "";

            while (response.length() == 0 || response.charAt(response.length()-1) != 'Q'){
                if (in.ready()) {
                    response += in.readLine();
                    Log.d("RESPONSE_TESTING", "IN LOOP: " + response);
                }
            }

            response = response.substring(0, response.length() - 1);
            if (response.length() == 0){
                return;
            }
            JSONObject ret = new JSONObject(response);
            Log.d("SCORES TESTING", ret.getString("score"));

            setScores(ret.getString("score"), ret.getString("trips"), ret.getString("people"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
