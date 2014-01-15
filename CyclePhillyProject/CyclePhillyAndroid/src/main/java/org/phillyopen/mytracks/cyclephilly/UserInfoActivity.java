/**	 Cycle Altanta, Copyright 2012 Georgia Institute of Technology
 *                                    Atlanta, GA. USA
 *
 *   @author Christopher Le Dantec <ledantec@gatech.edu>
 *   @author Anhong Guo <guoanhong15@gmail.com>
 *
 *   Updated/Modified for Atlanta's app deployment. Based on the
 *   CycleTracks codebase for SFCTA.
 *
 *   CycleTracks, (c) 2009 San Francisco County Transportation Authority
 * 					  San Francisco, CA, USA
 *
 *   Licensed under the GNU GPL version 3.0.
 *   See http://www.gnu.org/licenses/gpl-3.0.txt for a copy of GPL version 3.0.
 *
 * 	 @author Billy Charlton <billy.charlton@sfcta.org>
 *
 */
package org.phillyopen.mytracks.cyclephilly;

import java.util.Map;
import java.util.Map.Entry;

import org.phillyopen.mytracks.cyclephilly.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class UserInfoActivity extends Activity {
	public final static int PREF_AGE = 1;
	public final static int PREF_ZIPHOME = 2;
	public final static int PREF_ZIPWORK = 3;
	public final static int PREF_ZIPSCHOOL = 4;
	public final static int PREF_EMAIL = 5;
	public final static int PREF_GENDER = 6;
	public final static int PREF_CYCLEFREQ = 7;
	public final static int PREF_ETHNICITY = 8;
	public final static int PREF_INCOME = 9;
	public final static int PREF_RIDERTYPE = 10;
	public final static int PREF_RIDERHISTORY = 11;

	private static final String TAG = "UserPrefActivity";

	private final static int MENU_SAVE = 0;

	final String[] freqDesc = { "Less than once a month",
			"Several times a month", "Several times per week", "Daily" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.userprefs);

		// Don't pop up the soft keyboard until user clicks!
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		SeekBar sb = (SeekBar) findViewById(R.id.SeekCycleFreq);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				TextView tv = (TextView) findViewById(R.id.TextFreq);
				tv.setText(freqDesc[arg1 / 100]);
			}
		});

		Button btn = (Button) findViewById(R.id.saveButton);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(UserInfoActivity.this,
						MainInput.class);
				startActivity(intent);
				finish();
			}

		});

		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		Map<String, ?> prefs = settings.getAll();
		for (Entry<String, ?> p : prefs.entrySet()) {
			int key = Integer.parseInt(p.getKey());
			// CharSequence value = (CharSequence) p.getValue();

			switch (key) {
			case PREF_AGE:
				((Spinner) findViewById(R.id.ageSpinner))
						.setSelection(((Integer) p.getValue()).intValue());
				break;
			case PREF_ETHNICITY:
				((Spinner) findViewById(R.id.ethnicitySpinner))
						.setSelection(((Integer) p.getValue()).intValue());
				break;
			case PREF_INCOME:
				((Spinner) findViewById(R.id.incomeSpinner))
						.setSelection(((Integer) p.getValue()).intValue());
				break;
			case PREF_RIDERTYPE:
				((Spinner) findViewById(R.id.ridertypeSpinner))
						.setSelection(((Integer) p.getValue()).intValue());
				break;
			case PREF_RIDERHISTORY:
				((Spinner) findViewById(R.id.riderhistorySpinner))
						.setSelection(((Integer) p.getValue()).intValue());
				break;
			case PREF_ZIPHOME:
				((EditText) findViewById(R.id.TextZipHome)).setText((CharSequence) p.getValue());
				break;
			case PREF_ZIPWORK:
				((EditText) findViewById(R.id.TextZipWork)).setText((CharSequence) p.getValue());
				break;
			case PREF_ZIPSCHOOL:
				((EditText) findViewById(R.id.TextZipSchool)).setText((CharSequence) p.getValue());
				break;
			case PREF_EMAIL:
				((EditText) findViewById(R.id.TextEmail)).setText((CharSequence) p.getValue());
				break;
			case PREF_CYCLEFREQ:
				((SeekBar) findViewById(R.id.SeekCycleFreq)).setProgress(((Integer) p.getValue()).intValue());
				break;
			case PREF_GENDER:
				int x = ((Integer) p.getValue()).intValue();
				if (x == 2) {
					((RadioButton) findViewById(R.id.ButtonMale)).setChecked(true);
				} else if (x == 1) {
					((RadioButton) findViewById(R.id.ButtonFemale)).setChecked(true);
				}
				break;
			}
		}
	}

	@Override
	public void onDestroy() {
		savePreferences();
		super.onDestroy();
	}

	private void savePreferences() {
		// Save user preferences. We need an Editor object to
		// make changes. All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("" + PREF_AGE, ((Spinner) findViewById(R.id.ageSpinner))
				.getSelectedItemPosition());
		editor.putInt("" + PREF_ETHNICITY,
				((Spinner) findViewById(R.id.ethnicitySpinner))
						.getSelectedItemPosition());
		editor.putInt("" + PREF_INCOME,
				((Spinner) findViewById(R.id.incomeSpinner))
						.getSelectedItemPosition());
		editor.putInt("" + PREF_RIDERTYPE,
				((Spinner) findViewById(R.id.ridertypeSpinner))
						.getSelectedItemPosition());
		editor.putInt("" + PREF_RIDERHISTORY,
				((Spinner) findViewById(R.id.riderhistorySpinner))
						.getSelectedItemPosition());

		editor.putString("" + PREF_ZIPHOME,
				((EditText) findViewById(R.id.TextZipHome)).getText()
						.toString());
		editor.putString("" + PREF_ZIPWORK,
				((EditText) findViewById(R.id.TextZipWork)).getText()
						.toString());
		editor.putString("" + PREF_ZIPSCHOOL,
				((EditText) findViewById(R.id.TextZipSchool)).getText()
						.toString());
		editor.putString("" + PREF_EMAIL,
				((EditText) findViewById(R.id.TextEmail)).getText().toString());
		editor.putInt("" + PREF_CYCLEFREQ, ((SeekBar) findViewById(R.id.SeekCycleFreq)).getProgress());

		RadioGroup rbg = (RadioGroup) findViewById(R.id.RadioGroup01);
		if (rbg.getCheckedRadioButtonId() == R.id.ButtonMale) {
			editor.putInt("" + PREF_GENDER, 2);
			//Log.v(TAG, "gender=" + 2);
		}
		if (rbg.getCheckedRadioButtonId() == R.id.ButtonFemale) {
			editor.putInt("" + PREF_GENDER, 1);
			//Log.v(TAG, "gender=" + 1);
		}

//		Log.v(TAG,
//				"ageIndex="
//						+ ((Spinner) findViewById(R.id.ageSpinner))
//								.getSelectedItemPosition());
//		Log.v(TAG,
//				"ethnicityIndex="
//						+ ((Spinner) findViewById(R.id.ethnicitySpinner))
//								.getSelectedItemPosition());
//		Log.v(TAG,
//				"incomeIndex="
//						+ ((Spinner) findViewById(R.id.incomeSpinner))
//								.getSelectedItemPosition());
//		Log.v(TAG,
//				"ridertypeIndex="
//						+ ((Spinner) findViewById(R.id.ridertypeSpinner))
//								.getSelectedItemPosition());
//		Log.v(TAG,
//				"riderhistoryIndex="
//						+ ((Spinner) findViewById(R.id.riderhistorySpinner))
//								.getSelectedItemPosition());
//		Log.v(TAG, "ziphome="
//				+ ((EditText) findViewById(R.id.TextZipHome)).getText()
//						.toString());
//		Log.v(TAG, "zipwork="
//				+ ((EditText) findViewById(R.id.TextZipWork)).getText()
//						.toString());
//		Log.v(TAG, "zipschool="
//				+ ((EditText) findViewById(R.id.TextZipSchool)).getText()
//						.toString());
//		Log.v(TAG, "email="
//				+ ((EditText) findViewById(R.id.TextEmail)).getText()
//						.toString());
//		Log.v(TAG,
//				"frequency="
//						+ ((SeekBar) findViewById(R.id.SeekCycleFreq))
//								.getProgress() / 100);

		// Don't forget to commit your edits!!!
		editor.commit();
		Toast.makeText(getBaseContext(), "User preferences saved.",
				Toast.LENGTH_SHORT).show();
	}

	/* Creates the menu items */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_SAVE, 0, "Save").setIcon(
				android.R.drawable.ic_menu_save);
		return true;
	}

	/* Handles item selections */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SAVE:
			savePreferences();
			this.finish();
			return true;
		}
		return false;
	}
}
