package set.sneek.snap;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import set.sneek.snap.R;
import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class Snap extends AppCompatActivity {
	SurfacePreviewDummy surfacePreviewDummy;
	Button snap;
	Camera camera;
	boolean autoMode = false;

	@SuppressLint("HandlerLeak")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		surfacePreviewDummy = new SurfacePreviewDummy(this, (SurfaceView) findViewById(R.id.surfaceView));
		surfacePreviewDummy.setLayoutParams(new LayoutParams(0, 0));
		((RelativeLayout) findViewById(R.id.layout)).addView(surfacePreviewDummy);
		surfacePreviewDummy.setVisibility(View.INVISIBLE);

		snap = (Button) findViewById(R.id.btnCapture);

		snap.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				camera.takePicture(null, null, jpegCallback);
			}
		});

		snap.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				camera.autoFocus(null);
				Toast.makeText(getApplicationContext(), "Camera Focused", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		SwitchCompat mode = (SwitchCompat) findViewById(R.id.switch1);
		mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
					autoMode = true;
					snap.setEnabled(false);
				} else {
					autoMode = false;
					snap.setEnabled(true);
				}
					
			}
		});
		
		/* Here is the code for periodic snapping */
		
		final Handler uiCallback = new Handler () {
		    public void handleMessage (Message msg) {
		        // do stuff with UI
		    	try{
		    		camera.autoFocus(null);
		    		camera.takePicture(null, null, jpegCallback);
		    	} catch (RuntimeException e){
		    		//Camera not initialised yet
		    	}
		    }
		};
		
		Thread timer = new Thread() {
		    public void run () {
		        for (;;) {
		            // do stuff in a separate thread
		        	if(autoMode)
		        		uiCallback.sendEmptyMessage(0);
		            try {
						Thread.sleep(3000);                        // <-- 3 second delay while snapping pics
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}    // sleep for 3 seconds
		        }
		    }
		};
		timer.start();
		
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		int numCams = Camera.getNumberOfCameras();
		if (numCams > 0) {
			try {
				camera = Camera.open(0);
				camera.startPreview();
				surfacePreviewDummy.setCamera(camera);
			} catch (RuntimeException ex) {
				Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onPause() {
		if (camera != null) {
			camera.stopPreview();
			surfacePreviewDummy.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		camera.startPreview();
		surfacePreviewDummy.setCamera(camera);
	}

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new Save().execute(data);
			resetCam();
		}
	};

	private class Save extends AsyncTask<byte[], Void, String> {

		@Override
		protected String doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(sdCard.getAbsolutePath() + File.separator + ".snap");
				dir.mkdirs();

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hhmmss");
			    String date = dateFormat.format(new Date());
			    String photoFile =  date + ".jpg";
				File outFile = new File(dir, photoFile);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();
				return  photoFile + " saved!";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return "Error";
		}
		
		@Override
		protected void onPostExecute(String result){
			super.onPostExecute(result);
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
		}

	}
}
