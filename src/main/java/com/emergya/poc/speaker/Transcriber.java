package com.emergya.poc.speaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.SpeechSettings;
import com.google.cloud.speech.v1p1beta1.WordInfo;
import com.google.protobuf.ByteString;

import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

public class Transcriber {

	private Transcriber() {
		throw new IllegalStateException("Utility class");
	}

	private static final String SENTENCE = "en breves momentos le atenderemos";
	private static final String PATH_TO_CSV = System.getProperty("user.dir");
	private static final String PATH_GOOGLE_CREDENTIALS_JSON = "/home/ajherrera/Escritorio/speechtranscriber-321809-22a5f801cacd.json";
	private static final Logger LOGGER = Logger.getLogger("[Transcriber log]");

	public static Map<String, String> transcribeDiarization(String fileName) throws Exception {
		// Cut wav file to 50s
		cutAudioFile(fileName, fileName, 0, 50);

		Map<String, String> filePathTime = new HashMap<>();

		// Use this block of export google credentials
		SpeechSettings speechSettings = setGoogleCredentials();

		try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
			// The language of the supplied audio
			String languageCode = "es-ES";
			StringBuilder sentenceToCompare = new StringBuilder("");

			// Sample rate in Hertz of the audio data sent
			int sampleRateHertz = 8000;

			// Encoding of audio data sent. This sample sets this explicitly.
			// This field is optional for FLAC and WAV audio formats.
			// In this case, we process the audio as an mp3 encoding instead of a wav
			// encoding
			// SetEnableWordTimeOffset get offset of each word
			RecognitionConfig config = RecognitionConfig.newBuilder().setLanguageCode(languageCode)
					.setSampleRateHertz(sampleRateHertz).setEncoding(AudioEncoding.MP3).setEnableWordTimeOffsets(true)
					.build();

			// Processes the audio and waits to get the response in an audio
			Path path = Paths.get(fileName);
			byte[] data = Files.readAllBytes(path);
			ByteString content = ByteString.copyFrom(data);

			RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(content).build();

			// Use non-blocking call for getting file transcription
			OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response = speechClient
					.longRunningRecognizeAsync(config, audio);

			while (!response.isDone()) {
				LOGGER.log(Level.INFO, "Waiting for response...");
				Thread.sleep(10000);
			}

			List<SpeechRecognitionResult> results = response.get().getResultsList();
			int i = 0;

			for (SpeechRecognitionResult result : results) {
				SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);

				LOGGER.log(Level.INFO, "Transcription: " + alternative.getTranscript());
				boolean firstAppear = true;
				for (WordInfo wordInfo : alternative.getWordsList()) {
					String timeAppareance = wordInfo.getStartTime().getSeconds() + "."
							+ wordInfo.getStartTime().getNanos() / 100000000;

					Double firstAppareance = 0.;
					Double time = Double.parseDouble(timeAppareance);

					if (time < 20.0 && SENTENCE.split(" ")[i].equals(wordInfo.getWord())) {
						sentenceToCompare.append(wordInfo.getWord() + " ");
						i++;

						if (firstAppear) {
							firstAppareance = time;
						}

						firstAppear = false;

						if (sentenceToCompare.toString().trim().equals(SENTENCE)) {
							filePathTime.put(fileName, firstAppareance.toString());

							return filePathTime;
						}
					} else {
						i = 0;
						firstAppareance = 0.;
						firstAppear = true;
						sentenceToCompare = new StringBuilder();
					}
				}
			}
		}

		filePathTime.put(fileName, "false");

		writeHashMapToCsv(filePathTime);

		return filePathTime;
	}

	public static SpeechSettings setGoogleCredentials() throws FileNotFoundException, IOException {
		FileInputStream credentialsStream = new FileInputStream(PATH_GOOGLE_CREDENTIALS_JSON);
		GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
		FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
		SpeechSettings speechSettings = SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
		return speechSettings;
	}

	public static boolean cutAudioFile(String sourcefile, String targetfile, int start, int end) {
		try {
			if (!sourcefile.toLowerCase().endsWith(".wav") || !targetfile.toLowerCase().endsWith(".wav")) {
				return false;
			}
			File wav = new File(sourcefile);
			if (!wav.exists()) {
				return false;
			}
			long t1 = getTimeLength(wav);
			if (start < 0 || end <= 0 || start >= t1 || end > t1 || start >= end) {
				return false;
			}
			FileInputStream fis = new FileInputStream(wav);
			long wavSize = wav.length() - 44;
			long splitSize = (wavSize / t1) * (end - start);
			long skipSize = (wavSize / t1) * start;
			int splitSizeInt = Integer.parseInt(String.valueOf(splitSize));
			int skipSizeInt = Integer.parseInt(String.valueOf(skipSize));

			ByteBuffer buf1 = ByteBuffer.allocate(4);
			buf1.putInt(splitSizeInt + 36);
			byte[] flen = buf1.array();
			ByteBuffer buf2 = ByteBuffer.allocate(4);
			buf2.putInt(splitSizeInt);
			byte[] dlen = buf2.array();
			flen = reverse(flen);
			dlen = reverse(dlen);
			byte[] head = new byte[44];
			fis.read(head, 0, head.length);
			for (int i = 0; i < 4; i++) {
				head[i + 4] = flen[i];
				head[i + 40] = dlen[i];
			}
			byte[] fbyte = new byte[splitSizeInt + head.length];
			for (int i = 0; i < head.length; i++) {
				fbyte[i] = head[i];
			}
			byte[] skipBytes = new byte[skipSizeInt];
			fis.read(skipBytes, 0, skipBytes.length);
			fis.read(fbyte, head.length, fbyte.length - head.length);
			fis.close();

			File target = new File(targetfile);
			if (target.exists()) {
				target.delete();
			}
			FileOutputStream fos = new FileOutputStream(target);
			fos.write(fbyte);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Exception: " + e);
			return false;
		}
		return true;
	}

	public static long getTimeLength(File file) {
		long tlen = 0;
		if (file != null && file.exists()) {
			Encoder encoder = new Encoder();
			try {
				MultimediaInfo m = encoder.getInfo(file);
				long ls = m.getDuration();
				tlen = ls / 1000;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Exception: " + e);
			}
		}
		return tlen;
	}

	public static byte[] reverse(byte[] array) {
		byte temp;
		int len = array.length;
		for (int i = 0; i < len / 2; i++) {
			temp = array[i];
			array[i] = array[len - 1 - i];
			array[len - 1 - i] = temp;
		}
		return array;
	}

	public static void writeHashMapToCsv(Map<String, String> map) {
		String eol = System.getProperty("line.separator");

		try (Writer writer = new FileWriter(PATH_TO_CSV + "results.csv", true)) {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				writer.append(entry.getKey()).append(',').append(entry.getValue()).append(eol);
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "Exception: " + ex);
		}
	}
}
