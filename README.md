[ ![Download](https://api.bintray.com/packages/michaelbukachi/realm/realm-koroutines/images/download.svg?version=1.0.1) ](https://bintray.com/michaelbukachi/realm/realm-koroutines/1.0.1/link)

Realm Coroutines
----------------

A collection of convenience extension functions for realm database.


Usage
-----
```kotlin
launch {
    // main thread
    val realm = Realm.getDefaultInstance()
    realm.transactAwait {
        // Different thread
        val testObject = TestObject(name = "Some Test")
        it.copyToRealm(testObject)
    }
    
    val result = realm.where<TestObject>().awaitFirst()
    ...
}

```

Offline Objects Support
---------------
Realm objects queried from the db cannot be used in different
threads without explicitly disconnecting the objects as follows:
```kotlin
val realm = Realm.getDefaultInstance()
val result = realm.where<TestObject>().findFirst()
val offlineResult = realm.copyFromRealm(result)
launch(Dispatchers.IO) {
    // Do something with offlineResult
}
```
Now with the added offline functions, it can be done as follows:
```kotlin
val realm = Realm.getDefaultInstance()
val result = realm.where<TestObject>().firstOffline()
launch(Dispatchers.IO) {
    // Do something with result
}
```
With flows:
```kotlin
 launch(Dispatchers.IO) {
    val realm = Realm.getDefaultInstance()
    val resultsFlow = realm.where<TestObject>().flowAll()
    val testObjects = resultsFlow.take(1).first()
 }
 
```

Note
-----
Don't use frozen objects if you are planning to use this library. You
can't attach listeners to frozen objects meaning you can't convert the
callbacks to suspended functions/flows
As of version `1.0.1`, attempting to use a frozen object with this library
will raise a `FrozenException`


Download
--------

```groovy
implementation 'com.michaelbukachi:realmkoroutines:1.0.1'
```

License
-------

    Copyright (C) 2019 Michael Bukachi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
