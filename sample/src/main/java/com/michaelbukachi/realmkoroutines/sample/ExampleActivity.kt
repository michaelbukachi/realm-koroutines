package com.michaelbukachi.realmkoroutines.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.michaelbukachi.realmkoroutines.awaitFirst
import com.michaelbukachi.realmkoroutines.transactAwait
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*

class ExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launchWhenResumed {
            val realm = Realm.getDefaultInstance()
            realm.transactAwait {
                it.deleteAll()
                val testObject = TestObject(name = "Some Test")
                it.copyToRealm(testObject)
            }
            val result = realm.where<TestObject>().awaitFirst()
            greetings.text = result?.name ?: "Default"
        }
    }
}
