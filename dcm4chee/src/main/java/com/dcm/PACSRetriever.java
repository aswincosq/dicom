package com.dcm;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DimseRSPHandler;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.DicomServiceRegistry;

import java.util.concurrent.Executors;

public class PACSRetriever {

    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 4242;
        String calledAETitle = "ORTHANC";
        String callingAETitle = "MY_RETRIEVER";
        String studyInstanceUID = "1.3.6.1.4.1.14519.5.2.1.1600.1206.303404081778937298059938444830";

        try {
            // Set up the device and application entity
            Device device = new Device("DCMQR");
            ApplicationEntity ae = new ApplicationEntity(callingAETitle);
            device.addApplicationEntity(ae);
            device.setDimseRQHandler(new DicomServiceRegistry());

            // Initialize the executor
            device.setExecutor(Executors.newSingleThreadExecutor());
            device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

            // Set up the connection
            Connection remote = new Connection();
            remote.setHostname(hostname);
            remote.setPort(port);

            Connection local = new Connection();
            device.addConnection(local);
            ae.addConnection(local);

            // Create the association request
            AAssociateRQ rq = new AAssociateRQ();
            rq.setCalledAET(calledAETitle);

            // Add the Presentation Context for Study Root Query/Retrieve - FIND
            int pcid = 1; // Presentation context ID
            rq.addPresentationContext(new PresentationContext(pcid, "1.2.840.10008.5.1.4.1.2.2.1", "1.2.840.10008.1.2"));
            pcid += 2; // Increment by 2 for the next context (even numbers are reserved for responses)

            // Create the association
            Association association = ae.connect(local, remote, rq);

            // Query for the study
            Attributes queryKeys = new Attributes();
            queryKeys.setString(Tag.StudyInstanceUID, VR.UI, studyInstanceUID);
            queryKeys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");

            // Perform the query
            association.cfind("1.2.840.10008.5.1.4.1.2.2.1", 1, queryKeys, null, new DimseRSPHandler(association.nextMessageID()) {
                @Override
                public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
                    if (data != null) {
                        String iuid = data.getString(Tag.StudyInstanceUID);
                        System.out.println("Study Instance UID: " + iuid);

                        // Retrieve the DICOM files
                        try {
                            // Corrected cmove call
                            association.cmove(calledAETitle, 1, queryKeys, null, "1.2.840.10008.1.2.1", new DimseRSPHandler(association.nextMessageID()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // Wait for the association to complete
            association.waitForOutstandingRSP();

            // Ensure the association is in the correct state before releasing it
            if (association.isReadyForDataTransfer()) {
                association.release();
            } else {
                System.err.println("Association is not ready for release");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
