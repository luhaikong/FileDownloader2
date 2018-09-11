package com.ynyx.filedownloader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ynyx.filedownloader.util.ApkUtil;
import com.ynyx.filedownloader.util.TimeUtil;

import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.base.Status;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.OnRetryableFileDownloadStatusListener;
import org.wlf.filedownloader.util.FileUtil;
import org.wlf.filedownloader.util.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/9/11.
 */

public class DownloadFileListAdapter3 extends RecyclerView.Adapter<DownloadFileListAdapter3.FileViewHolder>
        implements OnRetryableFileDownloadStatusListener {

    private static final String TAG = DownloadFileListAdapter3.class.getSimpleName();
    // all download infos
    private List<DownloadFileInfo> mDownloadFileInfos = Collections.synchronizedList(new ArrayList<DownloadFileInfo>());
    // select download file infos
    private List<DownloadFileInfo> mSelectedDownloadFileInfos = new ArrayList<DownloadFileInfo>();

    private Context context;
    private Toast mToast;

    public DownloadFileListAdapter3(Context context) {
        this.context = context;
        initDownloadFileInfos();
    }

    // init DownloadFileInfos
    private void initDownloadFileInfos() {
        this.mDownloadFileInfos = FileDownloader.getDownloadFiles();
        mSelectedDownloadFileInfos.clear();
        if (mOnItemSelectListener != null) {
            mOnItemSelectListener.onNoneSelect();
        }
    }

    /**
     * update show
     */
    public void updateShow() {
        initDownloadFileInfos();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position;// make viewType == position
    }

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent == null) {
            return null;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.main__item_download,parent,false);
        FileViewHolder holder = new FileViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()){
            onBindViewHolder(holder, position);
        } else {
            Payload payload = null;
            for (int i = payloads.size() - 1; i >= 0; i--) {
                try {
                    payload = (Payload) payloads.get(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (payload != null) {
                    break;
                }
            }
            DownloadFileInfo downloadFileInfo = mDownloadFileInfos.get(position);
            if (downloadFileInfo != null) {
                final String url = downloadFileInfo.getUrl();

                if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                    holder.ivIcon.setImageResource(R.mipmap.main__ic_apk);
                } else {
                    holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
                }

                // file name
                holder.tvFileName.setText(downloadFileInfo.getFileName());

                // download progress
                int totalSize = (int) downloadFileInfo.getFileSizeLong();
                int downloaded = (int) downloadFileInfo.getDownloadedSizeLong();
                double rate = (double) totalSize / Integer.MAX_VALUE;
                if (rate > 1.0) {
                    totalSize = Integer.MAX_VALUE;
                    downloaded = (int) (downloaded / rate);
                }

                holder.pbProgress.setMax(totalSize);
                holder.pbProgress.setProgress(downloaded);

                // file size
                double downloadSize = downloadFileInfo.getDownloadedSizeLong() / 1024f / 1024;
                double fileSize = downloadFileInfo.getFileSizeLong() / 1024f / 1024;

                holder.tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
                holder.tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

                // downloaded percent
                double percent = downloadSize / fileSize * 100;
                holder.tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

                switch (downloadFileInfo.getStatus()) {
                    // download file status:unknown
                    case Status.DOWNLOAD_STATUS_UNKNOWN:
                        holder.tvText.setText(context.getString(R.string.main__can_not_download));
                        break;
                    // download file status:retrying
                    case Status.DOWNLOAD_STATUS_RETRYING:
                        String retryTimesStr = "";
                        if (payload != null) {
                            retryTimesStr = "(" + payload.mRetryTimes + ")";
                        }
                        holder.tvText.setText(context.getString(R.string.main__retrying_connect_resource) + retryTimesStr);
                        break;
                    // download file status:preparing
                    case Status.DOWNLOAD_STATUS_PREPARING:
                        holder.tvText.setText(context.getString(R.string.main__getting_resource));
                        break;
                    // download file status:prepared
                    case Status.DOWNLOAD_STATUS_PREPARED:
                        holder.tvText.setText(context.getString(R.string.main__connected_resource));
                        break;
                    // download file status:paused
                    case Status.DOWNLOAD_STATUS_PAUSED:
                        holder.tvText.setText(context.getString(R.string.main__paused));
                        break;
                    // download file status:downloading
                    case Status.DOWNLOAD_STATUS_DOWNLOADING:
                        if (holder.tvText.getTag() != null) {
                            holder.tvText.setText((String) holder.tvText.getTag());
                        } else {
                            if (payload != null && payload.mDownloadSpeed > 0 && payload.mRemainingTime > 0) {
                                holder.tvText.setText(MathUtil.formatNumber(payload.mDownloadSpeed) + "KB/s   " + TimeUtil
                                        .seconds2HH_mm_ss(payload.mRemainingTime));
                            } else {
                                holder.tvText.setText(context.getString(R.string.main__downloading));
                            }
                        }
                        break;
                    // download file status:error
                    case Status.DOWNLOAD_STATUS_ERROR:
                        String msg = context.getString(R.string.main__download_error);
                        if (payload != null && payload.mFailReason != null) {
                            FileDownloadStatusFailReason failReason = payload.mFailReason;
                            if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                                msg += context.getString(R.string.main__check_network);
                            } else if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                                msg += context.getString(R.string.main__url_illegal);
                            } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failReason.getType())) {
                                msg += context.getString(R.string.main__network_timeout);
                            }
                        }
                        holder.tvText.setText(msg);
                        break;
                    // download file status:waiting
                    case Status.DOWNLOAD_STATUS_WAITING:
                        holder.tvText.setText(context.getString(R.string.main__waiting));
                        break;
                    // download file status:completed
                    case Status.DOWNLOAD_STATUS_COMPLETED:
                        holder.tvDownloadSize.setText("");
                        if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                            String packageName = ApkUtil.getUnInstallApkPackageName(context, downloadFileInfo.getFilePath());
                            boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);
                            if (isInstall) {
                                holder.tvText.setText(context.getString(R.string.main__open));
                            } else {
                                holder.tvText.setText(context.getString(R.string.main__not_install));
                            }
                        } else {
                            holder.tvText.setText(context.getString(R.string.main__download_completed));
                        }
                        break;
                    // download file status:file not exist
                    case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                        holder.tvDownloadSize.setText("");
                        holder.tvText.setText(context.getString(R.string.main__file_not_exist));
                        break;
                }

                holder.cbSelect.setChecked(false);
                for (DownloadFileInfo info:mSelectedDownloadFileInfos){
                    if (info.getUrl().equals(downloadFileInfo.getUrl())){
                        holder.cbSelect.setChecked(true);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        DownloadFileInfo downloadFileInfo = mDownloadFileInfos.get(position);
        if (downloadFileInfo != null) {
            final String url = downloadFileInfo.getUrl();

            if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                holder.ivIcon.setImageResource(R.mipmap.main__ic_apk);
            } else {
                holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
            }

            // file name
            holder.tvFileName.setText(downloadFileInfo.getFileName());

            // download progress
            int totalSize = (int) downloadFileInfo.getFileSizeLong();
            int downloaded = (int) downloadFileInfo.getDownloadedSizeLong();
            double rate = (double) totalSize / Integer.MAX_VALUE;
            if (rate > 1.0) {
                totalSize = Integer.MAX_VALUE;
                downloaded = (int) (downloaded / rate);
            }

            holder.pbProgress.setMax(totalSize);
            holder.pbProgress.setProgress(downloaded);

            // file size
            double downloadSize = downloadFileInfo.getDownloadedSizeLong() / 1024f / 1024;
            double fileSize = downloadFileInfo.getFileSizeLong() / 1024f / 1024;

            holder.tvDownloadSize.setText(((float) (Math.round(downloadSize * 100)) / 100) + "M/");
            holder.tvTotalSize.setText(((float) (Math.round(fileSize * 100)) / 100) + "M");

            // downloaded percent
            double percent = downloadSize / fileSize * 100;
            holder.tvPercent.setText(((float) (Math.round(percent * 100)) / 100) + "%");

            switch (downloadFileInfo.getStatus()) {
                // download file status:unknown
                case Status.DOWNLOAD_STATUS_UNKNOWN:
                    holder.tvText.setText(context.getString(R.string.main__can_not_download));
                    break;
                // download file status:retrying
                case Status.DOWNLOAD_STATUS_RETRYING:
                    holder.tvText.setText(context.getString(R.string.main__retrying_connect_resource));
                    break;
                // download file status:preparing
                case Status.DOWNLOAD_STATUS_PREPARING:
                    holder.tvText.setText(context.getString(R.string.main__getting_resource));
                    break;
                // download file status:prepared
                case Status.DOWNLOAD_STATUS_PREPARED:
                    holder.tvText.setText(context.getString(R.string.main__connected_resource));
                    break;
                // download file status:paused
                case Status.DOWNLOAD_STATUS_PAUSED:
                    holder.tvText.setText(context.getString(R.string.main__paused));
                    break;
                // download file status:downloading
                case Status.DOWNLOAD_STATUS_DOWNLOADING:
                    if (holder.tvText.getTag() != null) {
                        holder.tvText.setText((String) holder.tvText.getTag());
                    } else {
                        holder.tvText.setText(context.getString(R.string.main__downloading));
                    }
                    break;
                // download file status:error
                case Status.DOWNLOAD_STATUS_ERROR:
                    holder.tvText.setText(context.getString(R.string.main__download_error));
                    break;
                // download file status:waiting
                case Status.DOWNLOAD_STATUS_WAITING:
                    holder.tvText.setText(context.getString(R.string.main__waiting));
                    break;
                // download file status:completed
                case Status.DOWNLOAD_STATUS_COMPLETED:
                    holder.tvDownloadSize.setText("");
                    if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(downloadFileInfo.getFileName()))) {// apk
                        String packageName = ApkUtil.getUnInstallApkPackageName(context, downloadFileInfo.getFilePath());
                        boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);
                        if (isInstall) {
                            holder.tvText.setText(context.getString(R.string.main__open));
                        } else {
                            holder.tvText.setText(context.getString(R.string.main__not_install));
                        }
                    } else {
                        holder.tvText.setText(context.getString(R.string.main__download_completed));
                    }
                    break;
                // download file status:file not exist
                case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                    holder.tvDownloadSize.setText("");
                    holder.tvText.setText(context.getString(R.string.main__file_not_exist));
                    break;
            }

            holder.cbSelect.setChecked(false);
            for (DownloadFileInfo info:mSelectedDownloadFileInfos){
                if (info.getUrl().equals(downloadFileInfo.getUrl())){
                    holder.cbSelect.setChecked(true);
                    break;
                }
            }

            holder.cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mSelectedDownloadFileInfos.add(FileDownloader.getDownloadFile(url));
                        if (mOnItemSelectListener != null) {
                            // select a download file
                            mOnItemSelectListener.onSelected(mSelectedDownloadFileInfos);
                        }
                    } else {
                        mSelectedDownloadFileInfos.remove(FileDownloader.getDownloadFile(url));
                        if (mSelectedDownloadFileInfos.isEmpty()) {
                            if (mOnItemSelectListener != null) {
                                // select none
                                mOnItemSelectListener.onNoneSelect();
                            }
                        } else {
                            if (mOnItemSelectListener != null) {
                                // select a download file
                                mOnItemSelectListener.onSelected(mSelectedDownloadFileInfos);
                            }
                        }
                    }
                }
            });

            // set convertView click
            setBackgroundOnClickListener(holder.lnlyDownloadItem, downloadFileInfo);
        }
    }

    @Override
    public int getItemCount() {
        return mDownloadFileInfos.size();
    }

    // show toast
    private void showToast(CharSequence text) {
        if (mToast == null) {
            mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        } else {
            mToast.cancel();
            mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }


    // set convertView click
    private void setBackgroundOnClickListener(final View lnlyDownloadItem, final DownloadFileInfo curDownloadFileInfo) {

        lnlyDownloadItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final Context context = v.getContext();
                if (curDownloadFileInfo != null) {
                    switch (curDownloadFileInfo.getStatus()) {
                        // download file status:unknown
                        case Status.DOWNLOAD_STATUS_UNKNOWN:
                            showToast(context.getString(R.string.main__can_not_download2) + curDownloadFileInfo
                                    .getFilePath() + context.getString(R.string.main__re_download));
                            break;
                        // download file status:error & paused
                        case Status.DOWNLOAD_STATUS_ERROR:
                        case Status.DOWNLOAD_STATUS_PAUSED:
                            FileDownloader.start(curDownloadFileInfo.getUrl());
                            showToast(context.getString(R.string.main__start_download) + curDownloadFileInfo
                                    .getFileName());
                            break;
                        // download file status:file not exist
                        case Status.DOWNLOAD_STATUS_FILE_NOT_EXIST:
                            // show dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(context.getString(R.string.main__whether_re_download)).setNegativeButton
                                    (context.getString(R.string.main__dialog_btn_cancel), null);
                            builder.setPositiveButton(context.getString(R.string.main__dialog_btn_confirm), new
                                    DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // re-download
                                            FileDownloader.reStart(curDownloadFileInfo.getUrl());
                                            showToast(context.getString(R.string.main__re_download2) + curDownloadFileInfo
                                                    .getFileName());
                                        }
                                    });
                            builder.show();
                            break;
                        // download file status:retrying & waiting & preparing & prepared & downloading
                        case Status.DOWNLOAD_STATUS_RETRYING:
                        case Status.DOWNLOAD_STATUS_WAITING:
                        case Status.DOWNLOAD_STATUS_PREPARING:
                        case Status.DOWNLOAD_STATUS_PREPARED:
                        case Status.DOWNLOAD_STATUS_DOWNLOADING:
                            // pause
                            FileDownloader.pause(curDownloadFileInfo.getUrl());

                            showToast(context.getString(R.string.main__paused_download) + curDownloadFileInfo
                                    .getFileName());

                            TextView tvText = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);
                            if (tvText != null) {
                                tvText.setText(context.getString(R.string.main__paused));
                            }
                            break;
                        // download file status:completed
                        case Status.DOWNLOAD_STATUS_COMPLETED:

                            TextView tvDownloadSize = (TextView) lnlyDownloadItem.findViewById(R.id.tvDownloadSize);
                            if (tvDownloadSize != null) {
                                tvDownloadSize.setText("");
                            }

                            final TextView tvText2 = (TextView) lnlyDownloadItem.findViewById(R.id.tvText);

                            if ("apk".equalsIgnoreCase(FileUtil.getFileSuffix(curDownloadFileInfo.getFileName())))
                            {// apk

                                final String packageName = ApkUtil.getUnInstallApkPackageName(context,
                                        curDownloadFileInfo.getFilePath());
                                boolean isInstall = ApkUtil.checkAppInstalled(context, packageName);

                                if (isInstall) {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__open));
                                        try {
                                            Intent intent2 = context.getPackageManager().getLaunchIntentForPackage
                                                    (packageName);
                                            context.startActivity(intent2);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // show install dialog
                                            ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                            showToast(context.getString(R.string.main__not_install_apk) +
                                                    curDownloadFileInfo.getFileName());
                                            tvText2.setText(context.getString(R.string.main__no_install));
                                        }
                                    }
                                } else {
                                    if (tvText2 != null) {
                                        tvText2.setText(context.getString(R.string.main__no_install));
                                    }
                                    ApkUtil.installApk(context, curDownloadFileInfo.getFilePath());
                                    showToast(context.getString(R.string.main__not_install_apk2) +
                                            curDownloadFileInfo.getFileName());
                                }
                            } else {
                                tvText2.setText(context.getString(R.string.main__download_completed));
                            }
                            break;
                    }
                }
            }
        });
    }

    /**
     * 等待下载（等待其它任务执行完成，或者FileDownloader在忙别的操作）
     * @param downloadFileInfo
     */
    @Override
    public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    /**
     * 正在重试下载（如果你配置了重试次数，当一旦下载失败时会尝试重试下载），retryTimes是当前第几次重试
     * @param downloadFileInfo
     * @param retryTimes
     */

    @Override
    public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1,
                    retryTimes, null));
        }
    }

    /**
     * 准备中（即，正在连接资源）
     * @param downloadFileInfo
     */
    @Override
    public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    /**
     * 已准备好（即，已经连接到了资源）
     * @param downloadFileInfo
     */
    @Override
    public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    /**
     * 正在下载，downloadSpeed为当前下载速度，单位KB/s，remainingTime为预估的剩余时间，单位秒
     * @param downloadFileInfo
     * @param downloadSpeed
     * @param remainingTime
     */
    @Override
    public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long
            remainingTime) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), downloadSpeed, remainingTime, -1, null));
        }
    }

    /**
     * 下载已被暂停
     * @param downloadFileInfo
     */
    @Override
    public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    /**
     * 下载完成（整个文件已经全部下载完成）
     * @param downloadFileInfo
     */
    @Override
    public void onFileDownloadStatusCompleted(DownloadFileInfo downloadFileInfo) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, null));
        }
    }

    /**
     * 下载失败了，详细查看失败原因failReason，有些失败原因你可能必须关心
     * @param url
     * @param downloadFileInfo
     * @param failReason
     */
    @Override
    public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo,
                                           FileDownloadStatusFailReason failReason) {
        int position = findPosition(downloadFileInfo);
        if (position >= 0 && position < getItemCount()) {
            notifyItemChanged(position, new Payload(downloadFileInfo.getStatus(), downloadFileInfo.getUrl(), -1, -1, -1, failReason));
        }
        if (context!=null){
            String msg = context.getString(R.string.main__download_error);
            if (failReason != null) {
                if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__check_network);
                } else if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__url_illegal);
                } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__network_timeout);
                } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__storage_space_is_full);
                } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_CAN_NOT_WRITE.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__storage_space_can_not_write);
                } else if (FileDownloadStatusFailReason.TYPE_FILE_NOT_DETECT.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__file_not_detect);
                } else if (FileDownloadStatusFailReason.TYPE_BAD_HTTP_RESPONSE_CODE.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__http_bad_response_code);
                } else if (FileDownloadStatusFailReason.TYPE_HTTP_FILE_NOT_EXIST.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__http_file_not_exist);
                } else if (FileDownloadStatusFailReason.TYPE_SAVE_FILE_NOT_EXIST.equals(failReason.getType())) {
                    msg += context.getString(R.string.main__save_file_not_exist);
                }
            }

            showToast(msg + "，url：" + url);
        }
    }


    private int findPosition(DownloadFileInfo downloadFileInfo) {
        if (downloadFileInfo == null) {
            return -1;
        }
        mDownloadFileInfos = FileDownloader.getDownloadFiles();
        for (int i = 0; i < mDownloadFileInfos.size(); i++) {
            DownloadFileInfo downloadFile = mDownloadFileInfos.get(i);
            if (downloadFile == null || TextUtils.isEmpty(downloadFile.getUrl())) {
                continue;
            }
            if (downloadFile.getUrl().equals(downloadFileInfo.getUrl())) {
                // find
                return i;
            }
        }
        return -1;
    }

    private static class Payload {

        private int mStatus = Status.DOWNLOAD_STATUS_UNKNOWN;

        private String mUrl;
        private float mDownloadSpeed;
        private long mRemainingTime;
        private int mRetryTimes;
        private FileDownloadStatusFailReason mFailReason;

        public Payload(int status, String url, float downloadSpeed, long remainingTime, int retryTimes, FileDownloadStatusFailReason failReason) {
            this.mStatus = status;
            this.mUrl = url;
            this.mDownloadSpeed = downloadSpeed;
            this.mRemainingTime = remainingTime;
            this.mRetryTimes = retryTimes;
            this.mFailReason = failReason;
        }

        @Override
        public String toString() {
            return "Payload{" +
                    "mStatus=" + mStatus +
                    ", mUrl='" + mUrl + '\'' +
                    ", mDownloadSpeed=" + mDownloadSpeed +
                    ", mRemainingTime=" + mRemainingTime +
                    ", mRetryTimes=" + mRetryTimes +
                    ", mFailReason=" + mFailReason +
                    '}';
        }
    }

    /**
     * OnItemSelectListener
     * 复选框操作接口回调
     */
    public interface OnItemSelectListener {

        void onSelected(List<DownloadFileInfo> selectDownloadFileInfos);

        void onNoneSelect();
    }

    private OnItemSelectListener mOnItemSelectListener;

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        this.mOnItemSelectListener = onItemSelectListener;
    }


    public class FileViewHolder extends RecyclerView.ViewHolder{

        LinearLayout lnlyDownloadItem;
        ImageView ivIcon;
        TextView tvFileName;
        ProgressBar pbProgress;
        TextView tvDownloadSize;
        TextView tvTotalSize;
        TextView tvPercent;
        TextView tvText;
        CheckBox cbSelect;

        public FileViewHolder(View itemView) {
            super(itemView);
            lnlyDownloadItem = (LinearLayout) itemView.findViewById(R.id.lnlyDownloadItem);
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
            tvFileName = (TextView) itemView.findViewById(R.id.tvFileName);
            pbProgress = (ProgressBar) itemView.findViewById(R.id.pbProgress);
            tvDownloadSize = (TextView) itemView.findViewById(R.id.tvDownloadSize);
            tvTotalSize = (TextView) itemView.findViewById(R.id.tvTotalSize);
            tvPercent = (TextView) itemView.findViewById(R.id.tvPercent);
            tvText = (TextView) itemView.findViewById(R.id.tvText);
            cbSelect = (CheckBox) itemView.findViewById(R.id.cbSelect);
        }
    }
}
