package com.example.myapplication;

import android.location.Location;

    public class GeofenceHelper {

        // إحداثيات موقع الشركة المسموح به (مثال: إحداثيات موقع في الرياض)
        public static final double COMPANY_LATITUDE = 24.7136;
        public static final double COMPANY_LONGITUDE = 46.6753;

        // نصف القطر المسموح للبصمة (150 متر)
        public static final float ALLOWED_RADIUS_METERS = 150.0f;

        /**
         * تتحقق مما إذا كان الموظف داخل نطاق موقع الشركة
         */
        public static boolean isWithinCompanyRange(double userLat, double userLng) {
            float distance = getDistanceInMeters(userLat, userLng);
            return distance <= ALLOWED_RADIUS_METERS;
        }

        /**
         * تحسب المسافة الفعلية بين الموظف ومقر الشركة بالمتر
         */
        public static float getDistanceInMeters(double userLat, double userLng) {
            float[] results = new float[1];
            Location.distanceBetween(
                    userLat, userLng,
                    COMPANY_LATITUDE, COMPANY_LONGITUDE,
                    results
            );
            return results[0];
        }
    }

