package com.shopify.rnandroidauto;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactInstanceManagerBuilder;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.modules.appregistry.AppRegistry;
import com.facebook.react.modules.core.TimingModule;

import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleObserver;


public final class CarService extends CarAppService {
    private ReactInstanceManager mReactInstanceManager;
    private CarScreen screen;

    public CarService() {
        // Exported services must have an empty public constructor.
    }


    @Override
    public void onCreate() {
        mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
    }

    private ReactInstanceManager makeInstance() {
        ReactInstanceManagerBuilder builder =
                ReactInstanceManager.builder()
                        .setApplication(getApplication())
                        .setJSMainModulePath("android_auto")
                        .setUseDeveloperSupport(true)
                        .setJSIModulesPackage(null)
                        .setInitialLifecycleState(LifecycleState.BEFORE_CREATE);

        builder.addPackage(new AndroidAutoPackage());

        ReactInstanceManager reactInstanceManager = builder.build();

        return reactInstanceManager;
    }

    @Override
    @NonNull
    public Session onCreateSession() {
        return new Session() {
            @Override
            @NonNull
            public Screen onCreateScreen(@Nullable Intent intent) {
                screen = new CarScreen(getCarContext(), mReactInstanceManager.getCurrentReactContext());
                screen.setMarker("root");
//                Lifecycle lifecycle = getLifecycle();
//                lifecycle.addObserver(new LifecycleObserver() {
//                    public void onDestroy(@NonNull LifecycleOwner owner) {
//                    new LifecycleObserver() {
//                        public void onDestroy(@NonNull LifecycleOwner owner) {
//                            Log.d("ReactAUTO", "ondestroy");
//                            CarContext context = getCarContext();
//                            context.stopService(new Intent(context, CarService.class));
//                        }
//                    };
//                        mReactInstanceManager.destroy();
//                    }
//                });
                runJsApplication();
                return screen;
            }


            private void runJsApplication() {
                ReactContext reactContext = mReactInstanceManager.getCurrentReactContext();

                if (reactContext == null) {
                    mReactInstanceManager.addReactInstanceEventListener(
                            new ReactInstanceManager.ReactInstanceEventListener() {
                                @Override
                                public void onReactContextInitialized(ReactContext reactContext) {
                                    invokeStartTask(reactContext);
                                    mReactInstanceManager.removeReactInstanceEventListener(this);
                                }
                            });
                    mReactInstanceManager.createReactContextInBackground();
                } else {
                    invokeStartTask(reactContext);
                }
            }

            @Override
            public void onNewIntent(@NonNull Intent intent) {
                super.onNewIntent(intent);
            }

            private void invokeStartTask(ReactContext reactContext) {
                try {
                    if (mReactInstanceManager == null) {
                        return;
                    }

                    if (reactContext == null) {
                        return;
                    }

                    CatalystInstance catalystInstance = reactContext.getCatalystInstance();
                    String jsAppModuleName = "androidAuto";

                    WritableNativeMap appParams = new WritableNativeMap();
                    appParams.putDouble("rootTag", 1.0);
                    @Nullable Bundle appProperties = Bundle.EMPTY;
                    if (appProperties != null) {
                        appParams.putMap("initialProps", Arguments.fromBundle(appProperties));
                    }

                    catalystInstance.getJSModule(AppRegistry.class).runApplication(jsAppModuleName, appParams);
                    TimingModule timingModule = reactContext.getNativeModule(TimingModule.class);

                    AndroidAutoModule carModule = mReactInstanceManager
                            .getCurrentReactContext()
                            .getNativeModule(AndroidAutoModule.class);
                    carModule.setCarContext(getCarContext(), screen);

                    timingModule.onHostResume();
                } finally {
                }
            }
        };
    }

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        } else {
            return new HostValidator.Builder(getApplicationContext())
                    .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                    .build();
        }
    }
}
