/**
 * Copyright 2016 YouVersion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nuclei.ui;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import io.nuclei.BuildConfig;
import nuclei.task.ContextHandle;
import nuclei.task.http.Http;
import nuclei.logs.Logs;
import nuclei.task.TaskJobService;
import nuclei.task.TaskPool;
import nuclei.task.Tasks;

/**
 * Base Application which preconfigures things like Http Pools and Task Pools and Context Handles
 */
public class NucleiApplication extends Application {

    protected void initializeHttp() {
        try {
            Http.initialize(this);
        } catch (Throwable err) {
            Logs.newLog(NucleiApplication.class).w("Error initializing HTTP: " + err.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ContextHandle.initialize(this);
        Tasks.initialize(this);
        initializeHttp();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            onInitializeL();

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    @TargetApi(21)
    void onInitializeL() {
        TaskJobService.initialize(Tasks.get(TaskPool.DEFAULT_POOL));
    }

}
