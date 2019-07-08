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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.View;

import nuclei.persistence.PersistenceList;
import nuclei.persistence.PersistenceObserver;
import nuclei.persistence.Query;
import nuclei.persistence.QueryArgs;
import nuclei.persistence.QueryManager;
import nuclei.persistence.SupportPersistenceLoaderImpl;
import nuclei.persistence.adapter.PersistenceAdapterListener;
import nuclei.persistence.adapter.PersistenceListAdapter;
import nuclei.task.ContextHandle;

/**
 * Base Fragment with easy hooks for managing PersistenceLists and ContextHandles
 */
public abstract class NucleiSupportFragment extends Fragment implements NucleiContext, QueryManager {

    private ContextHandle mHandle;
    private ContextHandle mViewHandle;
    private SupportPersistenceLoaderImpl mLoader;
    private LifecycleManager mLifecycleManager;
    private Handler mHandler;

    @LifecycleManager.ManagedLifecycle
    private int mLifecycleStage;

    public void registerObserver(Uri uri, PersistenceObserver observer) {
        if (mHandler == null)
            mHandler = new Handler(Looper.getMainLooper());
        registerObserver(uri, mHandler, observer);
    }

    public void registerObserver(Uri uri, Handler handler, PersistenceObserver observer) {
        ContentObserverImpl contentObserver = new ContentObserverImpl(handler, getActivity(), observer);
        getActivity().getContentResolver().registerContentObserver(uri, true, contentObserver);
        manage(contentObserver);
    }

    protected <T extends Destroyable> T manage(T destroyable) {
        if (mLifecycleManager == null)
            mLifecycleManager = new LifecycleManager(LifecycleManager.FRAGMENT);
        mLifecycleManager.manage(mLifecycleStage, destroyable);
        return destroyable;
    }

    protected void destroy(Destroyable destroyable) {
        if (mLifecycleManager != null)
            mLifecycleManager.destroy(destroyable);
        else
            destroyable.onDestroy();
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, QueryArgs args) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, listener).execute(args);
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, listener).execute();
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener, QueryArgs args) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, new PersistenceAdapterListener<T>(listener)).execute(args);
    }

    @Override
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener) {
        if (mLoader == null)
            mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
        return mLoader.newLoaderBuilder(query, new PersistenceAdapterListener<T>(listener)).execute();
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceList.Listener<T> listener, String orderBy, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
            return mLoader.executeWithOrder(query, listener, orderBy, selectionArgs);
        } catch (IllegalStateException err) {
            Log.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQueryWithOrder(Query<T> query, PersistenceListAdapter<T> adapter, String orderBy, String...selectionArgs) {
        return executeQueryWithOrder(query, new PersistenceAdapterListener<T>(adapter), orderBy, selectionArgs);
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, String...selectionArgs) {
        try {
            if (mLoader == null)
                mLoader = SupportPersistenceLoaderImpl.newLoaderManager(getLoaderManager());
            return mLoader.execute(query, listener, selectionArgs);
        } catch (IllegalStateException err) {
            Log.wtf("Error executing query", err);
            return -1;
        }
    }

    @Deprecated
    public <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> adapter, String...selectionArgs) {
        return executeQuery(query, new PersistenceAdapterListener<T>(adapter), selectionArgs);
    }

    @Deprecated
    public void reexecuteQuery(int id, String...selectionArgs) {
        if (mLoader != null)
            mLoader.reexecute(id, selectionArgs);
    }

    @Deprecated
    public <T> void reexecuteQueryByName(int id, Query<T> query, String...selectionArgs) {
        if (mLoader != null) {
            mLoader.reexecute(id, query, selectionArgs);
        }
    }

    public void destroyQuery(int id) {
        if (mLoader != null)
            mLoader.destroyQuery(id);
    }

    protected int getLifecycleStage() {
        return mLifecycleStage;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mLifecycleStage = LifecycleManager.FRAGMENT;
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mLifecycleStage = LifecycleManager.VIEW;
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Get a managed context handle.
     *
     * When the context is destroyed, the handle will be released.
     *
     * @return The Context Handle
     */
    @Override
    public ContextHandle getContextHandle() {
        if (mHandle == null)
            mHandle = ContextHandle.obtain(getActivity());
        return mHandle;
    }

    @Override
    public ContextHandle getViewContextHandle() {
        if (getView() == null)
            return null;
        if (mViewHandle == null)
            mViewHandle = ContextHandle.obtain(getContext());
        return mViewHandle;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mLifecycleManager != null)
            mLifecycleManager.onDestroy(mLifecycleStage);
        mLifecycleStage = LifecycleManager.FRAGMENT;
        if (mViewHandle != null)
            mViewHandle.release();
        mViewHandle = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mHandle != null)
            mHandle.attach(context);
        if (mViewHandle != null)
            mViewHandle.attach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mHandle != null)
            mHandle.release();
        if (mViewHandle != null)
            mViewHandle.release();
    }

    @Override
    public void onDestroy() {
        if (mLifecycleManager != null)
            mLifecycleManager.onDestroy(mLifecycleStage);
        mLifecycleManager = null;
        if (mHandle != null)
            mHandle.release();
        mHandle = null;
        if (mViewHandle != null)
            mViewHandle.release();
        mViewHandle = null;
        super.onDestroy();
        if (mLoader != null)
            mLoader.onDestroy();
        mLoader = null;
        mHandler = null;
    }

}
