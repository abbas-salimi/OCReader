package apps;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import reader.Utils;


import org.datavec.image.loader.NativeImageLoader;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Controller.class);

    public static Scalar SCALAR_BLACK = new Scalar(0.0, 0.0, 0.0);
    public static Scalar SCALAR_WHITE = new Scalar(255.0, 255.0, 255.0);
    public static Scalar SCALAR_YELLOW =new Scalar(0.0, 255.0, 255.0);
    public static Scalar SCALAR_GREEN = new Scalar(0.0, 200.0, 0.0);
    public static Scalar SCALAR_RED =   new Scalar(0.0, 0.0, 255.0);
    public static Scalar SCALAR_BLUE =   new Scalar(255.0, 0.0, 0.0);

    private MultiLayerNetwork net;

    @FXML
    Button btnLoadBankCard;

    @FXML
    TextField tfBankCardNumber;

    @FXML
    ImageView imgView1;
    @FXML
    ImageView imgView2;
    @FXML
    ImageView imgView3;
    @FXML
    ImageView imgView4;


    @FXML
    StackPane stackPane1;
    @FXML
    StackPane stackPane2;
    @FXML
    StackPane stackPane3;
    @FXML
    StackPane stackPane4;

    @FXML
    public void onClickBtnLoadBankCard() {

        Runnable r = () -> {



            Mat matSource = Imgcodecs.imread("test.png");

            Mat matGray = new Mat(matSource.rows(),matSource.cols(), matSource.channels());

            Mat matPreprocess = new Mat(matSource.rows(),matSource.cols(), matSource.channels());

            Mat matContour = matSource.clone();



            Imgproc.threshold(matSource,matPreprocess, 70,255,Imgproc.THRESH_BINARY);

//            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
//                    new  Size(5, 5));


            Imgproc.cvtColor(matPreprocess, matGray, Imgproc.COLOR_RGB2GRAY);

            // just do any preprocessing that you want

//            Imgproc.GaussianBlur(matGray, matGray,new Size(45,45), 3);

//            Imgproc.blur(matGray, matGray,new Size(45,45));

//            Imgproc.dilate(matGray,matGray,kernel);
//            Imgproc.erode(matGray, matGray, kernel);

//                Imgproc.adaptiveThreshold(matGray, matPreprocess, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
//                        Imgproc.THRESH_BINARY, 9, 9);

//            Imgproc.threshold(matGray,matPreprocess, 45,255,Imgproc.THRESH_BINARY);

//            Imgproc.equalizeHist(matGray, matGray);

            Imgproc.threshold(matGray,matPreprocess, 0,255,Imgproc.THRESH_OTSU|Imgproc.THRESH_BINARY);

            // get the contours
            ArrayList<Rect> contours = Utils.detection_contours(matPreprocess);

            // grouping of rects
            List<List<Rect>> listOfList = Utils.getGrouping(contours);

            try {

                List<Rect> bankCardNumber = listOfList.get(0);

                String predictCardNumber = "";

                for (Rect rect: bankCardNumber) {

                    Mat candidate = matPreprocess.submat(new Rect(new Point(rect.tl().x-9, rect.tl().y-9),
                            new Point(rect.br().x+9, rect.br().y+9)));


                    if(rect.height > rect.width){
                        Mat sourcePadding = new Mat(candidate.size(),candidate.type());
                        int borderValue = Math.abs((candidate.width() - candidate.height())/2);

                        Core.copyMakeBorder(candidate, sourcePadding, 0, 0, borderValue,
                                borderValue, Core.BORDER_CONSTANT,SCALAR_BLACK);

                    }else if(rect.height < rect.width){
                        Mat sourcePadding = new Mat(candidate.size(),candidate.type());
                        int borderValue = Math.abs((candidate.width() - candidate.height())/2);

                        Core.copyMakeBorder(candidate, sourcePadding, borderValue, borderValue, 0, 0,
                                Core.BORDER_CONSTANT, SCALAR_BLACK);

                    }

                    Mat sourceResize = new Mat (new Size(28,28), candidate.type());
                    Imgproc.resize(candidate, sourceResize, sourceResize.size(), 0, 0, Imgproc.INTER_AREA );

                    NativeImageLoader loader = new NativeImageLoader(28, 28, 1, true);
                    INDArray image = loader.asRowVector(Utils.matToBufferedImage(sourceResize));
                    //ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(1, 0);
                    //scaler.transform(image);
                    INDArray output = net.output(image);

                    predictCardNumber = predictCardNumber + net.predict(image)[0];


                    Imgproc.rectangle(matContour, rect.tl(), rect.br(), SCALAR_GREEN, 2);
                }

                tfBankCardNumber.setText(predictCardNumber);

                List<Rect> year = listOfList.get(1);

                for (Rect rect: year) {
                    Imgproc.rectangle(matContour, rect.tl(), rect.br(), SCALAR_YELLOW, 2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // convert the mat to image
            Image imgSource = Utils.bufferedImage2Image(Utils.matToBufferedImage(matSource));
            Image imgPreprocess = Utils.bufferedImage2Image(Utils.matToBufferedImage(matPreprocess));
            Image imgContour = Utils.bufferedImage2Image(Utils.matToBufferedImage(matContour));

            Platform.runLater(()->{
                // set it to ui
                imgView1.setImage(imgSource);
                imgView2.setImage(imgPreprocess);
                imgView3.setImage(imgContour);
            });
        };

        Thread thread = new Thread(r);
        thread.start();

    }

    @FXML public void initialize(){
        // some init setups
        imgView1.fitWidthProperty().bind(stackPane1.widthProperty());
        imgView1.fitHeightProperty().bind(stackPane1.heightProperty());

        imgView2.fitWidthProperty().bind(stackPane2.widthProperty());
        imgView2.fitHeightProperty().bind(stackPane2.heightProperty());

        imgView3.fitWidthProperty().bind(stackPane3.widthProperty());
        imgView3.fitHeightProperty().bind(stackPane3.heightProperty());

        // load the network
        try {
            File model = new File("lib/mnist/minist-model.zip");
            System.out.println("Filepath :" +model.getPath());
            if (!model.exists())
                throw new IOException("Can't find the model");
            net = ModelSerializer.restoreMultiLayerNetwork(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
