package bl00dy_c0d3_.echovr_installer;

public class QuestWizardState extends WizardState {

    // Fallback only: the real name comes from the manifest's BASE_APK header. Kept so a
    // download still works when the manifest can't be reached.
    private String apkFilename = "echo_quest_16-07-2026.001.apk";
    private String baseApkSha256;
    private String installedApkSha256;
    private int adbDeviceStatus = -1;
    private boolean isPatchedApk = false;

    public String getApkFilename() {
        return apkFilename;
    }

    public void setApkFilename(String apkFilename) {
        this.apkFilename = apkFilename;
    }

    /** SHA-256 of the base APK this install is built from (manifest BASE_APK header). */
    public String getBaseApkSha256() {
        return baseApkSha256;
    }

    public void setBaseApkSha256(String baseApkSha256) {
        this.baseApkSha256 = baseApkSha256;
    }

    /** SHA-256 of the APK actually installed; differs from the base when patched. */
    public String getInstalledApkSha256() {
        return installedApkSha256;
    }

    public void setInstalledApkSha256(String installedApkSha256) {
        this.installedApkSha256 = installedApkSha256;
    }

    public int getAdbDeviceStatus() {
        return adbDeviceStatus;
    }

    public void setAdbDeviceStatus(int adbDeviceStatus) {
        this.adbDeviceStatus = adbDeviceStatus;
    }

    public boolean isPatchedApk() {
        return isPatchedApk;
    }

    public void setPatchedApk(boolean patchedApk) {
        isPatchedApk = patchedApk;
    }

    @Override
    public String toString() {
        return "QuestWizardState{userType=" + getUserType()
                + ", path=" + getInstallPath()
                + ", apkFilename=" + apkFilename
                + ", baseApkSha256=" + baseApkSha256
                + ", installedApkSha256=" + installedApkSha256
                + ", adbDeviceStatus=" + adbDeviceStatus
                + ", isPatchedApk=" + isPatchedApk + "}";
    }
}
