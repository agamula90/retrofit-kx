## Retrofit-kx

Wrapper around [Retrofit](https://github.com/square/retrofit) library, that provides kotlin friendly api.  
[Retrofit](https://github.com/square/retrofit) is really powerful and nice library from square to handle http communication with usage of annotations.  
Though its usages by kotlin users can be improved by:
- using [sealed classes](#retrofit-kx-sealed-classes) to handle success / exceptional flows
- fixing [runtime usage](#runtime-setup-and-usage-pattern) pattern
- reconfiguring [base urls](#retrofit-kx-base-urls)

### Setup

Retrofit-kx is based on ksp, so it should be applied first:
- in top level build.gradle `id ("com.google.devtools.ksp") version "1.7.20-1.0.7" apply false`
- in project build.gradle - `id ("com.google.devtools.ksp")` in plugins section
- find root kotlin directory in build/generated subdirectories - `kotlinPath` (examples can be found in `retrofit-kx-android-sample` / `retrofit-kx-kotlin-sample`)
- add kotlin directory to sources:

```
sourceSets.getByName("main") {
    kotlin.srcDir("$kotlinPath")
}
```

Retrofit-kx need to know about servicesPackage and default error:
- servicesPackage - package with retrofit services declaration ([Nested packages](#features-development) not supported yet)

```
ksp {
    arg("servicesPackage", "retrofitx.myservices")
}
```

- default error - one and only one type should be annotated with `RetrofitError`, it'll be used as type of error parameter of `DataResponse` / `UnitResponse`, but can be overridden with `Remote` annotation

```
@JsonClass(generateAdapter = true)
@RetrofitError
class DefaultError(val message: String)
```
- dependencies:

```
implementation ("io.github.agamula90:retrofit-kx:0.0.1")
ksp ("io.github.agamula90:retrofit-kx-ksp:0.0.1")
```

Retrofit-kx expose original types if users still want to use them instead of retrofit-kx extensions.

### Retrofit-kx sealed classes

[Retrofit](https://github.com/square/retrofit) is written with java, and therefore 
it's up to its users to handle connection issues with `try-catch` block, that can be forgotten.  
To handle exceptional flow retrofit users need to parse `retrofit2.Response`, that you can get as service function result 
(if user wrapped it with `retrofit2.Response`) or you can get from wrapped `HttpException` 
(if user not wrapped function result).

To promote complete processing of exceptional and success flows retrofit-kx introduce 2 sealed classes: `DataResponse` and `UnitResponse` 
for responses with body and without respectively. You can check some examples of their use in [usages](#usages) section.

### Runtime setup and usage pattern

To safely use Retrofit next steps required: 
- use `Retrofit.Builder` to get `Retrofit` instance 
- use generic `Retrofit.create` function to get retrofit service
- wrap all service function invocations with `try-catch` block to not crash in runtime in case of connection issues
- parse exceptions from response
- handle parsed responses for success / exceptional flows

Retrofit-kx fixes next issues with Retrofit usages:
- no need of builder pattern as it's kotlin, base url in `RetrofitX` is required explicitly unlike in `Retrofit.Builder` it required implicitly, so you'll get crash with `Retrofit.Builder` if base url not specified 
- all services are preconfigured and can be retrieved from `RetrofitX` properties (so you won't get crash in runtime if passing class instead of interface, etc.)
- no need of `try-catch` wrappers, all RetrofitX service functions are safe to use
- no exceptions parsing from user
- promote complete processing of success / exceptional flows by using sealed classes (IDE highlights incomplete processing)

### Retrofit-kx base urls

Retrofit doesn't provide ability to change base url per service, and they have no plans to support it in future - [base url override](https://github.com/square/retrofit/pull/3793#issuecomment-1369862341),
even though this feature was requested multiple times - [retrofit 3 discussion](https://github.com/square/retrofit/issues/2180#issuecomment-327745074).  
So RetrofitX provides this feature with ability to override not only base url, but also default error (see [usages](#usages) section)

Also Retrofit [doesn't provide](https://github.com/square/retrofit/issues/2161#issuecomment-274204152) ability to use dynamic base url, 
so RetrofitX generates constructor - with `baseUrl: Flow<String>` + `coroutineScope: CoroutineScope` instead of `baseUrl: String`

### Usages

1. [Setup](#setup) retrofit-kx
2. Add one or more services to services package

Service is interface where each suspend function has retrofit http annotation. 
If some suspend function of interface does not have http annotation, then 
this interface considered as invalid, and infrastructure for it won't be generated.

User can override remote url per service, or remote + error per service: 

``` 
@Remote(url = "https://google.com/", error = GoogleError::class)
interface ProductService {
 
}
```

User can override boxing per service (see `Boxed` documentation for details)  

3. Create RetrofitX instance (with `baseUrl: String` for static base url or `baseUrl: Flow<String>` + `coroutineScope: CoroutineScope` for dynamic base url)
4. Get configured service instance
5. Use functions from generated service to get responses, encapsulated into sealed classes:

``` 
when(val response = retrofitX.productService.getProducts()) {
     is DataResponse.ConnectionError -> {
         response.cause.printStackTrace()
     }
     is DataResponse.ApiError -> {
         response.cause
     }
     is DataResponse.Success -> {
         response.data
     }
}
```

6. For unit functions you can use regular function:

``` 
when(val response = retrofitX.productService.signOut()) {
     is UnitResponse.ConnectionError -> {
         response.cause.printStackTrace()
     }
     is UnitResponse.ApiError -> {
         response.cause
     }
     is UnitResponse.Success -> {
         
     }
}
```

or safeFunction if you don't want to handle connection / api errors ` retrofitX.productService.signOutSafe()`

7. If RetrofitX instance or RetrofitX service is out of date, run ksp task to get updated api:

```
./gradlew :retrofit-kx-android-sample:kspDebugKotlin 
./gradlew :retrofit-kx-kotlin-sample:kspKotlin
```

### Features development

There are certain features / fixes, that are not supported yet, but are planned to:

- improve testing support
  - ksp task should be disabled per tests ( use `kspDebug` / `kspRelease` tasks instead of `ksp`) or [compile testing](https://github.com/tschuchortdev/kotlin-compile-testing) could be used
- add support for functions with `retrofit2.Response` wrappers
- check subpackages for `RetrofitX` services generation
- generate obfuscation rules for android users
- more dynamic generation pattern based on user changes (grade plugins, [file watcher](https://www.jetbrains.com/help/idea/using-file-watchers.html))
  
    This one :arrow_up: looks most interesting but it requires lot of investigation to be done  

License
--------

    Copyright 2023 Andrii Hamula

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
