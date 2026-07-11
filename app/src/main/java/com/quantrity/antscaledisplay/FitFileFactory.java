package com.quantrity.antscaledisplay;

import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.WeightScaleMesg;

import java.io.File;
import java.util.Date;

/** Creates the Garmin FIT payload independently of activities and upload transport. */
final class FitFileFactory {
    File create(File output, Weight weight) {
        WeightScaleMesg weightMessage = new WeightScaleMesg();
        weightMessage.setTimestamp(new DateTime(new Date(weight.date)));
        weightMessage.setWeight((float) weight.weight);
        if (weight.percentFat != -1) weightMessage.setPercentFat((float) weight.percentFat);
        if (weight.percentHydration != -1) {
            weightMessage.setPercentHydration((float) weight.percentHydration);
        }
        if (weight.boneMass != -1) weightMessage.setBoneMass((float) weight.boneMass);
        if (weight.muscleMass != -1) weightMessage.setMuscleMass((float) weight.muscleMass);
        if (weight.physiqueRating != -1) {
            weightMessage.setPhysiqueRating((short) weight.physiqueRating);
        }
        if (weight.visceralFatRating != -1) {
            weightMessage.setVisceralFatRating((short) Math.round(weight.visceralFatRating));
        }
        if (weight.metabolicAge != -1) {
            weightMessage.setMetabolicAge((short) weight.metabolicAge);
        }
        if (weight.basalMet != -1) weightMessage.setActiveMet((float) weight.basalMet);
        else if (weight.activeMet != -1) weightMessage.setActiveMet((float) weight.activeMet);
        if (weight.height > 0 && weight.weight != -1) {
            weightMessage.setBmi((float) (weight.weight / Math.pow(weight.height / 100, 2)));
        }

        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.WEIGHT);
        fileId.setManufacturer(Manufacturer.TANITA);
        fileId.setProduct(1);
        fileId.setSerialNumber(1L);

        FileEncoder encoder = new FileEncoder(output, Fit.ProtocolVersion.V2_0);
        encoder.write(fileId);
        encoder.write(weightMessage);
        encoder.close();
        return output;
    }
}
