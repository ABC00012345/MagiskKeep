package com.lukas.magiskkeep

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AppSetup.newInstance] factory method to
 * create an instance of this fragment.
 */
class AppSetup : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_setup, container, false)

        val textView = view.findViewById<TextView>(R.id.textView2)
        textView.text = HtmlCompat.fromHtml(getString(R.string.app_created_by_abc00012345), HtmlCompat.FROM_HTML_MODE_LEGACY)
        textView.movementMethod = LinkMovementMethod.getInstance()

        // Find the button and set click listener
        val continueBtn = view.findViewById<ImageButton>(R.id.continueBtn)
        continueBtn.setOnClickListener {

            if(RootManager.execCmd("echo 'test'").contains("test")) {

                parentFragmentManager.setFragmentResult(
                    "setup_complete",
                    Bundle().apply { putBoolean("success", true) }
                )

                // Close the fragment
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context,
                    getString(R.string.please_make_sure_to_have_root_installed_and_allow_root_access), Toast.LENGTH_LONG).show()
            }
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AppSetup.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AppSetup().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}