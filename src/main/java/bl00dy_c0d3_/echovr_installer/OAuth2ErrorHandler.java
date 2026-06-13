package bl00dy_c0d3_.echovr_installer;

import javax.swing.*;

public class OAuth2ErrorHandler {

    public static void handleError(Throwable error, JDialog parent, SpecialButton triggerBtn) {
        if (error instanceof DiscordOAuth2Flow.OAuth2Exception oae) {
            if ("not_in_guild".equals(oae.getErrorCode())) {
                int choice = JOptionPane.showOptionDialog(parent,
                    oae.getMessage(),
                    "Join Server First",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new String[]{"Join Server", "Close"},
                    "Join Server");
                if (choice == 0) {
                    Helpers.openUrl("https://discord.gg/bMpsva6fmA");
                }
            } else if ("phone_verification_required".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Phone Verification Required",
                    "Discord requires a verified phone number to interact in this server.\n\n" +
                    "Please verify your phone number in Discord Settings → Account → Phone Number, then try again.", 0);
            } else if ("busy".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Bot Busy", oae.getMessage(), 0);
            } else if ("cancelled".equals(oae.getErrorCode())) {
                // User-initiated (or retry-initiated) cancel — no dialog, just re-enable the button below.
            } else if ("timeout".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Try again in a minute", oae.getMessage(), 0);
            } else if ("port_in_use".equals(oae.getErrorCode())) {
                new ErrorDialog().errorDialog(parent, "Authorization busy", oae.getMessage(), 0);
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
