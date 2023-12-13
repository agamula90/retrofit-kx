package io.github.retrofitx.android.inject

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dagger.Module
import io.github.retrofitx.android.BuildConfig
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.retrofitx.android.dto.DefaultError
import io.github.retrofitx.android.dto.IdError
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                interceptor = HttpLoggingInterceptor().apply {
                    level = when {
                        BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
                        else -> HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build();
    }

    @Singleton
    @Provides
    fun provideRetrofitFactory(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): RetrofitFactory {
        return RetrofitFactory {
            Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(it)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }
    }

    @Singleton
    @Provides
    fun provideDefaultErrorAdapter(moshi: Moshi): JsonAdapter<DefaultError> {
        return moshi.adapter(DefaultError::class.java)
    }

    @Singleton
    @Provides
    fun provideIdErrorAdapter(moshi: Moshi): JsonAdapter<IdError> {
        return moshi.adapter(IdError::class.java)
    }
}