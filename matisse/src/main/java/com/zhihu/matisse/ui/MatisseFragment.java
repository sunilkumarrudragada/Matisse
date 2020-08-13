package com.zhihu.matisse.ui;

import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.CaptureStrategy;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.AlbumCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity;
import com.zhihu.matisse.internal.ui.BasePreviewActivity;
import com.zhihu.matisse.internal.ui.MediaSelectionFragment;
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter;
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner;
import com.zhihu.matisse.internal.ui.widget.CheckRadioView;
import com.zhihu.matisse.internal.ui.widget.IncapableDialog;
import com.zhihu.matisse.internal.utils.MediaStoreCompat;
import com.zhihu.matisse.internal.utils.PathUtils;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;
import com.zhihu.matisse.internal.utils.SingleMediaScanner;
import com.zhihu.matisse.listener.OnSelectedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MatisseFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MatisseFragment extends Fragment implements
        AlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener, View.OnClickListener, AlbumMediaAdapter.OnMediaClickListener {

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";
    public static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    private static final int REQUEST_CODE_PREVIEW = 23;
    public static final int REQUEST_CODE_CAPTURE = 24;
    private static final String IMAGE_COUNT = "IMAGE_COUNT";
    private static final String MIME_TYPE = "MIME_TYPE";
    public static final String IMAGE_TYPE = "IMAGE_TYPE";
    public static final String VIDEO_TYPE = "VIDEO_TYPE";
    public static final String IMAGE_VIDEO = "IMAGE_VIDEO";
    public static final String NIGHT_THEME = "NIGHT_THEME";
    public static final String LIGHT_THEME = "LIGHT_THEME";
    public static final String THEME = "LIGHT_THEME";
    public static final String TOTAL_COUNT = "TOTAL_COUNT";
    public static final String SHOW_CAMERA = "SHOW_CAMERA";
    public static final String IS_PUBLIC = "IS_PUBLIC";
    public static final String AUTHORITY_NAME = "AUTHORITY_NAME";
    public static final String DIRECTORY = "DIRECTORY";

    public static final String TAG = "MatisseFragment";
    public static final int RESULT_CANCELED    = 0;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String CHECK_STATE = "checkState";
    private final AlbumCollection mAlbumCollection = new AlbumCollection();
    private AlbumsSpinner mAlbumsSpinner;
    private AlbumsAdapter mAlbumsAdapter;
    private View mContainer;
    private View mEmptyView;
    private SelectionSpec mSpec;
    private SelectedItemCollection mSelectedCollection;
    private MediaStoreCompat mMediaStoreCompat;
    private TextView mButtonPreview;
    private TextView mButtonApply;
    private LinearLayout mOriginalLayout;
    private CheckRadioView mOriginal;
    private boolean mOriginalEnable;
    private TextView requiredCountText;
    public Fragment fragment;
    View view;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MatisseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MatisseFragment newInstance(int count, int totalCount, String type, String theme) {
        MatisseFragment fragment = new MatisseFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IMAGE_COUNT, count);
        bundle.putString(MIME_TYPE, type);
        bundle.putString(THEME, theme);
        bundle.putInt(TOTAL_COUNT, totalCount);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static MatisseFragment newInstance(int count, int totalCount, String type, String theme, boolean showCamera, boolean isPublic, String authority, String directory) {
        MatisseFragment fragment = new MatisseFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IMAGE_COUNT, count);
        bundle.putString(MIME_TYPE, type);
        bundle.putString(THEME, theme);
        bundle.putInt(TOTAL_COUNT, totalCount);
        bundle.putBoolean(SHOW_CAMERA, showCamera);
        bundle.putBoolean(IS_PUBLIC, isPublic);
        bundle.putString(AUTHORITY_NAME, authority);
        bundle.putString(DIRECTORY, directory);

        fragment.setArguments(bundle);
        return fragment;
    }

    public MatisseFragment() {
        // Required empty public constructor
    }

    private void onAlbumSelected(Album album) {
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mContainer.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            fragment = MediaSelectionFragment.newInstance(album);
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, MediaSelectionFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mSelectedCollection = new SelectedItemCollection(getActivity());
        mSpec = SelectionSpec.getCleanInstance();
        mSpec.themeId = R.style.Night_Theme;
        mSpec.showPreview = false;
        mSpec.mediaTypeExclusive = false;
        if (getArguments() != null) {
            mSpec.maxSelectable = getArguments().getInt(IMAGE_COUNT);
            mSpec.capture = getArguments().getBoolean(SHOW_CAMERA, false);
            if (mSpec.capture) {
                mSpec.captureStrategy = new CaptureStrategy(getArguments().getBoolean(IS_PUBLIC, false ), getArguments().getString(AUTHORITY_NAME), getArguments().getString(DIRECTORY));
            }
            String mimType = getArguments().getString(MIME_TYPE);
            String theme = getArguments().getString(THEME);
            // select mimType
            if (mimType == IMAGE_TYPE) {
                mSpec.mimeTypeSet = MimeType.ofImage();
                mSpec.showSingleMediaType = true;
            } else if (mimType == VIDEO_TYPE) {
                mSpec.mimeTypeSet = MimeType.ofVideo();
                mSpec.showSingleMediaType = true;
            } else {
                mSpec.mimeTypeSet = MimeType.ofAll();
            }

            // theme
            if (theme == LIGHT_THEME) {
                mSpec.themeId = R.style.Light_Theme;
            }
        }
        mSpec.orientation = SCREEN_ORIENTATION_UNSPECIFIED;
        mSpec.countable = false;
        mSpec = SelectionSpec.getInstance();
        getActivity().setTheme(mSpec.themeId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       view =  inflater.inflate(R.layout.fragment_matisse, container, false);
        requiredCountText = view.findViewById(R.id.album_media_required_count);
        requiredCountText.setText("Select " +  getArguments().getInt(TOTAL_COUNT) + " Images");
        if (!mSpec.hasInited) {
            getActivity().setResult(RESULT_CANCELED);
            getActivity().finish();
            return null;
        }

        if (mSpec.needOrientationRestriction()) {
            getActivity().setRequestedOrientation(mSpec.orientation);
        }

        if (mSpec.capture) {
            mMediaStoreCompat = new MediaStoreCompat(getActivity());
            if (mSpec.captureStrategy == null)
                throw new RuntimeException("Don't forget to set CaptureStrategy.");
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy);
        }
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()). setSupportActionBar(toolbar);
        ActionBar actionBar =((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Drawable navigationIcon = toolbar.getNavigationIcon();
        TypedArray ta = getActivity(). getTheme().obtainStyledAttributes(new int[]{R.attr.album_element_color});
        int color = ta.getColor(0, 0);
        ta.recycle();
        navigationIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);

        mButtonPreview = (TextView) view.findViewById(R.id.button_preview);
        mButtonApply = (TextView) view.findViewById(R.id.button_apply);
        mButtonPreview.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mContainer = view.findViewById(R.id.container);
        mEmptyView = view.findViewById(R.id.empty_view);

        mOriginalLayout = view.findViewById(R.id.originalLayout);
        mOriginal = view.findViewById(R.id.original);
        mOriginalLayout.setOnClickListener(this);

        mSelectedCollection.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        updateBottomToolbar();

        mAlbumsAdapter = new AlbumsAdapter(getActivity(), null, false);
//        mAlbumsSpinner = new AlbumsSpinner(getActivity());
//        mAlbumsSpinner.setOnItemSelectedListener(this);
//        mAlbumsSpinner.setSelectedTextView((TextView) view.findViewById(R.id.selected_album));
//        mAlbumsSpinner.setPopupAnchorView(view.findViewById(R.id.toolbar));
//        mAlbumsSpinner.setAdapter(mAlbumsAdapter);
        mAlbumCollection.onCreate(getActivity(), this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mAlbumCollection.loadAlbums();
        toolbar.setNavigationIcon(null);

        // Inflate the layout for this fragment
        return view;
    }

    private void updateBottomToolbar() {

        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonPreview.setEnabled(false);
            mButtonApply.setEnabled(false);
            mButtonApply.setText(getString(R.string.button_apply_default));
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonPreview.setEnabled(true);
            mButtonApply.setText(R.string.button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonPreview.setEnabled(true);
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_apply, selectedCount));
        }


        if (mSpec.originalable) {
            mOriginalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mOriginalLayout.setVisibility(View.INVISIBLE);
        }


    }

    private void updateOriginalState() {
        mOriginal.setChecked(mOriginalEnable);
        if (countOverMaxSize() > 0) {

            if (mOriginalEnable) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec.originalMaxSize));
                incapableDialog.show(getActivity().getSupportFragmentManager(),
                        IncapableDialog.class.getName());

                mOriginal.setChecked(false);
                mOriginalEnable = false;
            }
        }
    }

    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            Item item = mSelectedCollection.asList().get(i);

            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMB(item.size);
                if (size > mSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_preview) {
            Intent intent = new Intent(getActivity(), SelectedPreviewActivity.class);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        } else if (v.getId() == R.id.button_apply) {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            getActivity().setResult(getActivity().RESULT_OK, result);
            getActivity().finish();
        } else if (v.getId() == R.id.originalLayout) {
            int count = countOverMaxSize();
            if (count > 0) {
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_count, count, mSpec.originalMaxSize));
                incapableDialog.show(getActivity().getSupportFragmentManager(),
                        IncapableDialog.class.getName());
                return;
            }

            mOriginalEnable = !mOriginalEnable;
            mOriginal.setChecked(mOriginalEnable);

            if (mSpec.onCheckedListener != null) {
                mSpec.onCheckedListener.onCheck(mOriginalEnable);
            }
        }
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mAlbumCollection.setStateCurrentSelection(position);
        mAlbumsAdapter.getCursor().moveToPosition(position);
        Album album = Album.valueOf(mAlbumsAdapter.getCursor());
        if (album.isAll() && SelectionSpec.getInstance().capture) {
            album.addCaptureCount();
        }
        onAlbumSelected(album);
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onAlbumLoad(Cursor cursor) {
        mAlbumsAdapter.swapCursor(cursor);
        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                cursor.moveToPosition(mAlbumCollection.getCurrentSelection());
//                mAlbumsSpinner.setSelection(getActivity(),
//                        mAlbumCollection.getCurrentSelection());
                Album album = Album.valueOf(cursor);
                if (album.isAll() && SelectionSpec.getInstance().capture) {
                    album.addCaptureCount();
                }
                onAlbumSelected(album);
            }
        });
    }

    @Override
    public void onAlbumReset() {
        mAlbumsAdapter.swapCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlbumCollection.onDestroy();
        mSpec.onCheckedListener = null;
        mSpec.onSelectedListener = null;
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        Intent intent = new Intent(getActivity(), AlbumPreviewActivity.class);
        intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album);
        intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item);
        intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
        startActivityForResult(intent, REQUEST_CODE_PREVIEW);
    }

    public void capture() {
        if (mMediaStoreCompat != null) {
            mMediaStoreCompat.dispatchCaptureIntent(getActivity(), REQUEST_CODE_CAPTURE);
        }
    }

    public SelectedItemCollection getmSelectedCollection() {
        return mSelectedCollection;
    }

    public List<Uri> getSelectedList() {
        ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
        return selectedUris;
    }

    public List<String> getSelectedPaths() {
        ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();
        return selectedPaths;
    }

    public HashMap<Integer, String> getSelectedPathsWithPostions() {
        HashMap<Integer, String> mHashMap = mSelectedCollection.asHashMap();
        return mHashMap;
    }

    public void unSelectItem(String value) {
        HashMap<Integer, String> mHashMap = mSelectedCollection.asHashMap();
        if (mHashMap != null && mHashMap.size() > 0) {
            for (int o : mHashMap.keySet()) {
                if (mHashMap.get(o).equals(value)) {
                   delete(o);
                }
            }
        }
    }

    private void delete(int position) {
        for (int i = 0; i < mSelectedCollection.getmItems().size(); i++) {
            if (mSelectedCollection.getmItems().get(i).viewHolderPosition == position) {
                RecyclerView.ViewHolder viewHolderForAdapterPosition = ((MediaSelectionFragment) fragment).mRecyclerView.findViewHolderForAdapterPosition(position);
                ((MediaSelectionFragment) fragment).unSelect(mSelectedCollection.getmItems().get(i), viewHolderForAdapterPosition);
                break;
            }
        }
    }

    public String capturedImage() {
        return mMediaStoreCompat.getCurrentPhotoPath();
    }

    public String removedItem() {
        return mSelectedCollection.removedItemPath;
    }
}