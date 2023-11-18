package org.example;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import java.util.List;
import java.util.Map;

public class ProtobufCreator {

    public DynamicMessage createMessage(Descriptors.Descriptor descriptor, Map<String, Object> fieldValues)
            throws Descriptors.DescriptorValidationException {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(descriptor);

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            Descriptors.FieldDescriptor field = descriptor.findFieldByName(entry.getKey());
            if (field != null) {
                if (field.isRepeated()) {
                    List<Object> values = (List<Object>) entry.getValue();
                    for (Object value : values) {
                        if (field.getType() == Descriptors.FieldDescriptor.Type.ENUM &&
                                value instanceof String) {
                            Descriptors.EnumValueDescriptor enumValue =
                                    field.getEnumType().findValueByName((String) value);
                            if (enumValue != null) {
                                messageBuilder.addRepeatedField(field, enumValue);
                            } else {
                                throw new IllegalArgumentException("Invalid enum value for field " + entry.getKey());
                            }
                        } else {
                            messageBuilder.addRepeatedField(field, value);
                        }
                    }
                } else  if (field.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
                    if (entry.getValue() instanceof Descriptors.EnumValueDescriptor) {
                        messageBuilder.setField(field, entry.getValue());
                    } else {
                        throw new IllegalArgumentException("Invalid enum value for field " + entry.getKey());
                    }
                } else {
                    messageBuilder.setField(field, entry.getValue());
                }
            }
        }

        return messageBuilder.build();
    }
}
