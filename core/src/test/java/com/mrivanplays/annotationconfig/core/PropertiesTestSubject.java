package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

@Comment("Test subject config. Can change at any time")
@Comment("Generated by AnnotationConfig")
public class PropertiesTestSubject {

  @Comment("The message that will be sent to the player")
  @Comment("Bla bla 2nd comment")
  private String message = "this is a message";

  @Comment("The message sending strategy with which the message will be sent")
  @Key("message.type")
  private MessageType messageType = MessageType.STRING;

  public String getMessage() {
    return message;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public enum MessageType {
    COMPONENT,
    STRING
  }

  public static class MessageTypeResolver implements FieldTypeSerializer<MessageType> {

    @Override
    public MessageType deserialize(ConfigDataObject data, Field field) throws Exception {
      try {
        return MessageType.valueOf(data.getAsString());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid message type: " + data.getAsString());
      }
    }

    @Override
    public SerializedObject serialize(MessageType value, Field field) throws Exception {
      return SerializedObject.object(value.name());
    }
  }
}
