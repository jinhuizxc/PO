package cn.com.shengchuang.po;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    //使用照相机拍照获取图片
    public static final int TAKE_PHOTO_CODE = 1;
    //使用相册中的图片
    public static final int SELECT_PIC_CODE = 2;
    //图片裁剪
    private static final int PHOTO_CROP_CODE = 3;
    //定义图片的Uri
    private Uri photoUri;
    //图片文件路径
    private String picPath;
    private MainActivity context;
    private PopupWindow mPopWindow;
    private ImageView showImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;
        showImageView = (ImageView) findViewById(R.id.iv_show);

    }

    public void onClick(View view) {
        showUploadAvatarDialog();
    }


    /**
     * 展示popuWindow
     */
    private void showUploadAvatarDialog() {

        View view = getLayoutInflater().inflate(R.layout.popu_photo, null);
        int screenWith = context.getWindowManager().getDefaultDisplay().getWidth();
        int screenHeiht = context.getWindowManager().getDefaultDisplay().getHeight();
        mPopWindow = new PopupWindow(view, screenWith, screenHeiht);
        ColorDrawable dw = new ColorDrawable(0xb0000000);
        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
        mPopWindow.setBackgroundDrawable(dw);
        mPopWindow.setTouchable(true);
        mPopWindow.setFocusable(true);
        mPopWindow.showAtLocation(showImageView, Gravity.BOTTOM, 0, 0);
        mPopWindow.setOutsideTouchable(true);
        TextView takePhoto = (TextView) view.findViewById(R.id.tv_take_photo_popu);
        TextView photoAlbum = (TextView) view.findViewById(R.id.tv_photo_album_popu);
        TextView cancel = (TextView) view.findViewById(R.id.tv_photo_cancle_popu);


        takePhoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mPopWindow.isShowing()) {
                    mPopWindow.dismiss();
                    picTyTakePhoto();
                }
                return false;
            }
        });
        photoAlbum.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mPopWindow.isShowing()) {
                    mPopWindow.dismiss();
                    pickPhoto();
                }
                return false;
            }
        });

        cancel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mPopWindow.isShowing()) {
                    mPopWindow.dismiss();

                }
                return false;
            }
        });
    }


    /**
     * 拍照获取图片
     */
    private void picTyTakePhoto() {
        //判断SD卡是否存在
        String SDState = Environment.getExternalStorageState();
        if (SDState.equals(Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//"android.media.action.IMAGE_CAPTURE"
            /***
             * 使用照相机拍照，拍照后的图片会存放在相册中。使用这种方式好处就是：获取的图片是拍照后的原图，
             * 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图有可能不清晰
             */
            ContentValues values = new ContentValues();
            photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, TAKE_PHOTO_CODE);
        } else {
            Toast.makeText(this, "内存卡不存在", Toast.LENGTH_LONG).show();
        }
    }

    /***
     * 从相册中取图片
     */
    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_PIC_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            //从相册取图片，有些手机有异常情况，请注意
            if (requestCode == SELECT_PIC_CODE) {
                if (null != data && null != data.getData()) {
                    photoUri = data.getData();
                    picPath = uriToFilePath(photoUri);
                    startPhotoZoom(photoUri, PHOTO_CROP_CODE);
                } else {
                    Toast.makeText(this, "图片选择失败", Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == TAKE_PHOTO_CODE) {
                String[] pojo = {MediaStore.Images.Media.DATA};
                Cursor cursor = managedQuery(photoUri, pojo, null, null, null);
                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(pojo[0]);
                    cursor.moveToFirst();
                    picPath = cursor.getString(columnIndex);
                    if (Build.VERSION.SDK_INT < 14) {
                        cursor.close();
                    }
                }
                if (picPath != null) {
                    photoUri = Uri.fromFile(new File(picPath));
                    startPhotoZoom(photoUri, PHOTO_CROP_CODE);
                } else {
                    Toast.makeText(this, "图片选择失败", Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == PHOTO_CROP_CODE) {
                if (photoUri != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(picPath);
                    if (bitmap != null) {
                        //这里可以把图片进行上传到服务器操作
//                        avatarImageView.setImageBitmap(bitmap);
                        showImageView.setImageBitmap(bitmap);
                    }
                }
            }
        }
    }

    /**
     * @param
     * @description 裁剪图片
     * @author ldm
     * @time
     */
    private void startPhotoZoom(Uri uri, int REQUE_CODE_CROP) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // 去黑边
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        // aspectX aspectY 是宽高的比例，根据自己情况修改
        intent.putExtra("aspectX", 3);
        intent.putExtra("aspectY", 2);
        // outputX outputY 是裁剪图片宽高像素
        intent.putExtra("outputX", 1280);
        intent.putExtra("outputY", 720);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        //取消人脸识别功能
        intent.putExtra("noFaceDetection", true);
        //设置返回的uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        //设置为不返回数据
        intent.putExtra("return-data", false);
        startActivityForResult(intent, REQUE_CODE_CROP);
    }

    /**
     * @param
     * @description 把Uri转换为文件路径
     * @author ldm
     * @time
     */
    private String uriToFilePath(Uri uri) {
        //获取图片数据
        String[] proj = {MediaStore.Images.Media.DATA};
        //查询
        Cursor cursor = managedQuery(uri, proj, null, null, null);
        //获得用户选择的图片的索引值
        int image_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        //返回图片路径
        return cursor.getString(image_index);
    }

}
