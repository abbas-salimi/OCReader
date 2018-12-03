package reader;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {

    public static BufferedImage matToBufferedImage(Mat frame) {
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width() ,frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);
        return image;
    }

    public static Image bufferedImage2Image(BufferedImage biImage){
        return SwingFXUtils.toFXImage(biImage,null);
    }

    // contours

    public static ArrayList<Rect> detection_contours(Mat input) {

        // initialize objects
        Mat hierarchyVector = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat inputClone = input.clone();

        // find contours
        Imgproc.findContours(inputClone, contours, hierarchyVector, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_KCOS);

        // double inputArea = input.height() * input.width();
        // double maxArea = inputArea * 0.008;
        // double minArea = inputArea * 0.002;

        // System.out.println("Image Area : " + inputArea);
        // System.out.println("Image maxArea : " + maxArea);
        // System.out.println("Image minArea : " + minArea);

        // initialize parameters
        int inputHeight = input.height();
        int maxBlobHeight = (int) (inputHeight * 0.10);
        int minBlobHeight = (int) (inputHeight * 0.03);

        double maxArea = (maxBlobHeight * maxBlobHeight);
        double minArea = (minBlobHeight * minBlobHeight);

        int minThreshLoc = (int) (inputHeight * 0.50);
        int maxThreshLoc = (int) (inputHeight * 0.80);

        // System.out.println("Image Height : " + inputHeight);
        // System.out.println("Image maxHeight : " + maxBlobHeight);
        // System.out.println("Image minHeight : " + minBlobHeight);
        //
        // System.out.println("Image maxArea : " + maxArea);
        // System.out.println("Image minArea : " + minArea);

        Rect blob = null;
        ArrayList<Rect> rect_array = new ArrayList<Rect>();

        for (int idx = 0; idx < contours.size(); idx++) {
            // get blob

            blob = Imgproc.boundingRect(contours.get(idx));

            // System.out.println("Blob " + (idx + 1) + " information : [Height:" +
            // blob.height + "][Width:" + blob.width
            // + "][Area:" + blob.area() + "]");

            // check if blob height is more than minimum blob height
            if (blob.height >= minBlobHeight && blob.height <= maxBlobHeight) {
                // check if blob width is less than blob height and if the blob width is more
                // than minimum threshold
                if (blob.width < blob.height + 5 && blob.width > (blob.height * 0.08)) {
                    // check if area meet minimum area requirement
                    if (blob.area() > minArea && blob.area() < maxArea) {
                        // System.out.println("Object " + idx + " -- Height : " + blob.height + " Width
                        // : " + blob.width);

                        // get the center points only retain the middle ones
                        if(getRectCenterpointY(blob)>minThreshLoc && getRectCenterpointY(blob) < maxThreshLoc){
                            rect_array.add(blob);
                        }
                    }
                }
            }
        }

        if (rect_array != null) {
            // eliminate blob inside blob
            rect_array = EliminateNoisyRectangles(rect_array);
        }

        hierarchyVector.release();

        return rect_array;

    }

    public static boolean Overlaps2(Rect r1, Rect r2) {
        return r1.tl().x - 15 < r2.tl().x && r1.tl().y - 15 < r2.tl().y && r1.br().x + 15 > r2.br().x
                && r1.br().y + 15 > r2.br().y;
    }

    public static ArrayList<Rect> EliminateNoisyRectangles(ArrayList<Rect> rects) {

        ArrayList<Rect> toBeRemoved = new ArrayList<Rect>();
        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            for (int j = 0; j < rects.size(); j++) {
                Rect rectInner = rects.get(j);
                if (Overlaps2(rect, rectInner) && rect.area() > rectInner.area() && i != j) {
                    toBeRemoved.add(rects.get(j));
                }
            }
        }
        rects.removeAll(toBeRemoved);
        return rects;

    }

    public static int getRectCenterpointY(Rect rect){

        int centerpoint = (int)(rect.tl().y + (rect.br().y - rect.tl().y));

        return centerpoint;
    }

    public static List<List<Rect>> getGrouping(ArrayList<Rect> blobs){
        List<List<Rect>> listOfLists = new ArrayList<List<Rect>>();

         while(!blobs.isEmpty()){

            int firstLoc = getRectCenterpointY(blobs.get(0));

            ArrayList<Rect> first = new ArrayList<Rect>();

            ArrayList<Rect> second = new ArrayList<Rect>();

            ArrayList<Rect> splitFirst = new ArrayList<Rect>();

            ArrayList<Rect> splitSecond = new ArrayList<Rect>();



            for (Rect blob: blobs) {

                if(getRectCenterpointY(blob) < firstLoc +5 && getRectCenterpointY(blob) > firstLoc -5){
                    first.add(blob);
                }else{
                    second.add(blob);
                }

            }

            blobs.removeAll(first);
            blobs.removeAll(second);

            System.out.println("The blobs count are :" + blobs.size());
            System.out.println("The first count are :" + first.size());
            System.out.println("The second count are :" + second.size());

            if(first.size() == 16 || first.size() > 10){

                // sort first then add to the listOfLists

                first = sorted(first);

                listOfLists.add(first);

                // sort second, split then add to listOfLists

                second = sorted(second);

                listOfLists.add(second);

                splitter(listOfLists, second, splitFirst, splitSecond);


            }else{

                // sort second then add to the listOfLists

                second = sorted(second);

                listOfLists.add(second);


                // sort first, split then add to the listOfLists

                first = sorted(first);

                listOfLists.add(first);

                splitter(listOfLists, first, splitFirst, splitSecond);

            }
         }

        return listOfLists;
    }

    public static void splitter(List<List<Rect>> listOfLists, ArrayList<Rect> input,
                                ArrayList<Rect> splitFirst, ArrayList<Rect> splitSecond) {
        if(input.size() == 8){
            for (int a = 0; a < 4; a++){
                splitFirst.add(input.get(a));
            }

            input.removeAll(splitFirst);

            splitSecond.addAll(input);

            listOfLists.add(splitFirst);
            listOfLists.add(splitSecond);
        }
    }

    public static ArrayList<Rect> sorted(ArrayList<Rect> input){

        int rectArraySize = input.size();

        System.out.println("Size before " + rectArraySize);

        Rect blobA = null;
        Rect blobB = null;

        for (int i = 0; i < rectArraySize - 1; i++) {
            for (int j = i + 1; j < rectArraySize; j++) {

                blobA = input.get(i);
                blobB = input.get(j);

                // int centerA = (int) Math.abs(blobA.tl().x - blobA.br().x);
                // int centerB = (int) Math.abs(blobB.tl().x - blobB.br().x);

                if (blobA.x > blobB.x) {
                    Collections.swap(input, i, j);
                }
            }
        }

        return input;
    }


}
