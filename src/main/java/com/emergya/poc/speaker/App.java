package com.emergya.poc.speaker;

import java.util.concurrent.TimeUnit;

public class App {
	public static void main(String[] args) {

		try {
			long startTime = System.nanoTime();

			Transcriber.transcribeDiarization("/home/ajherrera/Escritorio/nocumple.WAV");

			long stopTime = System.nanoTime();
			long totalTime = stopTime - startTime;

			System.out.println(TimeUnit.SECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + " seconds");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
