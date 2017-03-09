package com.gadgetsmend.vhicalowneralert;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class SaveSharedPreference {
    private static final String PREF_VEHICLE_NO= "VehicleNo";

    private static SharedPreferences getSharedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    static void setVehicleNo(Context ctx, String vehicle_no) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(PREF_VEHICLE_NO , vehicle_no);
        editor.apply();
    }

    static String getVehicleNo(Context ctx) {
        return getSharedPreferences(ctx).getString(PREF_VEHICLE_NO, "");
    }
}