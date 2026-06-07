package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;

public class OAuth2ErrorHandler {

    public static void handleError(Throwable error, JDialog parent, SpecialButton triggerBtn) {
        if (error instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
            if ("not_in_guild".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Join Server First", oae.getMessage(), 0);
            } else if ("busy".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Bot Busy", oae.getMessage(), 0);
            } else {
                new ErrorDialog().errorDialog(parent, "Authorization Failed", oae.getMessage(), 0);
            }
        } else {
            new ErrorDialog().errorDialog(parent, "Error",
                    "Failed: " + (error != null ? error.getMessage() : "Unknown error"), 0);
        }
        if (triggerBtn != null) {
            triggerBtn.setEnabled(true);
        }
    }
}
