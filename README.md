# speech-to-csv
Important: We read a file in .wav format

First we trim the audio to 50 seconds, since we are only interested in whether the phrase appears in the first 20 seconds.

Then, we search if there is any occurrence of a particular phrase in less than 20 seconds.
Finally we save the result in a csv, in case of finding the phrase the second where it occurs will be saved.

To test it, you must add the google credentials to use the speech-to-text tool, and change the path where the csv will be saved.

The input argument corresponds to the file where the audio is in .wav format.
