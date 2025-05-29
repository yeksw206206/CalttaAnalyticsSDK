package com.sensorsdata.sdk.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment

//	"$screen_name":"com.sensorsdata.sdk.demo.FragmentTestActivity|com.sensorsdata.sdk.demo.TestFragment",
class TestFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view  = inflater.inflate(R.layout.fragment_click, container, false)
        initViews(view)
        return view;
    }

    private fun initViews(view : View) {
        val button: Button = view.findViewById(R.id.button)
        val textView: TextView = view.findViewById(R.id.textView)
        val editText: EditText = view.findViewById<EditText>(R.id.editText)
        val checkBox: CheckBox = view.findViewById<CheckBox>(R.id.checkBox)
        val radioButton: RadioButton = view.findViewById<RadioButton>(R.id.radioButton)
        val switchButton: Switch = view.findViewById<Switch>(R.id.switchButton)
        val imageView: ImageView = view.findViewById<ImageView>(R.id.imageView)
        val progressBar: ProgressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        button.setOnClickListener {
            Log.d("ClickActivity", "sensor click")

        }
        checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            textView.text = "CheckBox: $isChecked"
        }
        radioButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            textView.text = "RadioButton: $isChecked"
        }
        switchButton.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            textView.text = "Switch: $isChecked"
        }
        imageView.setOnClickListener { v: View? ->
            progressBar.visibility = View.VISIBLE
        }
    }
}