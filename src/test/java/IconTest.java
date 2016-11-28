import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class IconTest extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  private static Image generateIcon(int dimension) throws IOException {
    Canvas canvas = new Canvas(dimension, dimension);
    canvas.getGraphicsContext2D().setFill(Color.RED);
    canvas.getGraphicsContext2D().fill();
    SnapshotParameters params = new SnapshotParameters();
    params.setFill(Color.RED);
    return canvas.snapshot(params, new WritableImage(dimension, dimension));
  }

  private static Image generateImage(int dimension) {
    BufferedImage appIcon = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
    Graphics2D appIconGraphics = appIcon.createGraphics();
    appIconGraphics.setColor(java.awt.Color.RED);
    appIconGraphics.fillRect(0, 0, dimension, dimension);

    appIconGraphics.setColor(java.awt.Color.WHITE);
    WritableImage fxIcon = new WritableImage(appIcon.getWidth(), appIcon.getHeight());
    return SwingFXUtils.toFXImage(appIcon, fxIcon);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    Scene scene = new Scene(new StackPane(new ImageView()));
    primaryStage.setScene(scene);
    primaryStage.getIcons().setAll(generateIcon(32));
    primaryStage.show();
  }
}
