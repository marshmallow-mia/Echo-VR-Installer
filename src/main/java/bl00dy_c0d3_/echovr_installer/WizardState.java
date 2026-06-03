package bl00dy_c0d3_.echovr_installer;

public class WizardState {

    public enum UserType {
        OWNER,
        NEW_PLAYER
    }

    public enum PlayStyle {
        STEAMVR,
        META_LINK
    }

    private UserType userType;
    private PlayStyle playStyle;
    private String installPath = "";

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public PlayStyle getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(PlayStyle playStyle) {
        this.playStyle = playStyle;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        if (installPath != null) {
            this.installPath = normalizePath(installPath);
        }
    }

    public String getBinPath() {
        return installPath + "/bin/win10";
    }

    @Override
    public String toString() {
        return "WizardState{userType=" + userType + ", playStyle=" + playStyle + ", path=" + installPath + "}";
    }

    private static String normalizePath(String path) {
        String normalized = path.replace("\\", "/");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
