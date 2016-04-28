package com.o3dr.droneit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.tvbarthel.lib.blurdialogfragment.BlurDialogFragment;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DialogFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DialogFragment extends BlurDialogFragment {
    /**
     * Bundle key used to start the blur dialog with a given scale factor (float).
     */
    private static final String BUNDLE_KEY_DOWN_SCALE_FACTOR = "bundle_key_down_scale_factor";

    /**
     * Bundle key used to start the blur dialog with a given blur radius (int).
     */
    private static final String BUNDLE_KEY_BLUR_RADIUS = "bundle_key_blur_radius";

    /**
     * Bundle key used to start the blur dialog with a given dimming effect policy.
     */
    private static final String BUNDLE_KEY_DIMMING = "bundle_key_dimming_effect";

    /**
     * Bundle key used to start the blur dialog with a given debug policy.
     */
    private static final String BUNDLE_KEY_DEBUG = "bundle_key_debug_effect";

    private static final String BUNDLE_KEY_TIME = "bundle_key_time";
    private static final String BUNDLE_KEY_WON = "bundle_key_won";
    private static final String BUNDLE_KEY_LEVEL = "bundle_key_level";

    private int mRadius;
    private float mDownScaleFactor;
    private boolean mDimming;
    private boolean mDebug;
    private int time;
    private boolean isWon;
    private int level;

    /**
     * Retrieve a new instance of the sample fragment.
     *
     * @param radius          blur radius.
     * @param downScaleFactor down scale factor.
     * @param dimming         dimming effect.
     * @param debug           debug policy.
     * @return well instantiated fragment.
     */
    public static DialogFragment newInstance(int radius, float downScaleFactor, boolean dimming, boolean debug, int time, boolean isWon, int level) {
        DialogFragment fragment = new DialogFragment();
        Bundle args = new Bundle();
        args.putInt(
                BUNDLE_KEY_BLUR_RADIUS,
                radius
        );
        args.putFloat(
                BUNDLE_KEY_DOWN_SCALE_FACTOR,
                downScaleFactor
        );
        args.putBoolean(
                BUNDLE_KEY_DIMMING,
                dimming
        );
        args.putBoolean(
                BUNDLE_KEY_DEBUG,
                debug
        );
        args.putInt(
                BUNDLE_KEY_TIME,
                time
        );
        args.putBoolean(
                BUNDLE_KEY_WON,
                isWon
        );
        args.putInt(
                BUNDLE_KEY_LEVEL,
                level
        );

        fragment.setArguments(args);

        return fragment;
    }

    public DialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Bundle args = getArguments();
        mRadius = args.getInt(BUNDLE_KEY_BLUR_RADIUS);
        mDownScaleFactor = args.getFloat(BUNDLE_KEY_DOWN_SCALE_FACTOR);
        mDimming = args.getBoolean(BUNDLE_KEY_DIMMING);
        mDebug = args.getBoolean(BUNDLE_KEY_DEBUG);
        time = args.getInt(BUNDLE_KEY_TIME);
        isWon = args.getBoolean(BUNDLE_KEY_WON);
        level = args.getInt(BUNDLE_KEY_LEVEL);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog, null);

        Button tryAgainBT = ((Button) view.findViewById(R.id.tryAgainBT));
        tryAgainBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTryAgainClicked(v);
            }
        });

        Button finishBT = ((Button) view.findViewById(R.id.finishBT));
        finishBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinishClicked(v);
            }
        });

        TextView time = ((TextView) view.findViewById(R.id.dialogTime));
        time.setText(getString(R.string.dialog_time_title) + " " + this.time + "s");

        TextView title = ((TextView) view.findViewById(R.id.dialogTitle));
        if(isWon)
            title.setText(getString(R.string.dialog_won_title));
        else
            title.setText(getString(R.string.dialog_gameOver_title));

        builder.setView(view);

        // Prevent go back
        builder.setCancelable(false);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return true;
                }
            });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        return dialog;
    }

    public void onTryAgainClicked(View view) {
        final Intent intent = new Intent(getActivity(), GameActivity.class);
        Bundle bundle = new Bundle();

        bundle.putInt(GameActivity.levelKey, level);
        intent.putExtra(GameActivity.gameBundleKey, bundle);
        startActivity(intent);

        this.dismiss();
    }

    public void onFinishClicked(View view) {
        final Intent intent = new Intent(getActivity(), LevelsActivity.class);
        startActivity(intent);

        this.dismiss();
    }

    @Override
    protected boolean isDebugEnable() {
        return mDebug;
    }

    @Override
    protected boolean isDimmingEnable() {
        return mDimming;
    }

    @Override
    protected boolean isActionBarBlurred() {
        return true;
    }

    @Override
    protected float getDownScaleFactor() {
        return mDownScaleFactor;
    }

    @Override
    protected int getBlurRadius() {
        return mRadius;
    }
}
