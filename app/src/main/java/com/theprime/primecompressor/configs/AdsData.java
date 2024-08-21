package com.theprime.primecompressor.configs;

import com.adsmedia.adsmodul.utils.AdsConfig;

public class AdsData {
    /*
    change placement ID
    AdsConfig.Banner_ID = "your_placement_banner";
    AdsConfig.Interstitial_ID ="your_placement_interstitial";
    AdsConfig.Open_App_ID = "your_placement_open_app";
     */

    /*
    Change Admob ads to other ads https://codegith.com/support
     */
    public static void getIDAds(){
        AdsConfig.Banner_ID = "IMG_16_9_APP_INSTALL#";
        AdsConfig.Interstitial_ID ="IMG_16_9_APP_INSTALL#";
        AdsConfig.Open_App_ID = "IMG_16_9_APP_INSTALL#";// only for Admob, Applovin, and Pangle
        AdsConfig.Game_App_ID = "IMG_16_9_APP_INSTALL#"; // for Unity and Pangle
        AdsConfig.Interval = 0; // change number for interstitial
    }
}
