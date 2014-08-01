package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "OCVSample::Activity";
	private static final Scalar EYES_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	public static final int NATIVE_DETECTOR = 1;

	private MenuItem mItemEyes50;
	private MenuItem mItemEyes40;
	private MenuItem mItemEyes30;
	private MenuItem mItemEyes20;
	private MenuItem mItemType;

	private static final int TM_SQDIFF = 0;
	private static final int TM_SQDIFF_NORMED = 1;
	private static final int TM_CCOEFF = 2;
	private static final int TM_CCOEFF_NORMED = 3;
	private static final int TM_CCORR = 4;
	private static final int TM_CCORR_NORMED = 5;

	private int learn_frames = 0;

	private Mat mRgba;
	private Mat mGray;
	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private DetectionBasedTracker mNativeDetector;

	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeEyesSize = 0.2f;
	private int mAbsoluteEyesSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Load native library after(!) OpenCV initialization
				System.loadLibrary("detection_based_tracker");

				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(
							R.raw.haarcascade_eye_tree_eyeglasses);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir,
							"haarcascade_eye_tree_eyeglasses.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					mJavaDetector = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else
						Log.i(TAG, "Loaded cascade classifier from "
								+ mCascadeFile.getAbsolutePath());

					mNativeDetector = new DetectionBasedTracker(
							mCascadeFile.getAbsolutePath(), 0);

					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				mOpenCvCameraView.setCameraIndex(1);
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public FdActivity() {
		mDetectorName = new String[2];
		mDetectorName[JAVA_DETECTOR] = "Java";
		mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.eyes_detect_surface_view);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();

		if (mAbsoluteEyesSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeEyesSize) > 0) {
				mAbsoluteEyesSize = Math.round(height * mRelativeEyesSize);
			}
			mNativeDetector.setMinEyesSize(mAbsoluteEyesSize);
		}

		MatOfRect eyes = new MatOfRect();

		if (mDetectorType == JAVA_DETECTOR) {
			if (mJavaDetector != null)
				mJavaDetector.detectMultiScale(mGray, eyes, 1.1, 2,
						2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
						new Size(mAbsoluteEyesSize, mAbsoluteEyesSize),
						new Size());
		} else if (mDetectorType == NATIVE_DETECTOR) {
			if (mNativeDetector != null)
				mNativeDetector.detect(mGray, eyes);
		} else {
			Log.e(TAG, "Detection method is not selected!");
		}

		Mat mROI = mGray;

		Rect[] eyesArray = eyes.toArray();

		Rect minRe = new Rect();

		for (int i = 0; i < eyesArray.length; i++) {

//			if (learn_frames < 10) {
				Core.rectangle(mRgba, eyesArray[i].tl(), eyesArray[i].br(),
						EYES_RECT_COLOR, 3);
				Rect e = eyesArray[i];

				// We compute the diagonal`s length of the rectangle containing
				// eye
				double dist = java.lang.Math.sqrt(e.width * e.width + e.height
						* e.height);

				// Convert to binary image by thresholding it.
				mROI = mGray.submat(e).clone();

				Core.MinMaxLocResult mmEye = Core.minMaxLoc(mROI);

				Imgproc.threshold(mROI, mROI, mmEye.minVal + 30, 255,
						Imgproc.THRESH_BINARY);

				// Find all contours
				List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

				Mat hierarchy = new Mat();

				Imgproc.findContours(mROI.clone(), contours, hierarchy,
						Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

				// Find the center of eye's rectangle
				Point center = new Point();
				center.x = e.x + e.width / 2;
				center.y = e.y + e.height / 2;

				Point ir = new Point();

				int len = contours.size();

				// We will keep a score for each contour found (based on its
				// brightness and its distance from the center of the eye's
				// rectangle)
				double[] score = new double[len];
				double minimum = 30000;
				int thick = 0, poz = 0;

				Point brectCenter = new Point();

				for (int j = 0; j < contours.size(); j++) {

					// Get rectangle from contour
					Rect brect = Imgproc.boundingRect(contours.get(j)); // Bounding
																		// box

					// Compute real position in frame of the contour rectangle
					Point t = new Point();
					Point b = new Point();
					t.x = e.tl().x + brect.tl().x;
					t.y = e.tl().y + brect.tl().y;

					b.x = e.tl().x + brect.br().x;
					b.y = e.tl().y + brect.br().y;

					Rect realRect = new Rect((int) t.x, (int) t.y, (int) b.x
							- (int) t.x, (int) b.y - (int) t.y);

					mROI = mGray.submat(realRect);
					Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

					// Compute real position in frame of min found
					ir.x = mmG.minLoc.x + t.x;
					ir.y = mmG.minLoc.y + t.y;

					// Core.circle(mRgba,ir, 4, new Scalar(255, 0, 0, 0), 4);

					// Compute the score for each contour
					// We take only the rectangles that can represent the iris/pupil
					// (are not too big)
					if (brect.width < e.width / 2) {
						score[j] = mmG.minVal / 255;
						score[j] = score[j]
								+ calculateDistance(center.x, center.y, ir.x,
										ir.y) / (dist / 2);

						brectCenter.x = brect.x + brect.width / 2;
						brectCenter.y = brect.y + brect.height / 2;

						score[j] = score[j]
								+ calculateDistance(center.x, center.y,
										brectCenter.x, brectCenter.y)
								/ (dist / 2);
					} else
						//The rectangle is not what we look for, we set score very high
						score[j] = 999;

					// Find contour with minimum score
					if (score[j] < minimum) {
						minimum = score[j];
						minRe = realRect;

					}

				}

				// Draw rectangle with minimum score
				Core.rectangle(mRgba, minRe.tl(), minRe.br(), new Scalar(255,
						135, 13, 0.5), 2);

//			} else {
//
//				match_eye(eyesArray[i], mGray.submat(minRe).clone(),
//						TM_SQDIFF_NORMED);
//				learn_frames++;
//			}

		}

		return mRgba;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemEyes50 = menu.add("Eyes size 50%");
		mItemEyes40 = menu.add("Eyes size 40%");
		mItemEyes30 = menu.add("Eyes size 30%");
		mItemEyes20 = menu.add("Eyes size 20%");
		mItemType = menu.add(mDetectorName[mDetectorType]);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == mItemEyes50)
			setMinEyesSize(0.5f);
		else if (item == mItemEyes40)
			setMinEyesSize(0.4f);
		else if (item == mItemEyes30)
			setMinEyesSize(0.3f);
		else if (item == mItemEyes20)
			setMinEyesSize(0.2f);
		else if (item == mItemType) {
			int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
			item.setTitle(mDetectorName[tmpDetectorType]);
			setDetectorType(tmpDetectorType);
		}
		return true;
	}

	private void setMinEyesSize(float eyeSize) {
		mRelativeEyesSize = eyeSize;
		mAbsoluteEyesSize = 0;
	}

	private void setDetectorType(int type) {
		if (mDetectorType != type) {
			mDetectorType = type;

			if (type == NATIVE_DETECTOR) {
				Log.i(TAG, "Detection Based Tracker enabled");
				mNativeDetector.start();
			} else {
				Log.i(TAG, "Cascade detector enabled");
				mNativeDetector.stop();
			}
		}
	}

	public static double calculateDistance(double x1, double y1, double x2,
			double y2) {
		double Sum = 0.0;
		Sum = Sum + Math.pow((x1 - x2), 2.0) + Math.pow((y1 - y2), 2.0);
		return Math.sqrt(Sum);
	}

	private void match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			return;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		type = TM_SQDIFF_NORMED;

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			matchLoc = mmres.minLoc;
		} else {
			matchLoc = mmres.maxLoc;
		}

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
				matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
				255));
		Rect rec = new Rect(matchLoc_tx, matchLoc_ty);

	}

}
