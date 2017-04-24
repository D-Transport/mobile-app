package fr.dtransport.mobile;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * History fragment
 *
 * @author Mathieu Porcel & Victor Le
 */
public class HistoryFragment extends Fragment {

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_history, container, false);

        List<String> validations = new ArrayList<>();
        String address = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_eth_addr", "");
        try {
            // Find user info
            long nbValidation = 0;
            for (long i = 0; i < SmartContract.s.getUsersCount(); i++) {
                List<Type> user = SmartContract.s.getUser(i);
                if (user != null && SmartContract.s.toAddress(user.get(0)).equals(address)) {
                    nbValidation = ((BigInteger) user.get(2).getValue()).longValue();
                    break;
                }
            }

            // Get validations list
            for (long i = 0; i < nbValidation; i++) {
                validations.add(SmartContract.s.toAddress(SmartContract.s.getValidation(address, i).get(1)));
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1, validations);

        ((ListView) view.findViewById(R.id.list_view)).setAdapter(adapter);

        return view;
    }
}
