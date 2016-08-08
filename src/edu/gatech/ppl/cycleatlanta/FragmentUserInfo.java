package edu.gatech.ppl.cycleatlanta;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.gatech.ppl.cycleatlanta.region.ObaRegionsLoader;
import edu.gatech.ppl.cycleatlanta.region.elements.ObaRegion;
import edu.gatech.ppl.cycleatlanta.region.utils.PreferenceUtils;

public class FragmentUserInfo extends Fragment implements
        LoaderManager.LoaderCallbacks<ArrayList<ObaRegion>> {

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

    private static final String RELOAD = ".reload";

    private Spinner regionSpinner;

    private List<ObaRegion> mObaRegions;

    private boolean mLoaderCheck = false;

    public FragmentUserInfo() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.activity_user_info,
                container, false);
        final Button getStarted = (Button) rootView
                .findViewById(R.id.buttonGetStarted);
        getStarted.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ObaRegion currentRegion = Application.get().getCurrentRegion();

                if (currentRegion != null) {
                    String tutorialUrl = currentRegion.getTutorialUrl();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                            .parse(tutorialUrl));
                    startActivity(browserIntent);
                }
            }
        });

        if (Application.get().getCurrentRegion() == null) {
            getStarted.setVisibility(View.GONE);
        }

        SharedPreferences settings = getActivity().getSharedPreferences(
                "PREFS", 0);
        Map<String, ?> prefs = settings.getAll();
        for (Entry<String, ?> p : prefs.entrySet()) {
            int key = Integer.parseInt(p.getKey());

            switch (key) {
                case PREF_AGE:
                    ((Spinner) rootView.findViewById(R.id.ageSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_ETHNICITY:
                    ((Spinner) rootView.findViewById(R.id.ethnicitySpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_INCOME:
                    ((Spinner) rootView.findViewById(R.id.incomeSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_RIDERTYPE:
                    ((Spinner) rootView.findViewById(R.id.ridertypeSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_RIDERHISTORY:
                    ((Spinner) rootView.findViewById(R.id.riderhistorySpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_ZIPHOME:
                    ((EditText) rootView.findViewById(R.id.TextZipHome))
                            .setText((CharSequence) p.getValue());
                    break;
                case PREF_ZIPWORK:
                    ((EditText) rootView.findViewById(R.id.TextZipWork))
                            .setText((CharSequence) p.getValue());
                    break;
                case PREF_ZIPSCHOOL:
                    ((EditText) rootView.findViewById(R.id.TextZipSchool))
                            .setText((CharSequence) p.getValue());
                    break;
                case PREF_EMAIL:
                    ((EditText) rootView.findViewById(R.id.TextEmail))
                            .setText((CharSequence) p.getValue());
                    break;
                case PREF_CYCLEFREQ:
                    ((Spinner) rootView.findViewById(R.id.cyclefreqSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
                case PREF_GENDER:
                    ((Spinner) rootView.findViewById(R.id.genderSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
                    break;
            }
        }

        final EditText edittextEmail = (EditText) rootView
                .findViewById(R.id.TextEmail);

        edittextEmail.setImeOptions(EditorInfo.IME_ACTION_DONE);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initRegions();
    }

    private void initRegions() {
        regionSpinner = (Spinner) getActivity().findViewById(R.id.regionsSpinner);

        Bundle args = new Bundle();
        args.putBoolean(RELOAD, false);
        getLoaderManager().initLoader(0, args, this);

        regionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mLoaderCheck) {
                    mLoaderCheck = false;
                    return;
                }
                if (mObaRegions != null && position < mObaRegions.size()) {
                    ObaRegion selectedRegion = mObaRegions.get(position);
                    Application.get().setCurrentRegion(selectedRegion);
                    Application.get().setCustomApiUrl(null);
                    PreferenceUtils
                            .saveBoolean(getString(R.string.preference_key_auto_select_region), false);
                } else if (mObaRegions != null && mObaRegions.size() == position) {
                    showCustomApiDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void showCustomApiDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext = new EditText(getActivity());
        alert.setTitle(getActivity().getString(R.string.custom_api_server_title));

        alert.setView(edittext);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = edittext.getText().toString();
                String validValue = validateUrl(value);
                if (validValue != null) {
                    setCustomApiUrl(validValue);
                } else {
                    resetSelection();
                    Toast.makeText(getActivity(), getString(R.string.custom_api_url_error),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                resetSelection();
            }
        });

        alert.show();
    }

    private void resetSelection() {
        ArrayList<String> arraySpinner = new ArrayList<String>();
        ObaRegion currentRegion = Application.get().getCurrentRegion();
        int selection = 0;
        int i = 0;

        for (ObaRegion r : mObaRegions) {
            arraySpinner.add(r.getName());
            if (currentRegion != null && r.getId() == currentRegion.getId()) {
                selection = i;
            }
            i++;
        }

        arraySpinner.add(getActivity().getString(R.string.custom_api_server));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, arraySpinner);
        regionSpinner.setAdapter(adapter);
        regionSpinner.setSelection(selection);
    }

    private void setCustomApiUrl(String value) {
        Application.get().setCurrentRegion(null);
        Application.get().setCustomApiUrl(value);

        ArrayList<String> arraySpinner = new ArrayList<String>();

        for (ObaRegion r : mObaRegions) {
            arraySpinner.add(r.getName());
        }

        arraySpinner.add(getActivity().getString(R.string.custom_api_server));

        arraySpinner.add(value);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, arraySpinner);
        regionSpinner.setAdapter(adapter);
        regionSpinner.setSelection(arraySpinner.size() - 1);
    }

    /**
     * Returns true if the provided apiUrl could be a valid URL, false if it could not
     *
     * @param apiUrl the URL to validate
     * @return true if the provided apiUrl could be a valid URL, false if it could not
     */
    private String validateUrl(String apiUrl) {
        if (apiUrl == null) return null;

        try {
            // URI.parse() doesn't tell us if the scheme is missing, so use URL() instead (#126)
            URL url = new URL(apiUrl);
        } catch (MalformedURLException e) {
            // Assume HTTP scheme if none is provided
            apiUrl = getString(R.string.http_prefix) + apiUrl;
            return apiUrl;
        }
        if (Patterns.WEB_URL.matcher(apiUrl).matches())
            return apiUrl;
        else
            return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void savePreferences() {
        SharedPreferences settings = getActivity().getSharedPreferences(
                "PREFS", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("" + PREF_AGE,
                ((Spinner) getActivity().findViewById(R.id.ageSpinner))
                        .getSelectedItemPosition());
        editor.putInt("" + PREF_ETHNICITY, ((Spinner) getActivity()
                .findViewById(R.id.ethnicitySpinner)).getSelectedItemPosition());
        editor.putInt("" + PREF_INCOME,
                ((Spinner) getActivity().findViewById(R.id.incomeSpinner))
                        .getSelectedItemPosition());
        editor.putInt("" + PREF_RIDERTYPE, ((Spinner) getActivity()
                .findViewById(R.id.ridertypeSpinner)).getSelectedItemPosition());
        editor.putInt("" + PREF_RIDERHISTORY, ((Spinner) getActivity()
                .findViewById(R.id.riderhistorySpinner))
                .getSelectedItemPosition());

        editor.putString("" + PREF_ZIPHOME, ((EditText) getActivity()
                .findViewById(R.id.TextZipHome)).getText().toString());
        editor.putString("" + PREF_ZIPWORK, ((EditText) getActivity()
                .findViewById(R.id.TextZipWork)).getText().toString());
        editor.putString("" + PREF_ZIPSCHOOL, ((EditText) getActivity()
                .findViewById(R.id.TextZipSchool)).getText().toString());
        editor.putString("" + PREF_EMAIL, ((EditText) getActivity()
                .findViewById(R.id.TextEmail)).getText().toString());

        editor.putInt("" + PREF_CYCLEFREQ, ((Spinner) getActivity()
                .findViewById(R.id.cyclefreqSpinner)).getSelectedItemPosition());

        editor.putInt("" + PREF_GENDER,
                ((Spinner) getActivity().findViewById(R.id.genderSpinner))
                        .getSelectedItemPosition());

        // Don't forget to commit your edits!!!
        editor.commit();
        Toast.makeText(getActivity(), "User information saved.",
                Toast.LENGTH_SHORT).show();
    }

    /* Creates the menu items */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu items for use in the action bar
        inflater.inflate(R.menu.user_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save_user_info:
                savePreferences();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<ArrayList<ObaRegion>> onCreateLoader(int id, Bundle args) {
        boolean refresh = args.getBoolean(RELOAD);
        return new ObaRegionsLoader(getActivity(), refresh);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<ObaRegion>> loader, ArrayList<ObaRegion> data) {
        mObaRegions = data;
        mLoaderCheck = true;

        ArrayList<String> arraySpinner = new ArrayList<String>();
        ObaRegion currentRegion = Application.get().getCurrentRegion();
        String customApiUrl = Application.get().getCustomApiUrl();
        int selection = 0;
        int i = 0;

        for (ObaRegion r : data) {
            arraySpinner.add(r.getName());
            if (currentRegion != null && r.getId() == currentRegion.getId()) {
                selection = i;
            }
            i++;
        }

        arraySpinner.add(getActivity().getString(R.string.custom_api_server));

        if (currentRegion == null && customApiUrl != null) {
            // Add the custom api to beginning of the list
            arraySpinner.add(customApiUrl);
            selection = arraySpinner.size() - 1;
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, arraySpinner);
        regionSpinner.setAdapter(adapter);
        regionSpinner.setSelection(selection);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<ObaRegion>> loader) {

    }
}