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
package nuclei.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;

import nuclei.logs.Log;
import nuclei.logs.Logs;
import nuclei.media.playback.PlaybackManager;
import nuclei.ui.Destroyable;

public class MediaInterface implements Destroyable {

    private static final Log LOG = Logs.newLog(MediaInterface.class);

    public static final String ACTION_CONNECTED = "nuclei.media.interface.CONNECTED";

    private static final AtomicLong SURFACE_ID = new AtomicLong(1);

    private Activity mLActivity;
    private FragmentActivity mFragmentActivity;
    MediaInterfaceCallback mCallbacks;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaControls;
    private MediaControllerCompat.Callback mMediaCallback;
    private MediaPlayerController mPlayerControls;
    private ProgressHandler mHandler = new ProgressHandler(this);
    private Surface mSurface;
    private long mSurfaceId;

    public MediaInterface(FragmentActivity activity, MediaId mediaId, MediaInterfaceCallback callback) {
        mFragmentActivity = activity;
        mCallbacks = callback;
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mMediaBrowser = new MediaBrowserCompat(activity.getApplicationContext(),
                new ComponentName(activity, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
        mSurfaceId = SURFACE_ID.incrementAndGet();
        if (SURFACE_ID.longValue() == Long.MAX_VALUE)
            SURFACE_ID.set(1);
    }

    @TargetApi(21)
    public MediaInterface(Activity activity, MediaId mediaId, MediaInterfaceCallback callback) {
        mLActivity = activity;
        mCallbacks = callback;
        mPlayerControls = new MediaPlayerController(mediaId);
        mPlayerControls.setMediaControls(mCallbacks, null);
        mMediaBrowser = new MediaBrowserCompat(activity.getApplicationContext(),
                new ComponentName(activity, MediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        MediaInterface.this.onConnected();
                    }
                }, null);
        mMediaBrowser.connect();
        mSurfaceId = SURFACE_ID.incrementAndGet();
        if (SURFACE_ID.longValue() == Long.MAX_VALUE)
            SURFACE_ID.set(1);
    }

    public MediaPlayerController getPlayerController() {
        return mPlayerControls;
    }

    public MediaControllerCompat getMediaController() {
        return mMediaControls;
    }

    public void autoHide() {
        if (mHandler != null) {
            mHandler.removeMessages(ProgressHandler.AUTO_HIDE);
            mHandler.sendEmptyMessageDelayed(ProgressHandler.AUTO_HIDE, 3000);
        }
    }

    public void cancelAutoHide() {
        if (mHandler != null)
            mHandler.removeMessages(ProgressHandler.AUTO_HIDE);
    }

    public void setSurface(Surface surface) {
        if (mMediaControls != null) {
            final Bundle args = new Bundle();
            args.putParcelable(MediaService.EXTRA_SURFACE, surface);
            args.putLong(MediaService.EXTRA_SURFACE_ID, mSurfaceId);
            mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_SURFACE, args);
            mSurface = null;
        } else {
            mSurface = surface;
        }
    }

    @Override
    public void onDestroy() {
        if (mCallbacks != null)
            mCallbacks.onDestroy(this);
        if (mPlayerControls != null) {
            mPlayerControls.setMediaControls(null, null);
        }
        if (mMediaControls != null && mMediaCallback != null)
            mMediaControls.unregisterCallback(mMediaCallback);
        if (mMediaBrowser != null)
            mMediaBrowser.disconnect();
        mMediaControls = null;
        mMediaCallback = null;
        mPlayerControls = null;
        mMediaBrowser = null;
        mCallbacks = null;
        mFragmentActivity = null;
        mLActivity = null;
        mHandler = null;
    }

    @TargetApi(21)
    private void setMediaControllerL() {
        if (mLActivity != null)
            mLActivity.setMediaController((MediaController) mMediaControls.getMediaController());
    }

    void onConnected() {
        try {
            Context context = mFragmentActivity == null ? mLActivity : mFragmentActivity;
            if (context == null)
                return;
            mMediaControls = new MediaControllerCompat(context, mMediaBrowser.getSessionToken());
            mMediaCallback = new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    MediaInterface.this.onPlaybackStateChanged(state);
                }

                @Override
                public void onSessionEvent(String event, Bundle extras) {
                    MediaInterface.this.onSessionEvent(event, extras);
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    MediaInterface.this.onMetadataChanged(metadata);
                }
            };
            mMediaControls.registerCallback(mMediaCallback);

            if (mFragmentActivity != null) {
                MediaControllerCompat.setMediaController(mFragmentActivity, mMediaControls);
                //mFragmentActivity.setSupportMediaController(mMediaControls);
            } else if (mLActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                setMediaControllerL();

            if (mPlayerControls != null)
                mPlayerControls.setMediaControls(mCallbacks, mMediaControls);

            if (mMediaControls.getPlaybackState() != null)
                onPlaybackStateChanged(mMediaControls.getPlaybackState());

            final Intent intent = mFragmentActivity == null ? mLActivity.getIntent() : mFragmentActivity.getIntent();
            if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
                final Bundle params = intent.getExtras();
                final String query = params.getString(SearchManager.QUERY);
                LOG.i("Starting from voice search query=" + query);
                mMediaControls.getTransportControls()
                        .playFromSearch(query, params);
            }
            if (mCallbacks != null) {
                mCallbacks.onConnected(this);
                MediaMetadataCompat metadataCompat = mMediaControls.getMetadata();
                if (metadataCompat != null) {
                    final long duration = metadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                    if (duration > 0)
                        mCallbacks.setTimeTotal(this, duration);
                }
            }

            Bundle extras = mMediaControls.getExtras();
            if (extras != null && extras.containsKey(MediaService.EXTRA_CONNECTED_CAST)) {
                mCallbacks.onCasting(this, extras.getString(MediaService.EXTRA_CONNECTED_CAST));
            }

            if (mPlayerControls != null && mPlayerControls.isPlaying())
                mHandler.start();

            if (mSurface != null)
                setSurface(mSurface);

            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_CONNECTED));
        } catch (RemoteException err) {
            LOG.e("Error in onConnected", err);
        }
    }

    void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (mCallbacks != null) {
            if (state.getState() != PlaybackStateCompat.STATE_BUFFERING) {
                mCallbacks.onLoaded(mPlayerControls);
            } else if (state.getState() == PlaybackStateCompat.STATE_BUFFERING) {
                mCallbacks.onLoading(mPlayerControls);
            }
            mCallbacks.onStateChanged(this, state);
        }
        if (mPlayerControls != null) {
            if (MediaPlayerController.isPlaying(mMediaControls, state, mPlayerControls.getMediaId())) {
                if (mCallbacks != null)
                    mCallbacks.onPlaying(mPlayerControls);
                if (mHandler != null)
                    mHandler.start();
            } else {
                if (mCallbacks != null) {
                    if (state.getState() == PlaybackStateCompat.STATE_STOPPED)
                        mCallbacks.onStopped(mPlayerControls);
                    else
                        mCallbacks.onPaused(mPlayerControls);
                }
                if (mHandler != null)
                    mHandler.stop();
            }
        } else if (mHandler != null) {
            mHandler.stop();
        }
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING && mCallbacks != null) {
            if (mPlayerControls != null
                    && (!mPlayerControls.isMediaControlsSet() || !MediaPlayerController.isEquals(mMediaControls, mPlayerControls.getMediaId()))) {
                mPlayerControls.setMediaControls(mCallbacks, mMediaControls);
            }
        }
    }

    void onSessionEvent(String event, Bundle extras) {
        if (mCallbacks != null) {
            if (event.startsWith(MediaService.EVENT_TIMER)) {
                mCallbacks.onTimerChanged(this, MediaService.getTimerFromEvent(event));
            } else if (event.startsWith(MediaService.EVENT_SPEED)) {
                float speed = MediaService.getSpeedFromEvent(event);
                mCallbacks.onSpeedChanged(this, speed);
            } else if (event.startsWith(MediaService.EVENT_CAST)) {
                mCallbacks.onCasting(this, MediaService.getCastFromEvent(event));
            }
        }
    }

    void onMetadataChanged(@Nullable MediaMetadataCompat metadata) {
        if (mCallbacks != null) {
            if (mPlayerControls != null) {
                final String mediaId = MediaPlayerController.getMediaId(metadata);
                if (mediaId != null && (mPlayerControls.mMediaId == null || !mediaId.equals(mPlayerControls.mMediaId.toString()))) {
                    LOG.v("Media ID Changed");
                    mPlayerControls.mMediaId = MediaProvider.getInstance().getMediaId(mediaId);
                    onPlaybackStateChanged(mMediaControls.getPlaybackState());
                }
            }
            mCallbacks.onMetadataChanged(this, metadata);
            final long duration = metadata == null ? 0 : metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            if (duration > 0)
                mCallbacks.setTimeTotal(this, duration);
        }
    }

    public void setSpeed(float speed) {
        Bundle args = new Bundle();
        args.putFloat(MediaService.EXTRA_SPEED, speed);
        mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_SPEED, args);
    }

    public void setTimer(long timer) {
        Bundle args = new Bundle();
        args.putLong(MediaService.EXTRA_TIMER, timer);
        mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_TIMER, args);
    }

    public void setAutoContinue(boolean autoContinue) {
        Bundle args = new Bundle();
        args.putBoolean(MediaService.EXTRA_AUTO_CONTINUE, autoContinue);
        mMediaControls.getTransportControls().sendCustomAction(MediaService.ACTION_SET_AUTO_CONTINUE, args);
    }

    public interface MediaInterfaceCallback {

        void onConnected(MediaInterface mediaInterface);

        boolean onPlay(String currentMediaId, String id, MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onPause(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onSkipNext(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onSkipPrevious(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onFastForward(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);
        boolean onRewind(MediaPlayerController controller, MediaControllerCompat.TransportControls controls);

        long getNextPosition(long nextPosition);

        void onLoading(MediaPlayerController controller);

        void onLoaded(MediaPlayerController controller);

        void onPlaying(MediaPlayerController controller);

        void onPaused(MediaPlayerController controller);

        void onStopped(MediaPlayerController controller);

        void onTimerChanged(MediaInterface mediaInterface, long timer);

        void onSpeedChanged(MediaInterface mediaInterface, float speed);

        void onStateChanged(MediaInterface mediaInterface, PlaybackStateCompat state);

        void onCasting(MediaInterface mediaInterface, String deviceName);

        void setTimePlayed(MediaInterface mediaInterface, long played);

        void setTimeTotal(MediaInterface mediaInterface, long total);

        void setVisible(MediaInterface mediaInterface, boolean visible);

        boolean isPositionChanging(MediaInterface mediaInterface);

        void setPosition(MediaInterface mediaInterface, long max, long position, long secondaryPosition);

        void onMetadataChanged(MediaInterface mediaInterface, MediaMetadataCompat mediaMetadataCompat);

        void onDestroy(MediaInterface mediaInterface);

    }

    public static class ProgressHandler extends Handler {

        private static final int SHOW_PROGRESS = 1;
        private static final int AUTO_HIDE = 2;
        public static final int MAX_PROGRESS = 1000;

        private final WeakReference<MediaInterface> mMediaInterface;

        public ProgressHandler(MediaInterface mediaInterface) {
            mMediaInterface = new WeakReference<>(mediaInterface);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_PROGRESS: {
                    final MediaInterface mediaInterface = mMediaInterface.get();
                    if (mediaInterface != null
                            && mediaInterface.mCallbacks != null
                            && !mediaInterface.mCallbacks.isPositionChanging(mediaInterface)) {
                        final MediaPlayerController controller = mediaInterface.getPlayerController();
                        final long position = controller.getCurrentPosition();
                        final long duration = controller.getDuration();
                        long currentPos = 0;
                        if (duration > 0) {
                            currentPos = PlaybackManager.ONE_SECOND * position / duration;
                        }
                        final int percent = mediaInterface.getPlayerController().getBufferPercentage();
                        mediaInterface.mCallbacks.setPosition(mediaInterface, MAX_PROGRESS, currentPos, percent * 10);
                        mediaInterface.mCallbacks.setTimePlayed(mediaInterface, position);
                        sendEmptyMessageDelayed(SHOW_PROGRESS, PlaybackManager.ONE_SECOND - (position % PlaybackManager.ONE_SECOND));
                        //sendEmptyMessageDelayed(SHOW_PROGRESS, PlaybackManager.ONE_SECOND);
                    }
                    break;
                }
                case AUTO_HIDE: {
                    final MediaInterface mediaInterface = mMediaInterface.get();
                    if (mediaInterface != null && mediaInterface.mCallbacks != null) {
                        if (mediaInterface.mCallbacks.isPositionChanging(mediaInterface))
                            sendEmptyMessageDelayed(AUTO_HIDE, 3000);
                        else
                            mediaInterface.mCallbacks.setVisible(mediaInterface, false);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        public void start() {
            if (!hasMessages(SHOW_PROGRESS))
                sendEmptyMessage(SHOW_PROGRESS);
        }

        public void stop() {
            removeMessages(SHOW_PROGRESS);
        }
    }

}
