package apps;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.lang.reflect.Field;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/CardReaderUI.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }


    public static void main(String[] args) {
        initOpenCv();
        launch(args);
    }

    public static void initOpenCv() {

        setLibraryPath();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // System.out.println("OpenCV loaded. Version: " + Core.VERSION);

    }

    private static void setLibraryPath() {

        try {

            System.setProperty("java.library.path", "lib/x64");

            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }

    }
}
