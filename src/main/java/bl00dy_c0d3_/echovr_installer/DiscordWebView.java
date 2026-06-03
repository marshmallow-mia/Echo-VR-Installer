package bl00dy_c0d3_.echovr_installer;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.*;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

public class DiscordWebView {

    private Stage stage;
    private WebView webView;
    private WebEngine webEngine;
    private static final File COOKIE_DIR = new File(System.getProperty("user.home"), ".echovr_discord");

    static {
        Platform.setImplicitExit(false);
        COOKIE_DIR.mkdirs();
        try {
            Class<?> cmClass = Class.forName("com.sun.webkit.network.CookieManager");
            Method setStoragePath = cmClass.getMethod("setStoragePath", String.class);
            setStoragePath.invoke(null, COOKIE_DIR.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not set cookie storage path: " + e.getMessage());
        }
        new JFXPanel();
    }

    public DiscordWebView() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            webView = new WebView();
            webEngine = webView.getEngine();
            webEngine.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            VBox sidebar = new VBox(12);
            sidebar.setPadding(new Insets(16));
            sidebar.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #333; -fx-border-width: 0 1px 0 0;");
            sidebar.setPrefWidth(220);
            sidebar.setMinWidth(220);

            Label title = new Label("PATCH GUIDE");
            title.setFont(Font.font("Arial", 16));
            title.setTextFill(Color.WHITE);

            Label step1 = new Label("1. Log in to Discord");
            step1.setFont(Font.font("Arial", 12));
            step1.setTextFill(Color.LIGHTGRAY);
            step1.setWrapText(true);

            Label step2 = new Label("2. Click \"Authorize\" to grant access");
            step2.setFont(Font.font("Arial", 12));
            step2.setTextFill(Color.YELLOW);
            step2.setWrapText(true);

            Label step3 = new Label("3. Your patch file will be generated");
            step3.setFont(Font.font("Arial", 12));
            step3.setTextFill(Color.LIGHTGRAY);
            step3.setWrapText(true);

            Label step4 = new Label("4. File downloads automatically");
            step4.setFont(Font.font("Arial", 12));
            step4.setTextFill(Color.LIGHTGRAY);
            step4.setWrapText(true);

            Label arrow = new Label("\u27A1");
            arrow.setFont(Font.font("Arial", 40));
            arrow.setTextFill(Color.YELLOW);
            arrow.setAlignment(Pos.CENTER);

            sidebar.getChildren().addAll(title, step1, step2, step3, step4, arrow);

            HBox root = new HBox();
            HBox.setHgrow(webView, Priority.ALWAYS);
            root.getChildren().addAll(sidebar, webView);

            stage = new Stage();
            stage.setTitle("Discord — Echo VR Patcher");
            stage.setMinWidth(1150);
            stage.setMinHeight(680);
            Scene scene = new Scene(root, 1150, 680);
            stage.setScene(scene);
            stage.show();
            latch.countDown();
        });
        await(latch);
    }

    public void navigateTo(String url) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            webEngine.load(url);
            latch.countDown();
        });
        await(latch);
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

    public void close() {
        if (stage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                stage.close();
                latch.countDown();
            });
            await(latch);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for JavaFX operation", e);
        }
    }
}
