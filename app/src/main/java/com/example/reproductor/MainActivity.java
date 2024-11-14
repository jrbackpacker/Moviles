package com.example.reproductor;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private Spinner songSpinner;
    private SeekBar progressSeekBar, volumeSeekBar;
    private TextView timeLabel;
    private Handler handler = new Handler();
    private AudioManager audioManager;

    // Nombres de las canciones preguardadas en la carpeta `raw`
    private String[] songList = {"Song 1", "Song 2"};
    private int[] songResources = {R.raw.song1, R.raw.song2}; // Recursos de canciones en `raw`

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vinculamos las vistas del XML a las variables de la clase
        songSpinner = findViewById(R.id.songSpinner);
        progressSeekBar = findViewById(R.id.progressSeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        timeLabel = findViewById(R.id.timeLabel);

        Button playButton = findViewById(R.id.playButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button rewindButton = findViewById(R.id.rewindButton);
        Button forwardButton = findViewById(R.id.forwardButton);

        // Configurar AudioManager para controlar el volumen del dispositivo
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);

        // Listener para cambiar el volumen con la SeekBar
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Cargar canciones en Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, songList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        songSpinner.setAdapter(adapter);

        // Listener para cambiar de canción al seleccionar una en el Spinner
        songSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(MainActivity.this, songResources[position]);
                setupSeekBar();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configurar SeekBar de progreso de la canción
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Configurar los botones para las acciones correspondientes
        playButton.setOnClickListener(v -> play(v));
        pauseButton.setOnClickListener(v -> pause(v));
        stopButton.setOnClickListener(v -> stop(v));
        rewindButton.setOnClickListener(v -> rewind(v));
        forwardButton.setOnClickListener(v -> forward(v));
    }

    // Configura la SeekBar de progreso y el tiempo total de la canción
    private void setupSeekBar() {
        progressSeekBar.setMax(mediaPlayer.getDuration());
        updateSeekBar();

        mediaPlayer.setOnCompletionListener(mp -> {
            progressSeekBar.setProgress(0);
            timeLabel.setText("0:00 / " + formatTime(mediaPlayer.getDuration() / 1000));
        });
    }

    // Método para actualizar la SeekBar de progreso y el tiempo actual de la canción
    private void updateSeekBar() {
        if (mediaPlayer != null) {
            int currentPos = mediaPlayer.getCurrentPosition() / 1000;
            int duration = mediaPlayer.getDuration() / 1000;
            timeLabel.setText(formatTime(currentPos) + " / " + formatTime(duration));
            progressSeekBar.setProgress(mediaPlayer.getCurrentPosition());

            if (mediaPlayer.isPlaying()) {
                handler.postDelayed(this::updateSeekBar, 1000);
            }
        }
    }

    // Formatear el tiempo en minutos y segundos
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    // Métodos para los botones Play, Pause y Stop
    public void play(View view) {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateSeekBar();
        }
    }

    public void pause(View view) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void stop(View view) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
            progressSeekBar.setProgress(0);
            timeLabel.setText("0:00 / " + formatTime(mediaPlayer.getDuration() / 1000));
        }
    }

    // Métodos para adelantar y retroceder 10 segundos
    public void rewind(View view) {
        if (mediaPlayer != null) {
            int newPosition = Math.max(mediaPlayer.getCurrentPosition() - 10000, 0);
            mediaPlayer.seekTo(newPosition);
            updateSeekBar();
        }
    }

    public void forward(View view) {
        if (mediaPlayer != null) {
            int newPosition = Math.min(mediaPlayer.getCurrentPosition() + 10000, mediaPlayer.getDuration());
            mediaPlayer.seekTo(newPosition);
            updateSeekBar();
        }
    }

    // Liberar recursos de MediaPlayer al cerrar la aplicación
    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null); // Detener cualquier llamada pendiente al handler
        super.onDestroy();
    }
}
