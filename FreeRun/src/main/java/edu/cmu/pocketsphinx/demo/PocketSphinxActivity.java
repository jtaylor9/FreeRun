

package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {
		
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String START_SEARCH = "start";
    private static final String STOP_SEARCH = "stop";
    private static final String RESET_SEARCH = "reset";
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
    long lapTime;
    long  timeElapsed;
    String finalTime;

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
                //chron.setText(hh+":"+mm+":"+ss);
            }
        });
        //chron.setText(hh+":"+mm+":"+ss);
       // chron.setText("00:00:00");

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
            lapTimes.clear();                                             //Reset the recorded laps
            ((TextView) findViewById(R.id.laptext)).setText("");

            chron.setBase(SystemClock.elapsedRealtime());
            chron.start();
        }


        else if (text.equals(STOP_SEARCH)) {

            ((TextView) findViewById(R.id.receivedText)).setText("Got Stop");
            chron.stop();

            timeElapsed = SystemClock.elapsedRealtime() - chron.getBase();
            int hours = (int) (timeElapsed / 3600000);
            int minutes = (int) (timeElapsed - hours * 3600000) / 60000;
            int seconds = (int) (timeElapsed - hours * 3600000 - minutes * 60000) / 1000;
            finalTime = "Total Time: [" + hours + " hrs:" + minutes + " mins:" + seconds + " secs]";
            chron.setText("");
            lapCounter = 1;
            index = 0;
            //lapTimes.clear();

        }

        else if(text.equals(LAP_SEARCH)) {

            ((TextView) findViewById(R.id.receivedText)).setText("Got Lap");

        }

        else if(text.equals(RESULTS_SEARCH));

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
            if (text.equals(LAP_SEARCH)) {
                lapTime = SystemClock.elapsedRealtime() - chron.getBase();
                int hours = (int) (lapTime / 3600000);
                int minutes = (int) (lapTime - hours * 3600000) / 60000;
                int seconds = (int) (lapTime - hours * 3600000 - minutes * 60000) / 1000;
                lapTimes.add("Lap " + lapCounter + ": " +  + hours + " hrs:" + minutes + " mins:" + seconds + " secs]");
                lapCounter++;
                ((TextView) findViewById(R.id.laptext)).setText(lapTimes.get(index));
                index++;
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
        
        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 5000);

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
                .setKeywordThreshold(1e-40f)
                
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