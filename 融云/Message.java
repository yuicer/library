package com.sencent.chat.model;
 
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sencent.chat.business.MessageSendCallBack;
import com.sencent.chat.model.enumeration.ChatType;
import com.sencent.chat.model.enumeration.CustomType;
import com.sencent.chat.model.enumeration.Direct;
import com.sencent.chat.model.enumeration.Status;
import com.sencent.chat.model.enumeration.Type;
import com.sencent.chat.model.message.MagicPicMessage;
import com.sencent.chat.model.message.PicMessage;
import com.sencent.chat.model.message.RedPackMessage;
import com.sencent.chat.model.message.TextMessage;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Message implements Serializable {

    private static final long serialVersionUID = -4156711524788024305L;

    /*自增主键*/
    private int id;

    /*发送成功有uid*/
    private String uid;

    /*落库之后有会话id*/
    private int cid;

    /*会话类型*/
    private ChatType chatType;

    /*消息方向*/
    private Direct direct;

    /*消息状态*/
    private Status status;

    /*发送时间*/
    private Date sendTime;

    /*是否未读（默认是未读）*/
    private boolean isUnread = true;

    /*发送者信息-START*/
    private User fromUser;

    /*接收者信息-START*/
    private User toUser;

    /*群信息*/
    private ChatGroup chatGroup;

    /*消息内容-START*/

    /*消息内容类型*/
    private Type contentType;

    /*消息内容自定义类型*/
    private CustomType customType;
    /*消息内容*/
    private IMessageContent messageContent;

    /**
     * 消息类型, temp = 1时 为临时消息
     */
    private int temp;

    /**
     * 消息监听，消息状态。
     */
    private MessageListener listener;

    public Message() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public Direct getDirect() {
        return direct;
    }

    public void setDirect(Direct direct) {
        this.direct = direct;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ChatType getChatType() {
        return chatType;
    }

    public void setChatType(ChatType chatType) {
        this.chatType = chatType;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }

    public Type getContentType() {
        return contentType;
    }

    public void setContentType(Type contentType) {
        this.contentType = contentType;
    }

    public CustomType getCustomType() {
        return customType;
    }

    public void setCustomType(CustomType customType) {
        this.customType = customType;
    }

    public IMessageContent getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(IMessageContent messageContent) {
        this.messageContent = messageContent;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public ChatGroup getChatGroup() {
        return chatGroup;
    }

    public void setChatGroup(ChatGroup chatGroup) {
        this.chatGroup = chatGroup;
    }

    /**
     * 获取对方的user信息
     */
    public User getTarget() {
        return getDirect() == Direct.RECEIVE? getFromUser():getToUser();
    }

    /**
     * 获取自己的user信息
     */
    public User getMe() {
        return getDirect() == Direct.RECEIVE? getToUser():getFromUser();
    }

    public Message setTemp(int temp) {
        this.temp = temp;
        return this;
    }

    public int getTemp() {
        return temp;
    }


    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uid", UUID.randomUUID().toString());
        jsonObject.put("chatType", chatType.getCode());
        jsonObject.put("direct", Direct.SEND.getCode());
        jsonObject.put("status", Status.SUCCESS.getCode());
        jsonObject.put("sendTime", JSON.toJSON(new Date()));
        jsonObject.put("isUnread", true);
        if (fromUser != null) {
            jsonObject.put("fromUser", JSON.parseObject(fromUser.toJsonString()));
        }
        if (toUser != null) {
            jsonObject.put("toUser", JSON.parseObject(toUser.toJsonString()));
        }
        if (chatGroup != null) {
            jsonObject.put("chatGroup", JSON.parseObject(chatGroup.toJsonString()));
        }
        jsonObject.put("contentType", contentType.getCode());
        jsonObject.put("customType", customType.getCode());
        if (messageContent != null) {
            jsonObject.put("messageContent", JSON.parseObject(messageContent.toJsonString()));
        }
        return jsonObject.toJSONString();
    }

    public static Message fromJson(String json) {
        if (json == null) {
            return null;
        }
        JSONObject content = JSON.parseObject(json);
        Message message = new Message();
        message.setUid(content.getString("uid"));
        message.setChatType(ChatType.valueOfCode(content.getString("chatType")));
        message.setDirect(Direct.valueOfCode(content.getString("direct")));
        message.setStatus(Status.valueOfCode(content.getString("status")));
        message.setSendTime(content.getDate("sendTime"));
        message.setUnread(content.getBooleanValue("isUnread"));
        if (content.containsKey("fromUser")) {
            message.setFromUser(User.fromJsonString(content.getJSONObject("fromUser").toJSONString()));
        }

        if (content.containsKey("toUser")) {
            message.setToUser(User.fromJsonString(content.getJSONObject("toUser").toJSONString()));
        }

        if (content.containsKey("chatGroup")) {
            message.setChatGroup(ChatGroup.fromJsonString(content.getJSONObject("chatGroup").toJSONString()));
        }

        message.setContentType(Type.valueOfCode(content.getString("contentType")));
        message.setCustomType(CustomType.valueOfCode(content.getString("customType")));
        if (Type.TXT == Type.valueOfCode(content.getString("contentType"))) {
            message.setMessageContent(TextMessage.fromJsonString(content.getJSONObject("messageContent").toJSONString()));
        } else if (Type.IMAGE == Type.valueOfCode(content.getString("contentType"))) {
            message.setMessageContent(PicMessage.fromJsonString(content.getJSONObject("messageContent").toJSONString()));
        } else if (Type.MAGIC_PIC == Type.valueOfCode(content.getString("contentType"))) {
            message.setMessageContent(MagicPicMessage.fromJsonString(content.getJSONObject("messageContent").toJSONString()));
        } else if (Type.REDPACK == Type.valueOfCode(content.getString("contentType"))) {
            message.setMessageContent(RedPackMessage.fromJsonString(content.getJSONObject("messageContent").toJSONString()));
        }
        return message;
    }

    public interface IFactory<T> {
        T create(Message message);
    }

    public void onSuccess() {
        if (listener != null) {
            listener.onSuccess();
        }
    }

    public void onError(MessageSendCallBack.ErrorCode errorCode) {
        if (listener != null) {
            listener.onError(errorCode);
        }
    }

    public void onProgress(final int i, String s) {
        if (listener != null) {
            listener.onProgress(i,s);
        }
    }

    public interface MessageListener {
        void onSuccess();
        void onError(MessageSendCallBack.ErrorCode errorCode);
        void onProgress(final int i, String s);
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public MessageListener getMessageListener() {
        return listener;
    }
}
