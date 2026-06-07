package bl00dy_c0d3_.echovr_installer;

public class PCWizardState extends WizardState {

    public enum PlayStyle {
        STEAMVR,
        META_LINK
    }

    private PlayStyle playStyle;

    public PlayStyle getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(PlayStyle playStyle) {
        this.playStyle = playStyle;
    }

    public String getBinPath() {
        return getInstallPath() + "/ready-at-dawn-echo-arena/bin/win10";
    }

    public String getExePath() {
        return getBinPath() + "/echovr.exe";
    }

    @Override
    public String toString() {
        return "PCWizardState{userType=" + getUserType() + ", playStyle=" + playStyle + ", path=" + getInstallPath() + "}";
    }
}
