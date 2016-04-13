

package edu.hawaii.btsuha;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import hawaii.edu.btsuha.R;


import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {
		
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String START_SEARCH = "start";
    private static final String STOP_SEARCH = "stop";
    private static final String MENU_SEARCH = "menu";
    private static final String LAP_SEARCH = "lap";
    private static final String RESULTS_SEARCH = "results";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "free run please";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    /* The chronometer */
    Chronometer chron;

    String hh, mm, ss;

    ArrayList<String> lapTimes = new ArrayList<>();
    int lapCounter = 1;
    int index = 0;
    float finalDistance;
    long lapTime;
    long  timeElapsed;
    String finalTime;
    TextView textDistance;

    GPSTracker gps;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");
        textDistance = (TextView) findViewById(R.id.textDistance);

        gps = new GPSTracker(PocketSphinxActivity.this);
        if(!gps.canGetLocation()) {      //Checking if location is on
            gps.showSettingsAlert();
            gps.getLocation();
        }

        //Instantiate chronometer
        chron = (Chronometer) findViewById(R.id.chronometer);

        chron.setOnChronometerTickListener(new OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chron) {
                long time = SystemClock.elapsedRealtime() - chron.getBase();
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                hh = h < 10 ? "0" + h : h + "";
                mm = m < 10 ? "0" + m : m + "";
                ss = s < 10 ? "0" + s : s + "";

                float miles = gps.getDistance()/(float)1609.34;  //Convert from meters to miles
                textDistance.setText(String.format("%.2f miles", miles));  //Update text
            }
        });

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recognizer.cancel();
        recognizer.shutdown();
    }
    
    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {

        if (hypothesis == null)
    	    return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            switchSearch(MENU_SEARCH);
            ((TextView) findViewById(R.id.finalTime)).setText("");
        }
        else if (text.equals(START_SEARCH)) {
            ((TextView)findViewById(R.id.receivedText)).setText("Got Start");
            recognizer.stop();
        }


        else if (text.equals(STOP_SEARCH)) {
            ((TextView) findViewById(R.id.receivedText)).setText("Got Stop");
            recognizer.stop();

        }

        else if(text.equals(LAP_SEARCH)) {
            ((TextView) findViewById(R.id.receivedText)).setText("Got Lap");
            recognizer.stop();

        }

        else if(text.equals(RESULTS_SEARCH)){
            ((TextView) findViewById(R.id.receivedText)).setText("Got Results");
            recognizer.stop();
        }

        else
            ((TextView) findViewById(R.id.receivedText)).setText("Not recognized");
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.receivedText)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

            if (text.equals(START_SEARCH)) {
                lapTimes.clear();                                             //Reset the recorded laps
                ((TextView) findViewById(R.id.laptext)).setText("");

                chron.setBase(SystemClock.elapsedRealtime());
                chron.start();
                gps.resetDistance();
                gps.getLocation();

                switchSearch(KWS_SEARCH);
            }

            if (text.equals(STOP_SEARCH)) {
                chron.stop();

                timeElapsed = SystemClock.elapsedRealtime() - chron.getBase();
                int hours = (int) (timeElapsed / 3600000);
                int minutes = (int) (timeElapsed - hours * 3600000) / 60000;
                int seconds = (int) (timeElapsed - hours * 3600000 - minutes * 60000) / 1000;
                finalTime = "Total Time: [" + hours + " hrs:" + minutes + " mins:" + seconds + " secs]";
                finalDistance = gps.getDistance();
                chron.setText("");
                lapCounter = 1;
                index = 0;

                gps.stopUsingGPS();

                switchSearch(KWS_SEARCH);
            }

            if (text.equals(LAP_SEARCH)) {
                lapTime = SystemClock.elapsedRealtime() - chron.getBase();
                int hours = (int) (lapTime / 3600000);
                int minutes = (int) (lapTime - hours * 3600000) / 60000;
                int seconds = (int) (lapTime - hours * 3600000 - minutes * 60000) / 1000;
                lapTimes.add("[Lap " + lapCounter + ": " +  + hours + " hrs:" + minutes + " mins:" + seconds + " secs]");
                lapCounter++;
                ((TextView) findViewById(R.id.laptext)).setText(lapTimes.get(index));
                index++;

                switchSearch(KWS_SEARCH);
            }

            else if (text.equals(RESULTS_SEARCH)) {
                String resultString = "";
                chron.setText("");
                ((TextView) findViewById(R.id.finalTime)).setText("in results");

                for(int i =0; i < lapTimes.size(); i++){
                    resultString = resultString.concat(lapTimes.get(i) + "\n");
                }

                ((TextView) findViewById(R.id.laptext)).setText(resultString);
                ((TextView) findViewById(R.id.finalTime)).setText(finalTime);
                ((TextView) findViewById(R.id.textDistance)).setText(String.format("%.2f miles", finalDistance));

                switchSearch(KWS_SEARCH);

            }
        }

    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        
        // If we are not spotting, start listening with timeout (1 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 1000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                
                // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                //.setRawLogDir(assetsDir)
                
                // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-30f)
                
                // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)
                
                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        
        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}
