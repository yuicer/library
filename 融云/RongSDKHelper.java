package com.sencent.chat.business.rongcloud;
 
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.j256.ormlite.misc.TransactionManager;
import com.orhanobut.logger.Logger;
import com.sencent.chat.LoginCallBack;
import com.sencent.chat.WeIMClient;
import com.sencent.chat.business.IReceiveMessageInterceptor;
import com.sencent.chat.business.MessageBusiness;
import com.sencent.chat.business.ReceiveMessageInterceptor;
import com.sencent.chat.db.DataHelper;
import com.sencent.chat.model.CMDMessage;
import com.sencent.chat.model.ConnectStatusEvent;
import com.sencent.chat.model.NewMsgEvent;
import com.sencent.chat.model.enumeration.ConnectionStatus;
import com.sencent.chat.model.enumeration.Direct;
import com.sencent.chat.model.enumeration.Status;
import com.sencent.common.log.SLog;
import com.sencent.common.process.ProcessUtil;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.rong.imlib.ModuleManager;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.CommandMessage;
import io.rong.message.ImageMessage;
import io.rong.message.TextMessage;
import io.rong.push.RongPushClient;

/**
 * Created by yanpeng on 2016/5/17.
 */
public class RongSDKHelper {
    private static MessageBusiness messageBusiness = new MessageBusiness();
    private static RongMsgBusiness msgBusiness = new RongMsgBusiness();
    private static IReceiveMessageInterceptor messageInterceptor = new ReceiveMessageInterceptor();

    public void init(final Context context) {
        boolean inRongPushProcess = "io.rong.push".equals(ProcessUtil.getCurrentProcessName(WeIMClient.getContext()));
        if (WeIMClient.inMainProcess(WeIMClient.getContext()) || inRongPushProcess) {
            RongPushClient.registerMiPush(context, "2882303761517314678", "5751731486678");
            RongPushClient.registerHWPush(context);
            RongIMClient.init(context);
            compatRongyunNullpointer();
        }
        if (WeIMClient.inMainProcess(WeIMClient.getContext())) {
            RongIMClient.getInstance().setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageListener() {
                /**
                 * 收到消息回调
                 *
                 * @param message
                 * @param i
                 * @return true:不显示通知，false：显示通知
                 */
                @Override
                public boolean onReceived(Message message, int i) {
//                String mCurrentProcessName = SystemUtils.getCurrentProcessName(WeIMClient.getContext());
//                Log.v("-------接收消息------", mCurrentProcessName);

                    try {
                        MessageContent content = message.getContent();
                        Logger.t("wechat_message_receive").v(new String(content.encode()));
                        //1.普通消息
                        if (content instanceof TextMessage || content instanceof ImageMessage) {
                            com.sencent.chat.model.Message msg = msgBusiness.convertFromThirdPart(message);
                            handleMessage(msg, i, message.getMessageId());
                        }

                        //3.cmd消息
                        else if (content instanceof CommandMessage) {
                            String name = ((CommandMessage) content).getName();
                            String data = ((CommandMessage) content).getData();
                            Map<String, String> map = (Map<String, String>) JSON.parseObject(data, Map.class);
                            CMDMessage cmdMessage = new CMDMessage();
                            cmdMessage.setAction(name);
                            cmdMessage.setData(map);
                            //特殊cmd当做，按普通消息处理
                            if (CMDMessage.ActionType.CMD_MSG_SEND.getAction().equals(cmdMessage.getAction())) {
                                String msgJson = map.get("data");
                                com.sencent.chat.model.Message msg = com.sencent.chat.model.Message.fromJson(msgJson);
                                msg.setStatus(Status.SUCCESS);
                                msg.setDirect(Direct.RECEIVE);
                                //Fixme: 对于cmd消息，默认按照临时消息处理.
                                msg.setTemp(1);
                                handleMessage(msg, 0, 0);
                            }else {
                                messageBusiness.handleCMDMessage(cmdMessage);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("消息体为：chatType=").append(message.getConversationType().getName());
                        stringBuilder.append(",senderUserId=").append(message.getSenderUserId());
                        stringBuilder.append(",targetId=").append(message.getTargetId());

                        MessageContent content = message.getContent();
                        stringBuilder.append("content").append(new String(content.encode()));

                        RuntimeException runtimeException = new RuntimeException(stringBuilder.toString(), e);
                        SLog.report(runtimeException);
                        Log.i("RongMsgDetail", stringBuilder.toString(), runtimeException);
                    }
                    return true;
                }
            });

            RongIMClient.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus connectionStatus) {
                    com.sencent.chat.model.enumeration.ConnectionStatus status = RongMessageUtils.convertConnectStatus
                            (connectionStatus);
                    //可以识别的状态才发送event
                    if (status != null) {
                        EventBus.getDefault().post(new ConnectStatusEvent(status));
                    }
                }
            });
        }
    }

    public static ConnectionStatus getCurrentConnectionStatus() {
        return RongMessageUtils.convertConnectStatus(RongIMClient.getInstance().getCurrentConnectionStatus());
    }

    public static void login(String token, LoginCallBack loginCallBack) {
        if (WeIMClient.inMainProcess(WeIMClient.getContext())) {
            RongIMClient.ConnectionStatusListener.ConnectionStatus status = RongIMClient.getInstance().getCurrentConnectionStatus();
            //已连接，正在连接，不进行登录
            if (status != RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED
                    && status != RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING) {
                RongIMClient.connect(token, new MyConnectCallback(loginCallBack));
            }
        }
    }

    /**
     * 重连
     */
    public static void reconnect(LoginCallBack loginCallBack) {
        try {
            RongIMClient.getInstance().reconnect(new MyConnectCallback(loginCallBack));
        } catch (Exception e) {
            SLog.reportError(e);
        }
    }

    /*  删除融云库里面的老消息 + 补偿消息  */
    public static int handleRongDBMsg(String accountId, String rongKey) {
        int lastCheckId = messageBusiness.getLastCheckId();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        String path = "";
        int msgMakeupCount = 0;
        try {
            path = WeIMClient.getContext().getFilesDir().getPath() + "/" + rongKey + "/" + accountId + "/storage";
            database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
//            int position = lastCheckId - 5000;
//            if (position > 0) {
//                // 每次进入应用都会删除融云库里的老消息
//                database.execSQL(" delete from RCT_MESSAGE where id in (SELECT id FROM RCT_MESSAGE WHERE id < ? ORDER" +
//                        " BY id LIMIT 5000)", new String[]{String.valueOf(position)});
//            }
            // 查询需要补偿的消息
            cursor = database.rawQuery("select * from RCT_MESSAGE where id > ? order by id ", new String[]{String.valueOf(lastCheckId)});
            int count = cursor.getCount();
            if (count == 0) {
                return msgMakeupCount;
            }
            if (lastCheckId == 0) {
                return msgMakeupCount;
            } else {
                while (cursor.moveToNext()) {
                    int direct = cursor.getInt(cursor.getColumnIndex("message_direction"));
                    if (direct == 1) {
                        int messageId = cursor.getInt(cursor.getColumnIndex("id"));
                        String uid = cursor.getString(cursor.getColumnIndex("extra_column5"));
                        String targetId = cursor.getString(cursor.getColumnIndex("target_id"));
                        int conversationType = cursor.getInt(cursor.getColumnIndex("category_id"));
                        long sendTime = cursor.getLong(cursor.getColumnIndex("send_time"));
                        String senderUserId = cursor.getString(cursor.getColumnIndex("sender_id"));
                        String contentType = cursor.getString(cursor.getColumnIndex("clazz_name"));
                        String content = cursor.getString(cursor.getColumnIndex("content"));
                        com.sencent.chat.model.Message message = msgBusiness.constructCompensationMessage(uid, targetId, conversationType, sendTime, senderUserId, contentType, content);
                        count--;
                        handleMessage(message, count, messageId);
                        msgMakeupCount ++;
                    }
                }
            }
        } catch (Exception e) {
            SLog.reportError(new RuntimeException("handleRongDBMsg failed, accountId:" + accountId + ", db path:" + path, e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null) {
                database.close();
            }
        }
        return msgMakeupCount;
    }

    public static void logout() {
        RongIMClient.getInstance().logout();
    }

    private static HashSet<Integer> cidSet = new HashSet<>();
    private static int msgCount = 0;
    private static List<com.sencent.chat.model.Message> messageList = new ArrayList<>();

    private static synchronized void handleMessage(com.sencent.chat.model.Message msg, int left, final int messageId) throws SQLException {

        messageList.add(msg);
        if (left == 0) {
            DataHelper helper = DataHelper.getHelper(WeIMClient.getContext());
            try {
                TransactionManager.callInTransaction(helper.getConnectionSource(), new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        for (int i = 0; i < messageList.size(); i++) {
                            com.sencent.chat.model.Message m = messageList.get(i);
                            try {
                                if (!messageInterceptor.interceptor(m)) {
                                    messageBusiness.saveNewMessageToDB(m);
                                    cidSet.add(m.getCid());
                                    msgCount++;
                                }
                            } catch (Exception e) {
                                SLog.reportError(e);
                            }
                        }
                        if(messageId>0){
                            messageBusiness.createOrUpdateLastCheckId(messageId);
                        }
                        return null;
                    }
                });
            } finally {
                messageList.clear();
                DataHelper.release(helper);
            }
        }

        Logger.t("onMessageEvent").v("left=" + left + " message=" + msg);
        //该批次左后一条消息
        if (left == 0) {
            if (!cidSet.isEmpty()) {
                //发Event并clear
                EventBus.getDefault().post(new NewMsgEvent(cidSet));
                //发通知，响铃
                if (WeIMClient.getMessageNotifer() != null) {
                    WeIMClient.getMessageNotifer().onNewMsg(msg, cidSet, msgCount);
                } else {
                    Logger.t("error").v("notifier 为null");
                }

                cidSet.clear();
                msgCount = 0;
            }
        }
    }

    private static class MyConnectCallback extends RongIMClient.ConnectCallback {

        LoginCallBack loginCallBack;

        MyConnectCallback(LoginCallBack loginCallBack) {
            this.loginCallBack = loginCallBack;
        }

        @Override
        public void onTokenIncorrect() {
            Logger.t("connect").v("onTokenIncorrect");
            if (loginCallBack != null) {
                loginCallBack.onTokenIncorrect();
            }
        }

        @Override
        public void onSuccess(String s) {
            Logger.t("connect").v("onSuccess=" + s);
            RongIMClient.getInstance().removeNotificationQuietHours(new RongIMClient.OperationCallback() {
                @Override
                public void onSuccess() {
                    Logger.t("removeNotificationQuietHours").e("onSuccess=");
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    Logger.t("removeNotificationQuietHours").e("onError=");
                }
            });

            if (loginCallBack != null) {
                loginCallBack.onSuccess(s);
            }
        }

        @Override
        public void onError(RongIMClient.ErrorCode errorCode) {
            Logger.t("reConnect").v("onError=" + errorCode.toString());
            //暂时去掉重连失败上报
//            try {
//                String hxId = DefaultIMSDKModel.getInstance().getHXId();
//                MobclickAgent.reportError(WeIMClient.getContext(), "融云连接失败 erroCode = " + errorCode + "   account=" + hxId);
//            } catch (Exception e) {
//                MobclickAgent.reportError(WeIMClient.getContext(), e);
//            }

            if (loginCallBack != null) {
                loginCallBack.onError(errorCode.getMessage());
            }
        }
    }

    public static void disconnect() {
        RongIMClient.getInstance().disconnect();
    }

    /**
     * 融云2.9.5dev版本有空指针问题，在融云更新版本解决该问题之前，先自行处理
     *
     */
    //Fixme later
    private void compatRongyunNullpointer() {
        try {
            Field listenerList = ModuleManager.class.getDeclaredField("connectivityStateChangedListeners");
            if (listenerList == null) {
                return;
            }
            listenerList.setAccessible(true);
            if (listenerList.get(null) == null) {
                listenerList.set(null, new ArrayList<>());
                SLog.i("RongSDKHelper", "compatRongyunNullpointer set ModuleManager listeners");
            }
        } catch (Throwable e) {
            SLog.reportError(e);
        }
    }
}
