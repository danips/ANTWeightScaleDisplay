package com.quantrity.antscaledisplay;

/** Human-readable names for the ANT+ manufacturer identifiers used by devices. */
final class AntManufacturerRegistry {
    private AntManufacturerRegistry() {}

    static String nameFor(int manufacturerId) {
        switch (manufacturerId) {
            case 1: return "Garmin";
            case 3: return "Zephyr";
            case 6: return "SRM";
            case 7: return "Quarq";
            case 9: return "Saris";
            case 11: return "Tanita";
            case 13: return "Dynastream OEM";
            case 15: return "Dynastream";
            case 19: return "Beurer";
            case 20: return "CardioSport";
            case 21: return "A&D Medical";
            case 23: return "Suunto";
            case 32: return "Wahoo Fitness";
            case 41: return "Shimano";
            case 52: return "Seiko Epson";
            case 59: return "Mio Technology";
            case 68: return "CatEye";
            case 70: return "Sigma Sport";
            case 89: return "Tacx";
            case 95: return "Stryd";
            case 101: return "Body Bike Smart";
            case 106: return "Fitcare";
            case 107: return "Magene";
            case 110: return "Salutron";
            case 111: return "Technogym";
            case 115: return "iGPSPORT";
            case 116: return "ThinkRider";
            case 122: return "Johnson Health Tech";
            case 123: return "Polar";
            case 128: return "iFit";
            case 132: return "CYCPLUS";
            case 137: return "Bosch";
            case 140: return "Decathlon";
            case 148: return "EZON";
            case 150: return "MYZONE";
            case 152: return "Bafang";
            case 255: return "Development / unassigned";
            case 257: return "Health & Life";
            case 268: return "SRAM";
            case 294: return "COROS";
            case 305: return "WHOOP";
            case 310: return "Decathlon";
            case 340: return "Peloton";
            default: return null;
        }
    }
}
