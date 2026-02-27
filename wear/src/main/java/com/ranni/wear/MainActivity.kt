package com.ranni.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ranni.wear.ui.WearViewModel
import com.ranni.wear.ui.theme.RanniWearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RanniWearTheme {
                // WearViewModel is an AndroidViewModel — viewModel() handles instantiation
                val vm: WearViewModel = viewModel()
                WearApp(viewModel = vm)
            }
        }
    }
}
