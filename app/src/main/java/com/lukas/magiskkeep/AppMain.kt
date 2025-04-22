package com.lukas.magiskkeep

import android.os.Bundle
import android.provider.DocumentsContract.Root
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AppMain.newInstance] factory method to
 * create an instance of this fragment.
 */
class AppMain : Fragment() {
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

        val rootView = inflater.inflate(R.layout.fragment_app_main, container, false)

        val slot = RootManager.execCmd("getprop ro.boot.slot_suffix").trim()

        // try to get the boot partition path
        val possiblePaths = listOf(
            "/dev/block/by-name/boot$slot",
            "/dev/block/bootdevice/by-name/boot$slot",
            "/dev/block/platform/bootdevice/by-name/boot$slot"
        )
        var bootBlock = possiblePaths.firstOrNull { path ->
            RootManager.execCmd("[ -e $path ] && echo exists").contains("exists")
        }
        if (!bootBlock.isNullOrEmpty()) {
            bootBlock = bootBlock.substring(0,bootBlock.length-2)
        } else {
            Toast.makeText(context,"Error getting boot partition path!!! Unrecoverable error. Cannot continue", Toast.LENGTH_LONG).show() // todo: add to translation editor
            return rootView
        }

        if(slot.contains("_a") || slot.contains("_b")) {
            rootView?.findViewById<TextView>(R.id.curSlot)?.setText("Current slot: $slot")
            val targetSlot = if (slot.contains("_a")) "_b" else "_a"
            rootView?.findViewById<TextView>(R.id.targetSlot)?.setText("Target Slot: $targetSlot")
            val output = RootManager.execCmd("ls /dev/block/by-name/boot$slot")

            if (output.contains(bootBlock+slot)) {
                rootView?.findViewById<TextView>(R.id.bootImgPath)?.text = "Boot Partition path: $bootBlock$slot"
                rootView?.findViewById<TextView>(R.id.installBtn)?.isEnabled = true

                val logView = rootView.findViewById<TextView>(R.id.logview)
                // conformation dialog if install btn pressed
                rootView?.findViewById<TextView>(R.id.installBtn)?.setOnClickListener {
                    context?.let { it1 ->
                        MaterialAlertDialogBuilder(it1)
                            .setTitle("Confirmation")
                            .setMessage(
                                getString(
                                    R.string.are_you_sure_you_want_to_install,
                                    targetSlot
                                ))
                            // if user pressed yes, install
                            .setPositiveButton("Yes") { dialog, which ->

                                // install magisk
                                Utils.startMagiskInstallProcess(logView, bootBlock+targetSlot,
                                    requireContext()
                                ) { success ->

                                    if(success) {
                                        /// display another dialog inside here:
                                        context?.let { it1 ->
                                            MaterialAlertDialogBuilder(it1)
                                                .setTitle("Done")
                                                .setMessage(
                                                    getString(
                                                        R.string.magisk_installed_successfully,
                                                        targetSlot
                                                    )
                                                )
                                                .setPositiveButton("Ok") { dialog, which ->
                                                    // do nothing
                                                }
                                                .show()
                                        }
                                    } else {
                                        /// display another dialog inside here:
                                        context?.let { it1 ->
                                            MaterialAlertDialogBuilder(it1)
                                                .setTitle("Done")
                                                .setMessage(
                                                    getString(
                                                        R.string.magisk_installation_failed,
                                                        targetSlot
                                                    )
                                                )
                                                .setPositiveButton("Ok") { dialog, which ->
                                                    // do nothing
                                                }
                                                .show()
                                        }
                                    }

                                }
                            }
                            .setNegativeButton("No") { dialog, which ->
                                Toast.makeText(context,"Magisk wasn't installed!", Toast.LENGTH_LONG).show() // todo: add to translation editor
                            }
                            .show()
                    }
                }
            } else {

                Toast.makeText(
                    context,
                    "Error getting current boot partition path",
                    Toast.LENGTH_LONG
                ).show()
            }

        } else {
            Toast.makeText(
                context,
                "Query for current slot failed !!!",
                Toast.LENGTH_LONG
            ).show()
        }

        // Inflate the layout for this fragment
        return rootView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AppMain.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AppMain().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}