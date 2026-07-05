package bl00dy_c0d3_.echovr_installer;

public class QuestWizardState extends WizardState {

    private String apkFilename = "echo_quest_05-07-2026.001.apk";
    private int adbDeviceStatus = -1;
    private boolean isPatchedApk = false;

    public String getApkFilename() {
        return apkFilename;
    }

    public void setApkFilename(String apkFilename) {
        this.apkFilename = apkFilename;
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
                + ", adbDeviceStatus=" + adbDeviceStatus
                + ", isPatchedApk=" + isPatchedApk + "}";
    }
}
