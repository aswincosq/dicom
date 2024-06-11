package com.dcm;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OrthancRetrieve {
    private static final String ORTHANC_URL = "http://localhost:8082";
    // private static final String STUDY_INSTANCE_UID = "1.3.6.1.4.1.14519.5.2.1.1600.1206.314186418559291159892058485059";
    private static final String ORTHANC_ID = "e5dc89b5-a8a4fc4f-13c123cb-5c3a1f95-9b92491f";

    public static void main(String[] args) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet request = new HttpGet(ORTHANC_URL + "/studies/" + ORTHANC_ID + "/archive");
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inputStream = entity.getContent();
                     FileOutputStream outputStream = new FileOutputStream("study.zip")) {
                    int read;
                    byte[] buffer = new byte[4096];
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    System.out.println("DICOM files retrieved and saved as study.zip");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
