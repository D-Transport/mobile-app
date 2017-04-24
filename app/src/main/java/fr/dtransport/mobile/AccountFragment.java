package fr.dtransport.mobile;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.abi.datatypes.Type;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Main fragment
 *
 * @author Mathieu Porcel & Victor Le
 */
public class AccountFragment extends Fragment {

    public AccountFragment() {
        // Required empty public constructor
    }

    public void updateView(View view){
        // Get data
        String address = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_eth_addr", "");
        long balance = 0;
        try {
            balance = SmartContract.s.getBalance(address);
        } catch (Exception e) {}

        // Update view
        ((TextView) view.findViewById(R.id.txt_addr)).setText("Address: " + address);
        ((TextView) view.findViewById(R.id.txt_balance)).setText("Credit: " + balance);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_account, container, false);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        updateView(view);

        // Scan button
        final Button button = (Button) view.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String address = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_eth_addr", "");
                String terminalUrl = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_terminal_url", "");
                String terminalPort = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_terminal_port", "");

                try {
                    // TCP connection
                    Socket socket = new Socket(terminalUrl, Integer.parseInt(terminalPort));

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    // Send address
                    out.println(address);
                    out.flush();

                    // Read terminal index
                    long terminalIndex = Long.parseLong(in.readLine());
                    if (terminalIndex != -1) {
                        long price = Long.parseLong(in.readLine());
                        @SuppressWarnings("rawtypes")
                        List<Type> terminal = SmartContract.s.getTerminal(terminalIndex);

                        final String terminalAddr = SmartContract.s.toAddress(terminal.get(0));
                        String companyAddress = SmartContract.s.toAddress(terminal.get(3));

                        // Find company name
                        String company = "";
                        for (long i = 0; i < SmartContract.s.getCompanyCount(); i++) {
                            if (SmartContract.s.toAddress(SmartContract.s.getCompany(i).get(0)).equals(companyAddress)) {
                                company = SmartContract.s.getCompany(i).get(2).getValue().toString();
                                break;
                            }
                        }

                        // Check if terminal is ok
                        if (SmartContract.s.getAuthorizationDate(address, terminalAddr) != 0) {
                            // Confirmation dialog
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Confirmation")
                                    .setMessage(price + " credits\nCompany: " + company)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Validation
                                            try {
                                                SmartContract.s.validate(terminalAddr, 0);
                                            } catch (Exception e) {
                                                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                e.printStackTrace();
                                            }
                                            updateView(view);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Cancel
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Terminal error", Toast.LENGTH_LONG).show();
                    }

                    in.close();
                    out.close();
                    socket.close();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        return view;
    }
}
