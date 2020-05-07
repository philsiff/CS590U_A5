package com.example.currentplacedetailsonmap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MyScoresActivity extends Activity {

    private String IP;
    private int PORT;
    private int myId;
    private TextView time_diff_label;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        IP = extras.getString("IP");
        PORT = extras.getInt("PORT");
        myId = extras.getInt("myId");

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.my_scores_layout);

        time_diff_label = findViewById(R.id.time_diff_label);

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
            obj.put("query", "get_score");
            obj.put("id", myId);
            obj.put("time_diff", time_diff);
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
