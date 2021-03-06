package org.speechrecognizer;

import android.content.Context;
import io.reactivex.rxjava3.annotations.NonNull;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Singleton;
import org.speechrecognizer.impl.SpeechListener;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

/**
 * Service to recognize user's speech.
 */
@Singleton
public final class RecognizeSpeechService {

  private static final float SAMPLE_RATE = 16000.0f;

  private SpeechService voskSpeechService;
  private SpeechListener speechListener;
  private boolean isInitialized;

  /**
   * Initializes the service instance.
   *
   * @param context         the context, cannot be {@code null}
   * @param onReadyCallback the callback called when initialization done, cannot be {@code null}
   * @param onErrorCallback the callback called if initialization failed, cannot be {@code null}
   */
  public synchronized void initialize(@NonNull Context context, @NonNull Runnable onReadyCallback,
      @NonNull Consumer<? super Throwable> onErrorCallback) {
    if (!isInitialized) {
      initVosk(context, onReadyCallback, onErrorCallback);
    } else {
      onReadyCallback.run();
    }
  }

  /**
   * Closes the service.
   */
  public synchronized void close() {
    speechListener = null;

    if (voskSpeechService != null) {
      voskSpeechService.stop();
      voskSpeechService.shutdown();
      voskSpeechService = null;
    }

    isInitialized = false;
  }

  /**
   * Gets speech listener instance related to the service.
   *
   * @return the {@link ISpeechListener} instance, can be {@code null} if service not initialized
   * yet
   */
  public ISpeechListener getSpeechListener() {
    return voskSpeechService != null ? speechListener : null;
  }

  private void initVosk(@NonNull Context context, @NonNull Runnable onReadyCallback,
      @NonNull Consumer<? super Throwable> onErrorCallback) {

    StorageService.unpack(context, "model-ru", "model",
        voskModel -> {
          try {
            Recognizer rec = new Recognizer(voskModel, SAMPLE_RATE);
            this.voskSpeechService = new SpeechService(rec, SAMPLE_RATE);
          } catch (IOException exception) {
            onErrorCallback.accept(exception);
            return;
          }

          this.speechListener = createSpeechListener(context);
          onReadyCallback.run();

          // run startListening() AFTER callback call to complete initialization before real listening start
          voskSpeechService.startListening(speechListener);
          isInitialized = true;
        },
        exception -> onErrorCallback.accept(exception));
  }

  private SpeechListener createSpeechListener(Context context) {
    return new SpeechListener(context.getResources().getString(R.string.begin_recognize_word));
  }
}
