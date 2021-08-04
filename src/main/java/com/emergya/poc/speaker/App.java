package com.emergya.poc.speaker;

public class App {
	public static void main(String[] args) {

		try {
			String pathToFileWav = args[0];
			Transcriber.transcribeDiarization(pathToFileWav);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
