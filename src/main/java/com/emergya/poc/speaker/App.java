package com.emergya.poc.speaker;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class App {
	public static void main(String[] args) {

		try {
			long startTime = System.nanoTime();

			File dir = new File(args[0]);
			String pathCredentials = args[1];
			
			File[] directoryListing = dir.listFiles();
			if (directoryListing != null) {
				for (File child : directoryListing) {
					String path = child.getPath();

					if (path.endsWith("WAV")) {
						Transcriber.transcribeDiarization(path, pathCredentials);
					}
				}
			}

			long stopTime = System.nanoTime();
			long totalTime = stopTime - startTime;

			System.out.println(TimeUnit.SECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + " seconds");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
