# ez-vcard
-keep,includedescriptorclasses class ezvcard.property.** { *; }
-keep enum ezvcard.VCardVersion { *; }
-dontwarn ezvcard.io.json.**
-dontwarn freemarker.**
-keep class ezvcard.parameter.** {
    <init>(...);
}

# fork: the 37 SDK stubs drop FingerprintManager; Commons' app-lock library (reprint) still calls it on old phones
-dontwarn android.hardware.fingerprint.**
