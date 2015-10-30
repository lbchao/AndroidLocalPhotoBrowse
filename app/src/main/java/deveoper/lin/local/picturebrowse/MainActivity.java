package deveoper.lin.local.picturebrowse;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import deveoper.lin.local.picturebrowse.adapter.GirdViewAdapter;
import deveoper.lin.local.picturebrowse.entity.GirdViewEntity;
import deveoper.lin.local.picturebrowse.entity.PictureEntity;
import deveoper.lin.local.picturebrowse.entity.PictureFolderEntity;
import deveoper.lin.local.picturebrowse.widget.BottomPopWindow;

public class MainActivity extends AppCompatActivity implements BottomPopWindow.changeListener, PopupWindow.OnDismissListener {

    @InjectView(R.id.gird_view)
    GridView girdView;
    @InjectView(R.id.tv_left)
    TextView tvLeft;
    @InjectView(R.id.tv_right)
    TextView tvRight;
    @InjectView(R.id.rl_bottom)
    RelativeLayout rlBottom;

    public static final String TAG = "MainActivity";

    public static final int UPDATE_DATA = 1;
    @InjectView(R.id.btn_start)
    Button mBtnStart;
    private Uri imageUri;
    private ContentResolver contentResolver;
    private Cursor cursor;
    private Set<String> directoryPathList;
    //folder list
    private ArrayList<PictureFolderEntity> mfolderEntities = new ArrayList<>();
    private int currentFileCount;
    private File currentParentFile;
    private ProgressDialog mdialog;
    private GirdViewAdapter mGirdAdapter;
    private ArrayList<GirdViewEntity> mGirdList;
    private BottomPopWindow mPopWindow;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mdialog != null && mdialog.isShowing()) {
                mdialog.dismiss();
            }
            initData();
            initPopWindow();
        }
    };

    private void initData() {
        if (currentParentFile == null) {
            Toast.makeText(this, getString(R.string.no_photo), Toast.LENGTH_LONG).show();
            return;
        }
        mGirdList = getCurrentList();
        mGirdAdapter = new GirdViewAdapter(mGirdList, this);
        girdView.setAdapter(mGirdAdapter);
        initBottomViewData();
    }

    private ArrayList<GirdViewEntity> getCurrentList() {
        ArrayList<GirdViewEntity> list = new ArrayList<>();
        GirdViewEntity girdViewEntity = null;
        List<String> pathList = Arrays.asList(currentParentFile.list());
        for (int i = 0; i < pathList.size(); i++) {
            girdViewEntity = new GirdViewEntity();
            girdViewEntity.setIsSelected(false);
            girdViewEntity.setPath(pathList.get(i));
            girdViewEntity.setDirPath(currentParentFile.getAbsolutePath());
            list.add(girdViewEntity);
        }
        return list;
    }

    private void initBottomViewData() {
        tvLeft.setText(currentParentFile.getName());
        tvRight.setText(currentFileCount + "");
    }


    private void initPopWindow() {
        if (!mfolderEntities.isEmpty()) {
            mPopWindow = new BottomPopWindow(this, mfolderEntities);
            mPopWindow.setChangeListener(this);
            mPopWindow.setOnDismissListener(this);
        }
    }

    private void managerViewLightStatus(boolean isLightOn) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha = isLightOn ? 1.0f : 0.3f;
        getWindow().setAttributes(layoutParams);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.btn_start)
    public void onStartOperation() {
        getDataFromLocalStorage();
    }


    private void getDataFromLocalStorage() {
        imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        contentResolver = getContentResolver();
        mdialog = ProgressDialog.show(this, "", getString(R.string.load));
        EventBus.getDefault().post(new PictureEntity());
    }

    @Subscribe(threadMode = ThreadMode.Async)
    public void event(PictureEntity entity) {
        cursor = contentResolver.query(imageUri, null,
                MediaStore.Images.Media.MIME_TYPE
                        + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                new String[]{"image/png", "image/jpeg"}, MediaStore.Images.Media.DATE_MODIFIED);
        directoryPathList = new HashSet<>();
        //loop
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            File parentFile = new File(path).getParentFile();
            if (parentFile == null) {
                continue;
            }
            String direPath = parentFile.getAbsolutePath();
            PictureFolderEntity folderEntity = null;
            if (directoryPathList.contains(direPath)) {
                continue;
            } else {
                directoryPathList.add(direPath);
                folderEntity = new PictureFolderEntity();
                folderEntity.setDir(direPath);
                folderEntity.setFirstImagePath(path);
            }
            if (parentFile.list() == null) {
                continue;
            }
            int pictureCount = parentFile.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.endsWith(".jpg") ||
                            filename.endsWith(".jpeg") ||
                            filename.endsWith(".png")) {
                        return true;
                    }
                    return false;
                }
            }).length;
            if (folderEntity != null) {
                folderEntity.setDirPhotoCount(pictureCount);
                mfolderEntities.add(folderEntity);
                if (pictureCount > currentFileCount) {
                    currentFileCount = pictureCount;
                    currentParentFile = parentFile;
                }
            }
        }
        //get data over
        cursor.close();
        //update UI
        handler.sendEmptyMessage(UPDATE_DATA);
    }


    @Override
    protected void onStart() {
        super.onStart();
        EventBus.builder().logSubscriberExceptions(true);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @OnClick(R.id.rl_bottom)
    public void onClickBottomViewOperation() {
        if (mPopWindow != null) {
            mPopWindow.setAnimationStyle(R.style.pop_window_style);
            mPopWindow.showAsDropDown(rlBottom, 0, 0);
            managerViewLightStatus(false);
        }
    }


    @Override
    public void changeListener(PictureFolderEntity entity) {
        currentParentFile = new File(entity.getDir());
        mGirdList = getCurrentChangeList();
        mGirdAdapter = new GirdViewAdapter(mGirdList, this);
        girdView.setAdapter(mGirdAdapter);
        currentFileCount = mGirdList.size();
        initBottomViewData();
        if (mPopWindow != null) {
            mPopWindow.dismiss();
        }
    }

    private ArrayList<GirdViewEntity> getCurrentChangeList() {
        ArrayList<GirdViewEntity> list = new ArrayList<>();
        GirdViewEntity girdViewEntity = null;
        List<String> pathList = Arrays.asList(currentParentFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".jpg") ||
                        filename.endsWith(".jpeg") ||
                        filename.endsWith(".png")) {
                    return true;
                }
                return false;
            }
        }));
        for (int i = 0; i < pathList.size(); i++) {
            girdViewEntity = new GirdViewEntity();
            girdViewEntity.setIsSelected(false);
            girdViewEntity.setPath(pathList.get(i));
            girdViewEntity.setDirPath(currentParentFile.getAbsolutePath());
            list.add(girdViewEntity);
        }
        return list;
    }


    @Override
    public void onDismiss() {
        managerViewLightStatus(true);
    }
}
