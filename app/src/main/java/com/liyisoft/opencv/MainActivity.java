package com.liyisoft.opencv;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.liyisoft.opencv.utils.BitmapUtils;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * OpenCV 抠图测试(貌似效果并不好)
 * 抠图 https://blog.csdn.net/hardWork_yulu/article/details/78757665
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = MainActivity.class.getSimpleName();

//    private ImageView img_show;
    private Button select;
    private Button cut;
    private Button modify;
    private Button saveImage;
//    private Button btn_gray_test;
//    private Button btn_cut;
    private Bitmap resultBitmap;
    MyCropView cropView;
    private ImageView choiceView;
    static final int REQUEST_OPEN_IMAGE = 1;
    boolean targetChose = false;
    String mCurrentPhotoPath;

    private Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLoaderOpenCV();
        bitmap = BitmapUtils.getBitmapByAssetsNameRGB(this, "photo.jpeg");
//        img_show = this.findViewById(R.id.img_show);
//        btn_gray_test = this.findViewById(R.id.btn_gray_test);
//        btn_cut = this.findViewById(R.id.btn_cut);
        cropView = (MyCropView) findViewById(R.id.myCropView);
        select = (Button) findViewById(R.id.btn_gray_process);
        cut = (Button) findViewById(R.id.btn_cut_process);
        modify = (Button) findViewById(R.id.btn_modify_process);
        saveImage = (Button) findViewById(R.id.btn_save_process);
        choiceView = (ImageView) findViewById(R.id.croppedImageView);
//        btn_gray_test.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                gray();
//            }
//        });
//        btn_cut.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cupBitmap(resultBitmap,0,0,resultBitmap.getWidth(),resultBitmap.getHeight());
//            }
//        });
        select.setOnClickListener(this);
        cut.setOnClickListener(this);
        saveImage.setOnClickListener(this);
        modify.setOnClickListener(this);
    }


    ///======================

    /**
     * 初始化OpenCv组件
     */
    private void initLoaderOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        if (!success) {
            Log.d(TAG, "初始化失败");
        }
    }

    private Bitmap bitmap;

    /**
     * 灰度测试-测试通过
     */
    public void gray() {
        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGRA2GRAY);
        resultBitmap = getResultBitmap();
        Utils.matToBitmap(dst, resultBitmap);
        src.release();
        dst.release();

//        img_show.setImageBitmap(resultBitmap);

    }

    //获取bitmap
    private Bitmap getResultBitmap() {
        return Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
    }

//    //选择剪切区域
//    private void selectImageCut(){
//        targetChose = true;
//        try{
//            Bitmap cropBitmap = cropView.getCroppedImage();
//            choiceView.setImageBitmap(cropBitmap);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }


    private Bitmap cupBitmap(Bitmap bitmap, int x, int y, int width, int height) {
        Mat img = new Mat();
        //缩小图片尺寸
        // Bitmap bm = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth(),bitmap.getHeight(),true);
        //bitmap->mat
        Utils.bitmapToMat(bitmap, img);
        //转成CV_8UC3格式
        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGBA2RGB);
        //设置抠图范围的左上角和右下角
        Rect rect = new Rect(x, y, width, height);
        //生成遮板
        Mat firstMask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
        //这是实现抠图的重点，难点在于rect的区域，为了选取抠图区域，我借鉴了某大神的自定义裁剪View，返回坐标和宽高
        Imgproc.grabCut(img, firstMask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(firstMask, source, firstMask, Core.CMP_EQ);

        //抠图
        Mat foreground = new Mat(img.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        img.copyTo(foreground, firstMask);

        //mat->bitmap
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(foreground, bitmap1);
        return bitmap1;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gray_process:
                Intent getPictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getPictureIntent.setType("image/*");
                Intent pickPictureIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooserIntent = Intent.createChooser(getPictureIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{
                        pickPictureIntent
                });
                startActivityForResult(chooserIntent, REQUEST_OPEN_IMAGE);
                break;
            case R.id.btn_cut_process:
                //抠图是耗时的过程，子线程中运行，并dialog提示
                if (targetChose) {
                    final RectF croppedBitmapData = cropView.getCroppedBitmapData();
                    final int croppedBitmapWidth = cropView.getCroppedBitmapWidth();
                    final int croppedBitmapHeight = cropView.getCroppedBitmapHeight();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Bitmap bitmap = cupBitmap(originalBitmap, (int) croppedBitmapData.left, (int) croppedBitmapData.top, croppedBitmapWidth, croppedBitmapHeight);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    hasCut = true;
                                    choiceView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    }).start();

                }
                break;
            case R.id.btn_modify_process:
                selectImageCut();
                break;
            case R.id.btn_save_process:
//                if (hasCut) {
//                    String s = saveImageToGalleryString(this, ((BitmapDrawable) (choiceView).getDrawable()).getBitmap());
//                    Toast.makeText(this, "保存在" + s, Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(this, "请先扣图", Toast.LENGTH_SHORT).show();
//                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OPEN_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imgUri = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(imgUri, filePathColumn,
                            null, null, null);
                    cursor.moveToFirst();

                    int colIndex = cursor.getColumnIndex(filePathColumn[0]);
                    mCurrentPhotoPath = cursor.getString(colIndex);
                    cursor.close();
                    setPic();
                }
                break;
        }
    }

    //从图库中选择图片
    public void setPic() {
        originalBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        cropView.setBmpPath(mCurrentPhotoPath);
    }

    //选择剪切区域
    private void selectImageCut() {
        targetChose = true;
        try {
            Bitmap cropBitmap = cropView.getCroppedImage();
            choiceView.setImageBitmap(cropBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
